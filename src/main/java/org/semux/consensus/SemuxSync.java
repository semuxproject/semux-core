/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.SyncManager;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.BlockPartsMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Syncing manager downloads blocks from the network and imports them into
 * blockchain.
 * <p>
 * The {@link #download()} and the {@link #process()} methods are not
 * synchronized and need to be executed by one single thread at anytime.
 * <p>
 * The download/unfinished/pending queues are protected by lock.
 */
public class SemuxSync implements SyncManager {

    private static final Logger logger = LoggerFactory.getLogger(SemuxSync.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sync-" + cnt.getAndIncrement());
        }
    };

    private static final ScheduledExecutorService timer1 = Executors.newSingleThreadScheduledExecutor(factory);
    private static final ScheduledExecutorService timer2 = Executors.newSingleThreadScheduledExecutor(factory);

    private final long DOWNLOAD_TIMEOUT;

    private final int MAX_QUEUED_JOBS;
    private final int MAX_PENDING_JOBS;
    private final int MAX_PENDING_BLOCKS;

    private static final Random random = new Random();

    private Config config;

    private Blockchain chain;
    private ChannelManager channelMgr;

    // task queues
    private AtomicLong latestQueuedTask = new AtomicLong();
    private TreeSet<Long> toDownload = new TreeSet<>();
    private Map<Long, Long> toComplete = new HashMap<>();
    private TreeSet<Pair<Block, Channel>> toProcess = new TreeSet<>(
            Comparator.comparingLong(o -> o.getKey().getNumber()));
    private TreeSet<Pair<Block, Channel>> currentSet = new TreeSet<>(
            Comparator.comparingLong(o -> o.getKey().getNumber()));
    private TreeMap<Long, Pair<Block, Channel>> toFinalize = new TreeMap<>();

    private long lastBlockInSet;
    private boolean fastSync;
    private final Object lock = new Object();

    // current and target heights
    private AtomicLong begin = new AtomicLong();
    private AtomicLong current = new AtomicLong();
    private AtomicLong target = new AtomicLong();

    private Instant beginningInstant;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxSync(Kernel kernel) {
        this.config = kernel.getConfig();

        this.chain = kernel.getBlockchain();
        this.channelMgr = kernel.getChannelManager();

        this.DOWNLOAD_TIMEOUT = config.syncDownloadTimeout();
        this.MAX_QUEUED_JOBS = config.syncMaxQueuedJobs();
        this.MAX_PENDING_JOBS = config.syncMaxPendingJobs();
        this.MAX_PENDING_BLOCKS = config.syncMaxPendingBlocks();
    }

    @Override
    public void start(long targetHeight) {
        if (isRunning.compareAndSet(false, true)) {
            beginningInstant = Instant.now();

            logger.info("Syncing started, best known block = {}", targetHeight - 1);

            // [1] set up queues
            synchronized (lock) {
                toDownload.clear();
                toComplete.clear();
                toProcess.clear();
                currentSet.clear();
                toFinalize.clear();

                begin.set(chain.getLatestBlockNumber() + 1);
                current.set(chain.getLatestBlockNumber() + 1);
                target.set(targetHeight);
                latestQueuedTask.set(chain.getLatestBlockNumber());
                fastSync = false;
                growToDownloadQueue();
            }

            // [2] start tasks
            ScheduledFuture<?> download = timer1.scheduleAtFixedRate(this::download, 0, 5, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> process = timer2.scheduleAtFixedRate(this::process, 0, 5, TimeUnit.MILLISECONDS);

            // [3] wait until the sync is done
            while (isRunning.get()) {
                synchronized (isRunning) {
                    try {
                        isRunning.wait(1000);
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

            Instant end = Instant.now();
            logger.info("Syncing finished, took {}", TimeUtil.formatDuration(Duration.between(beginningInstant, end)));
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

    protected void addBlock(Block block, Channel channel) {
        synchronized (lock) {
            if (toDownload.remove(block.getNumber())) {
                growToDownloadQueue();
            }
            toComplete.remove(block.getNumber());
            toProcess.add(Pair.of(block, channel));
        }
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
            addBlock(block, channel);
            break;
        }
        case BLOCK_PARTS: {
            // try re-construct a block
            BlockPartsMessage blockPartsMsg = (BlockPartsMessage) msg;
            List<Block.BlockPart> parts = Block.BlockPart.decode(blockPartsMsg.getParts());
            List<byte[]> data = blockPartsMsg.getData();
            if (parts.size() != data.size()) {
                logger.debug("Parts id and data do not match");
                break;
            }

            // We need header, transactions, and votes
            if (parts.get(0) != Block.BlockPart.HEADER || parts.get(1) != Block.BlockPart.TRANSACTIONS
                    || parts.get(0) != Block.BlockPart.VOTES) {
                try {
                    Block block = Block.fromComponents(data.get(0), data.get(1), null, data.get(2));
                    addBlock(block, channel);
                } catch (Exception e) {
                    logger.debug("Failed to parse a block from components", e);
                }
            }
            break;
        }
        case BLOCK_HEADER: // deprecated
        default: {
            break;
        }
        }
    }

    private void download() {
        if (!isRunning()) {
            return;
        }

        synchronized (lock) {
            // filter all expired tasks
            long now = TimeUtil.currentTimeMillis();
            Iterator<Entry<Long, Long>> itr = toComplete.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Long, Long> entry = itr.next();

                if (entry.getValue() + DOWNLOAD_TIMEOUT < now) {
                    logger.debug("Downloading of block #{} has expired", entry.getKey());
                    toDownload.add(entry.getKey());
                    itr.remove();
                }
            }

            // quit if too many unfinished jobs
            if (toComplete.size() > MAX_PENDING_JOBS) {
                logger.trace("Max pending jobs reached");
                return;
            }

            // quit if no more tasks
            if (toDownload.isEmpty()) {
                return;
            }
            Long task = toDownload.first();

            // quit if too many pending blocks
            int pendingBlocks = toProcess.size() + currentSet.size() + toFinalize.size();
            if (pendingBlocks > MAX_PENDING_BLOCKS && task > toProcess.first().getKey().getNumber()) {
                logger.trace("Max pending blocks reached");
                return;
            }

            // get idle channels
            List<Channel> channels = channelMgr.getIdleChannels().stream()
                    .filter(channel -> channel.getRemotePeer().getLatestBlockNumber() >= task)
                    .collect(Collectors.toList());
            logger.trace("Idle peers = {}", channels.size());

            // quit if no idle channels.
            if (channels.isEmpty()) {
                return;
            }

            // pick a random channel
            Channel c = channels.get(random.nextInt(channels.size()));

            // request the block
            if (c.getRemotePeer().getLatestBlockNumber() >= task) {
                logger.trace("Request block #{} from {}", task, c.getRemoteIp());
                c.getMessageQueue().sendMessage(new GetBlockMessage(task));

                if (toDownload.remove(task)) {
                    growToDownloadQueue();
                }
                toComplete.put(task, TimeUtil.currentTimeMillis());
            }
        }
    }

    /**
     * Queue new tasks sequentially starting from
     * ${@link SemuxSync#latestQueuedTask} until the size of
     * ${@link SemuxSync#toDownload} queue is greater than or equal to
     * MAX_QUEUED_JOBS
     */
    private void growToDownloadQueue() {
        // To avoid overhead, this method doesn't add new tasks before the queue is less
        // than half-filled
        if (toDownload.size() >= MAX_QUEUED_JOBS / 2) {
            return;
        }

        for (long task = latestQueuedTask.get() + 1; //
                task < target.get() && toDownload.size() < MAX_QUEUED_JOBS; //
                task++) {
            latestQueuedTask.accumulateAndGet(task, (prev, next) -> next > prev ? next : prev);
            if (!chain.hasBlock(task)) {
                toDownload.add(task);
            }
        }
    }

    /**
     * Fast sync process: Validate votes only for the last block in each validator
     * set. For each block in the set, compare its hash against its child parent
     * hash. Once all hashes are validated, validate (while skipping vote
     * validation) and apply each block to the chain.
     */
    protected void process() {
        if (!isRunning()) {
            return;
        }

        long latest = chain.getLatestBlockNumber();
        if (latest + 1 >= target.get()) {
            stop();
            return; // This is important because stop() only notify
        }

        // Perform fast sync only if all blocks in current validator sets have been
        // forged
        synchronized (lock) {
            if (!fastSync) {
                // fastSync value is updated at the beginning of each set
                if (latest % config.spec().getValidatorUpdateInterval() == 0) {
                    if (target.get() >= latest + config.spec().getValidatorUpdateInterval()) {
                        toFinalize.clear();
                        currentSet.clear();
                        lastBlockInSet = latest + config.spec().getValidatorUpdateInterval();
                        fastSync = true;
                    }
                }
            }
        }

        Pair<Block, Channel> pair = null;
        synchronized (lock) {
            // If not all hashes in current validator set were validated
            if (fastSync && toFinalize.size() < lastBlockInSet - latest) {
                // Add missing blocks to currentSet
                Iterator<Pair<Block, Channel>> blocksToProcessIterator = toProcess.iterator();
                while (blocksToProcessIterator.hasNext()) {
                    Pair<Block, Channel> p = blocksToProcessIterator.next();
                    if (p.getKey().getNumber() <= latest) {
                        blocksToProcessIterator.remove();
                    } else if (p.getKey().getNumber() <= lastBlockInSet) {
                        blocksToProcessIterator.remove();
                        currentSet.add(p);
                    } else {
                        break;
                    }
                }

                // Validate remaining block hashes in current set
                validateSetHashes();
                return;
            }

            Iterator<Pair<Block, Channel>> blocksToApplyIterator;
            // Check if next block is ready to be applied to the chain
            if (fastSync) {
                blocksToApplyIterator = toFinalize.values().iterator();
            } else {
                blocksToApplyIterator = toProcess.iterator();
            }
            while (blocksToApplyIterator.hasNext()) {
                Pair<Block, Channel> p = blocksToApplyIterator.next();
                // In case a normal sync is performed, toProcess might contain blocks which
                // already have been applied to the chain
                if (p.getKey().getNumber() <= latest) {
                    blocksToApplyIterator.remove();
                } else if (p.getKey().getNumber() == latest + 1) {
                    toFinalize.remove(p.getKey().getNumber());
                    toProcess.remove(p);
                    pair = p;
                    break;
                } else {
                    break;
                }
            }
        }

        // Validate and apply block to the chain
        if (pair != null) {
            Block block = pair.getKey();
            boolean validateVotes = !fastSync; // If fastSync is true, skip vote validation

            if (chain.importBlock(block, validateVotes)) {
                // update current height
                current.set(block.getNumber() + 1);

                synchronized (lock) {
                    if (toDownload.remove(block.getNumber())) {
                        growToDownloadQueue();
                    }
                    toComplete.remove(block.getNumber());
                    if (block.getNumber() == lastBlockInSet) {
                        logger.info("{}", block); // Log last block in set
                        fastSync = false;
                    } else if (!fastSync) {
                        logger.info("{}", block); // Log all blocks
                    }
                }
            } else {
                handleInvalidBlock(block, pair.getValue());
            }
        }
    }

    protected void validateSetHashes() {
        synchronized (lock) {
            Iterator<Pair<Block, Channel>> iterator = currentSet.descendingIterator();
            while (iterator.hasNext()) {
                Pair<Block, Channel> p = iterator.next();
                if (toFinalize.containsKey(p.getKey().getNumber())) {
                    iterator.remove();
                } else {
                    Pair<Block, Channel> child = toFinalize.get(p.getKey().getNumber() + 1);
                    if (child != null) {
                        // Validate block header and compare its hash against its child parent hash
                        if (Arrays.equals(child.getKey().getParentHash(), p.getKey().getHash()) &&
                                p.getKey().getHeader().validate()) {
                            toFinalize.put(p.getKey().getNumber(), p);
                            iterator.remove();
                        } else {
                            handleInvalidBlock(p.getKey(), p.getValue());
                            return;
                        }
                    } else if (p.getKey().getNumber() == lastBlockInSet) {
                        iterator.remove();

                        Block block = p.getKey(); // Validate votes for last block in set
                        if (chain.validateBlockVotes(block)) {
                            toFinalize.put(block.getNumber(), p);
                            toComplete.remove(block.getNumber());
                        } else {
                            handleInvalidBlock(block, p.getValue());
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Handle invalid block: Add block back to download queue. Remove block from all
     * other queues. Disconnect from the peer that sent the block.
     *
     * @param block
     * @param channel
     */
    protected void handleInvalidBlock(Block block, Channel channel) {
        InetSocketAddress a = channel.getRemoteAddress();
        logger.info("Invalid block from {}:{}", a.getAddress().getHostAddress(), a.getPort());
        synchronized (lock) {
            toDownload.add(block.getNumber());
            toComplete.remove(block.getNumber());
            currentSet.remove(Pair.of(block, channel));
            toFinalize.remove(block.getNumber(), Pair.of(block, channel));
            toProcess.remove(Pair.of(block, channel));
        }
        // disconnect if the peer sends us invalid block
        channel.getMessageQueue().disconnect(ReasonCode.BAD_PEER);
    }

    @Override
    public SemuxSyncProgress getProgress() {
        return new SemuxSyncProgress(
                begin.get(),
                current.get(),
                target.get(),
                Duration.between(beginningInstant != null ? beginningInstant : Instant.now(), Instant.now()));
    }

    public static class SemuxSyncProgress implements Progress {

        final long startingHeight;

        final long currentHeight;

        final long targetHeight;

        final Duration duration;

        public SemuxSyncProgress(long startingHeight, long currentHeight, long targetHeight, Duration duration) {
            this.startingHeight = startingHeight;
            this.currentHeight = currentHeight;
            this.targetHeight = targetHeight;
            this.duration = duration;
        }

        @Override
        public long getStartingHeight() {
            return startingHeight;
        }

        @Override
        public long getCurrentHeight() {
            return currentHeight;
        }

        @Override
        public long getTargetHeight() {
            return targetHeight;
        }

        @Override
        public Duration getSyncEstimation() {
            Long speed = getSpeed();
            if (speed == null || speed == 0) {
                return null;
            }

            return Duration.ofMillis(BigInteger.valueOf(getTargetHeight())
                    .subtract(BigInteger.valueOf(getCurrentHeight()))
                    .multiply(BigInteger.valueOf(speed))
                    .longValue());
        }

        private Long getSpeed() {
            long downloadedBlocks = currentHeight - startingHeight;
            if (downloadedBlocks <= 0 || duration.toMillis() == 0) {
                return null;
            }

            return BigDecimal.valueOf(duration.toMillis())
                    .divide(BigDecimal.valueOf(downloadedBlocks), MathContext.DECIMAL64)
                    .round(MathContext.DECIMAL64)
                    .longValue();
        }
    }
}
