/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.semux.Config;
import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Sync;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxSync implements Sync {

    private static final Logger logger = LoggerFactory.getLogger(SemuxSync.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sync-mgr-" + cnt.getAndIncrement());
        }
    };

    private static final int BLOCK_REQUEST_REDUNDANCY = 1;

    private static final int MAX_UNFINISHED_JOBS = 16;

    private static final long MAX_DOWNLOAD_TIME = 30 * 1000; // 30 seconds

    private static final int MAX_PENDING_BLOCKS = 512;

    private Blockchain chain;
    private ChannelManager channelMgr;

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> download;
    private ScheduledFuture<?> process;

    // task queues
    private TreeSet<Long> toDownload = new TreeSet<>();
    private Map<Long, Long> toComplete = new HashMap<>();
    private TreeSet<Block> toProcess = new TreeSet<>();
    private long target;
    private Object lock = new Object();

    private volatile boolean isRunning;
    private Object done = new Object();

    private static SemuxSync instance;

    /**
     * Get the singleton instance.
     * 
     * @return
     */
    public static synchronized SemuxSync getInstance() {
        if (instance == null) {
            instance = new SemuxSync();
        }

        return instance;
    }

    private SemuxSync() {
    }

    @Override
    public void init(Blockchain chain, ChannelManager channelMgr) {
        this.chain = chain;
        this.channelMgr = channelMgr;
    }

    @Override
    public void start(long targetHeight) {
        if (!isRunning()) {
            isRunning = true;
            logger.info("Sync manager started, best known block = {}", targetHeight - 1);

            // [1] set up queues
            synchronized (lock) {
                toDownload.clear();
                toComplete.clear();
                toProcess.clear();

                target = targetHeight;
                for (long i = chain.getLatestBlockNumber() + 1; i < target; i++) {
                    toDownload.add(i);
                }
            }

            // [2] start tasks
            exec = Executors.newSingleThreadScheduledExecutor(factory);
            download = exec.scheduleAtFixedRate(() -> {
                download();
            }, 0, 50, TimeUnit.MILLISECONDS);
            process = exec.scheduleAtFixedRate(() -> {
                process();
            }, 0, 10, TimeUnit.MILLISECONDS);

            // [3] wait until the sync is done
            synchronized (done) {
                try {
                    done.wait();
                } catch (InterruptedException e) {
                    logger.info("Sync manager got interrupted");
                }
            }

            // [4] cancel tasks
            download.cancel(true);
            process.cancel(false);

            // [5] shutdown executor
            exec.shutdown();
            try {
                exec.awaitTermination(1, TimeUnit.MINUTES);
                logger.info("Sync finished");

            } catch (InterruptedException e) {
                logger.error("Executors were not properly shut down");
            }
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            isRunning = false;
            synchronized (done) {
                done.notifyAll();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public boolean onMessage(Channel channel, Message msg) {
        if (!isRunning()) {
            return false;
        }

        switch (msg.getCode()) {
        case BLOCK:
            BlockMessage blockMsg = (BlockMessage) msg;
            Block block = blockMsg.getBlock();
            if (block != null) {
                synchronized (lock) {
                    toDownload.remove(block.getNumber());
                    toComplete.remove(block.getNumber());
                    toProcess.add(block);
                }
            }
            return true;
        case BLOCK_HEADER:
            // TODO implement block header
            return true;
        default:
            return false;
        }
    }

    private void download() {
        List<Channel> channels = channelMgr.getIdleChannels();
        Collections.shuffle(channels);
        logger.trace("Idle peers = {}", channels.size());

        synchronized (lock) {
            // filter all expired tasks
            boolean hasExpired = false;
            long now = System.currentTimeMillis();
            Iterator<Entry<Long, Long>> itr = toComplete.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Long, Long> entry = itr.next();

                if (entry.getValue() + MAX_DOWNLOAD_TIME < now) {
                    logger.debug("Downloading of block #{} has expired", entry.getKey());
                    toDownload.add(entry.getKey());
                    itr.remove();
                    hasExpired = true;
                }
            }

            // quit if too many pending blocks
            if (!hasExpired && toProcess.size() > MAX_PENDING_BLOCKS) {
                return;
            }

            for (int i = 0; i + BLOCK_REQUEST_REDUNDANCY <= channels.size(); i += BLOCK_REQUEST_REDUNDANCY) {
                // quit if no more tasks or two many unfinished jobs
                if (toDownload.isEmpty() || toComplete.size() > MAX_UNFINISHED_JOBS) {
                    break;
                }

                Long task = toDownload.first();
                boolean requested = false;
                for (int j = 0; j < BLOCK_REQUEST_REDUNDANCY; j++) {
                    Channel c = channels.get(i + j);
                    if (c.getRemotePeer().getLatestBlockNumber() >= task) {
                        logger.debug("Request block #{} from cid = {}", task, c.getId());
                        c.getMessageQueue().sendMessage(new GetBlockMessage(task));
                        requested = true;
                    }
                }

                if (requested) {
                    toDownload.remove(task);
                    toComplete.put(task, System.currentTimeMillis());
                }
            }
        }
    }

    private void process() {
        long latest = chain.getLatestBlockNumber();
        if (latest + 1 == target) {
            stop();
            return; // This is important because stop() only notify
        }

        Block block = null;
        synchronized (lock) {
            Iterator<Block> iter = toProcess.iterator();
            while (iter.hasNext()) {
                Block b = iter.next();

                if (b.getNumber() <= latest) {
                    iter.remove();
                } else if (b.getNumber() == latest + 1) {
                    iter.remove();
                    block = b;
                    break;
                } else {
                    toProcess.add(b);
                    break;
                }
            }
        }

        if (block != null) {
            logger.info("{}", block);

            if (validateApplyBlock(block)) {
                synchronized (lock) {
                    toDownload.remove(block.getNumber());
                    toComplete.remove(block.getNumber());
                }
            } else {
                logger.info("Invalid block");
                synchronized (lock) {
                    toDownload.add(block.getNumber());
                }

                // sleep a while if you received an invalid block, to avoid consuming to much
                // resources.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Check if a block is valid, and apply to the chain if yes.
     * 
     * @param block
     * @return
     */
    private boolean validateApplyBlock(Block block) {
        try {
            // [1] check block integrity and signature
            if (!block.validate()) {
                logger.debug("Invalid block/transaction format");
                return false;
            }

            // [2] check number, prevHash and time stamp
            Block latest = chain.getLatestBlock();
            if (block.getNumber() != latest.getNumber() + 1) {
                logger.debug("Invalid block number");
                return false;
            }
            if (!Arrays.equals(block.getPrevHash(), latest.getHash())) {
                logger.debug("Invalid block previous hash");
                return false;
            }
            if (block.getTimestamp() <= latest.getTimestamp()) {
                logger.debug("Invalid block timestamp");
                return false;
            }

            AccountState as = chain.getAccountState().track();
            DelegateState ds = chain.getDelegateState().track();

            // [3] check votes
            List<String> validators = chain.getValidators();
            int twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);
            if (block.getVotes().size() < twoThirds) {
                logger.debug("Invalid BFT votes: {} < {}", block.getVotes().size(), twoThirds);
                return false;
            }

            Set<String> set = new HashSet<>(validators);
            Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                    block.getHash());
            byte[] encoded = vote.getEncoded();
            for (Signature sig : block.getVotes()) {
                String addr = Hex.encode(Hash.h160(sig.getPublicKey()));

                if (!set.contains(addr) || !EdDSA.verify(encoded, sig)) {
                    logger.debug("Invalid BFT vote: signer = {}", addr);
                    return false;
                }
            }

            // [4] check transactions
            TransactionExecutor exec = new TransactionExecutor();
            List<TransactionResult> results = exec.execute(block.getTransactions(), as, ds);
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).isValid()) {
                    Transaction tx = block.getTransactions().get(i);
                    logger.debug("Invalid transaction: type = {}, hash = {}", tx.getType(), Hex.encode(tx.getHash()));
                    return false;
                }
            }

            // [5] apply block reward and tx fees
            long reward = Config.getBlockReward(block.getNumber());
            for (Transaction tx : block.getTransactions()) {
                reward += tx.getFee();
            }
            if (reward > 0) {
                Account acc = as.getAccount(block.getCoinbase());
                acc.setAvailable(acc.getAvailable() + reward);
            }

            // [6] commit the updates
            as.commit();
            ds.commit();

            WriteLock lock = Config.STATE_LOCK.writeLock();
            lock.lock();
            try {
                // [7] flush state to disk
                chain.getAccountState().commit();
                chain.getDelegateState().commit();

                // [8] add block to chain
                chain.addBlock(block);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            logger.info("Exception in block validation", e);
            return false;
        }

        return true;
    }
}
