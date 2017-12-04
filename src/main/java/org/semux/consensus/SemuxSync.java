/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.SyncManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hex;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxSync implements SyncManager {

    private static final Logger logger = LoggerFactory.getLogger(SemuxSync.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sync-" + cnt.getAndIncrement());
        }
    };

    private static final int BLOCK_REQUEST_REDUNDANCY = 1;

    private static final int MAX_UNFINISHED_JOBS = 16;

    private static final long MAX_DOWNLOAD_TIME = 30L * 1000L; // 30 seconds

    private static final int MAX_PENDING_BLOCKS = 512;

    private Kernel kernel;
    private Config config;

    private Blockchain chain;
    private ChannelManager channelMgr;

    // task queues
    private TreeSet<Long> toDownload = new TreeSet<>();
    private Map<Long, Long> toComplete = new HashMap<>();
    private TreeSet<Block> toProcess = new TreeSet<>(Comparator.comparingLong(Block::getNumber));
    private long target;
    private final Object lock = new Object();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxSync(Kernel kernel) {
        this.kernel = kernel;
        this.config = kernel.getConfig();

        this.chain = kernel.getBlockchain();
        this.channelMgr = kernel.getChannelManager();
    }

    @Override
    public void start(long targetHeight) {
        if (isRunning.compareAndSet(false, true)) {
            Instant begin = Instant.now();

            logger.info("Syncing started, best known block = {}", targetHeight - 1);

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
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(factory);
            ScheduledFuture<?> download = exec.scheduleAtFixedRate(this::download, 0, 50, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> process = exec.scheduleAtFixedRate(this::process, 0, 10, TimeUnit.MILLISECONDS);

            // [3] wait until the sync is done
            while (isRunning.get()) {
                synchronized (isRunning) {
                    try {
                        isRunning.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Sync manager got interrupted");
                        break;
                    }
                }
            }

            // [4] cancel tasks
            download.cancel(true);
            process.cancel(false);

            // [5] shutdown executor
            exec.shutdown();
            try {
                exec.awaitTermination(1, TimeUnit.MINUTES);
                Instant end = Instant.now();
                logger.info("Syncing finished, took {}", TimeUtil.formatDuration(Duration.between(begin, end)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Executors were not properly shut down");
            }
        }
    }

    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            synchronized (isRunning) {
                isRunning.notifyAll();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void onMessage(Channel channel, Message msg) {
        if (!isRunning()) {
            return;
        }

        switch (msg.getCode()) {
        case BLOCK: {
            BlockMessage blockMsg = (BlockMessage) msg;
            Block block = blockMsg.getBlock();
            if (block != null) {
                synchronized (lock) {
                    toDownload.remove(block.getNumber());
                    toComplete.remove(block.getNumber());
                    toProcess.add(block);
                }
            }
            break;
        }
        case BLOCK_HEADER: {
            // TODO implement block header
            break;
        }
        default: {
            break;
        }
        }
    }

    private void download() {
        if (!isRunning()) {
            return;
        }

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
                        logger.debug("Request block #{} from channel = {}", task, c.getId());
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
        if (!isRunning()) {
            return;
        }

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
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Check if a block is success, and apply to the chain if yes.
     * 
     * @param block
     * @return
     */
    private boolean validateApplyBlock(Block block) {
        BlockHeader header = block.getHeader();
        List<Transaction> transactions = block.getTransactions();
        long number = header.getNumber();

        // [1] check block header
        Block latest = chain.getLatestBlock();
        if (!Block.validateHeader(latest.getHeader(), header)) {
            logger.debug("Invalid block header");
            return false;
        }

        // [2] check transactions and results
        if (transactions.size() > config.maxBlockSize()
                || !Block.validateTransactions(header, block.getTransactions())) {
            logger.debug("Invalid block transactions");
            return false;
        }
        if (!Block.validateResults(header, block.getResults())) {
            logger.debug("Results root does not match");
            return false;
        }

        AccountState as = chain.getAccountState().track();
        DelegateState ds = chain.getDelegateState().track();
        TransactionExecutor transactionExecutor = new TransactionExecutor(config);

        // [3] evaluate transactions
        List<TransactionResult> results = transactionExecutor.execute(transactions, as, ds);
        if (!Block.validateResults(header, results)) {
            logger.debug("Invalid transactions");
            return false;
        }

        // [4] evaluate votes
        List<String> validators = chain.getValidators();
        int twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);
        if (block.getVotes().size() < twoThirds) {
            logger.debug("Invalid BFT votes: {} < {}", block.getVotes().size(), twoThirds);
            return false;
        }

        Set<String> set = new HashSet<>(validators);
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, number, block.getView(), block.getHash());
        byte[] encoded = vote.getEncoded();
        for (Signature sig : block.getVotes()) {
            String a = Hex.encode(sig.getAddress());

            if (!set.contains(a) || !EdDSA.verify(encoded, sig)) {
                logger.debug("Invalid BFT vote: signer = {}", a);
                return false;
            }
        }

        // [5] apply block reward and tx fees
        long reward = config.getBlockReward(number);
        for (Transaction tx : block.getTransactions()) {
            reward += tx.getFee();
        }
        if (reward > 0) {
            as.adjustAvailable(block.getCoinbase(), reward);
        }

        // [6] commit the updates
        as.commit();
        ds.commit();

        WriteLock writeLock = kernel.getStateLock().writeLock();
        writeLock.lock();
        try {
            // [7] flush state to disk
            chain.getAccountState().commit();
            chain.getDelegateState().commit();

            // [8] add block to chain
            chain.addBlock(block);
        } finally {
            writeLock.unlock();
        }

        return true;
    }
}
