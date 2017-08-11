/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.Arrays;
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

import org.bouncycastle.util.encoders.Hex;
import org.semux.Config;
import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Delegate;
import org.semux.core.Sync;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.utils.ByteArray;
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

    private static final int MAX_UNFINISHED_JOBS = 16;

    private static final long MAX_DOWNLOAD_TIME = 2 * 60 * 1000;

    private static final int MAX_PENDING_BLOCKS = 256;

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

    private boolean isRunning;
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

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    @Override
    public void start(long targetHeight) {
        if (!isRunning()) {
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
            download = exec.scheduleAtFixedRate(() -> {
                download();
            }, 0, 50, TimeUnit.MILLISECONDS);
            process = exec.scheduleAtFixedRate(() -> {
                process();
            }, 0, 10, TimeUnit.MILLISECONDS);

            isRunning = true;
            logger.info("Sync manager started");
            logger.info("Height: current = {}, target = {}", chain.getLatestBlockNumber(), target);

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
            isRunning = false;
            logger.info("Sync manager stopped");
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
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
        logger.trace("Idle peers = {}", channels.size());

        synchronized (lock) {
            // filter all expired tasks
            long now = System.currentTimeMillis();
            Iterator<Entry<Long, Long>> itr = toComplete.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Long, Long> entry = itr.next();

                if (entry.getValue() + MAX_DOWNLOAD_TIME < now) {
                    itr.remove();
                    toDownload.add(entry.getKey());
                }
            }

            // quit if too many unprocessed blocks
            if (toProcess.size() > MAX_PENDING_BLOCKS) {
                return;
            }

            for (Channel c : channels) {
                // quit if no more tasks or two many unfinished jobs
                if (toDownload.isEmpty() || toComplete.size() > MAX_UNFINISHED_JOBS) {
                    break;
                }

                Long task = toDownload.first();
                logger.debug("Requesting for block #{}", task);
                c.getMessageQueue().sendMessage(new GetBlockMessage(task));

                toDownload.remove(task);
                toComplete.put(task, System.currentTimeMillis());
            }
        }
    }

    private void process() {
        long latest = chain.getLatestBlockNumber();
        if (latest + 1 == target) {
            stop();
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
            logger.info(block.toString());

            if (validateAndCommit(block)) {
                // [7] add block to chain
                chain.addBlock(block);

                // [8] flush state changes to disk
                chain.getAccountState().commit();
                chain.getDeleteState().commit();

                synchronized (lock) {
                    toDownload.remove(block.getNumber());
                    toComplete.remove(block.getNumber());
                }
            } else {
                logger.info("Invalid block: hash = {}", Hex.encode(block.getHash()));
                synchronized (lock) {
                    toDownload.add(block.getNumber());
                }
            }
        }
    }

    /**
     * Validate a block, and commit state change if valid.
     * 
     * @param block
     * @return
     */
    private boolean validateAndCommit(Block block) {
        try {
            // [1] check block integrity and signature
            if (!block.validate()) {
                return false;
            }

            // [2] check number and prevHash
            Block latest = chain.getLatestBlock();
            if (block.getNumber() != latest.getNumber() + 1 || !Arrays.equals(block.getPrevHash(), latest.getHash())) {
                return false;
            }

            AccountState as = chain.getAccountState().track();
            DelegateState ds = chain.getDeleteState().track();

            // [3] check transactions
            TransactionExecutor exec = new TransactionExecutor();
            List<TransactionResult> results = exec.execute(block.getTransactions(), as, ds, false);
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).isValid()) {
                    return false;
                }
            }

            // [4] check votes
            List<Delegate> validators = ds.getValidators();
            int twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);
            if (block.getVotes().size() < twoThirds) {
                return false;
            }

            Set<ByteArray> set = new HashSet<>();
            for (Delegate d : validators) {
                set.add(ByteArray.of(d.getAddress()));
            }
            Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getHash(), block.getNumber(),
                    block.getView());
            byte[] encoded = vote.getEncoded();
            for (Signature sig : block.getVotes()) {
                ByteArray addr = ByteArray.of(Hash.h160(sig.getPublicKey()));

                if (!set.contains(addr) || !EdDSA.verify(encoded, sig)) {
                    return false;
                }
            }

            // [5] apply block reward
            long reward = Config.getBlockReward(block.getNumber());
            if (reward > 0) {
                Account acc = as.getAccount(block.getCoinbase());
                acc.setBalance(acc.getBalance() + reward);
            }

            // [6] commit the updates
            as.commit();
            ds.commit();
        } catch (Exception e) {
            logger.info("Exception in sync block validation", e);
            return false;
        }

        return true;
    }
}
