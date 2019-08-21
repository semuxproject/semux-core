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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
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
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockPart;
import org.semux.core.Blockchain;
import org.semux.core.SyncManager;
import org.semux.net.Capability;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.Peer;
import org.semux.net.msg.Message;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.BlockPartsMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.consensus.GetBlockPartsMessage;
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

    // Blocks to download
    private TreeSet<Long> toDownload = new TreeSet<>();

    // Blocks which were requested but haven't been received
    private Map<Long, Long> toReceive = new HashMap<>();

    // Blocks which were received but haven't been validated
    private TreeSet<Pair<Block, Channel>> toValidate = new TreeSet<>(
            Comparator.comparingLong(o -> o.getKey().getNumber()));

    // Blocks which were validated but haven't been imported
    private TreeMap<Long, Pair<Block, Channel>> toImport = new TreeMap<>();

    private final Object lock = new Object();

    // current and target heights
    private AtomicLong begin = new AtomicLong();
    private AtomicLong current = new AtomicLong();
    private AtomicLong target = new AtomicLong();

    private Instant beginningInstant;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // reset at the beginning of a sync task
    private Set<String> badPeers = new HashSet<>();

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

            badPeers.clear();

            logger.info("Syncing started, best known block = {}", targetHeight - 1);

            // [1] set up queues
            synchronized (lock) {
                toDownload.clear();
                toReceive.clear();
                toValidate.clear();
                toImport.clear();

                begin.set(chain.getLatestBlockNumber() + 1);
                current.set(chain.getLatestBlockNumber() + 1);
                target.set(targetHeight);
                latestQueuedTask.set(chain.getLatestBlockNumber());
                growToDownloadQueue();
            }

            // [2] start tasks
            long period = config.syncFastSync() ? 1 : 5;
            ScheduledFuture<?> download = timer1.scheduleAtFixedRate(this::download, 0, period, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> process = timer2.scheduleAtFixedRate(this::process, 0, period, TimeUnit.MILLISECONDS);

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
            toReceive.remove(block.getNumber());
            toValidate.add(Pair.of(block, channel));
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
            List<BlockPart> parts = BlockPart.decode(blockPartsMsg.getParts());
            List<byte[]> data = blockPartsMsg.getData();

            // sanity check
            if (parts.size() != data.size()) {
                logger.debug("Part set and data do not match");
                break;
            }

            // parse the data
            byte[] header = null, transactions = null, results = null, votes = null;
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i) == BlockPart.HEADER) {
                    header = data.get(i);
                } else if (parts.get(i) == BlockPart.TRANSACTIONS) {
                    transactions = data.get(i);
                } else if (parts.get(i) == BlockPart.RESULTS) {
                    results = data.get(i);
                } else if (parts.get(i) == BlockPart.VOTES) {
                    votes = data.get(i);
                } else {
                    // unknown
                }
            }

            // import block
            try {
                Block block = Block.fromComponents(header, transactions, results, votes);
                addBlock(block, channel);
            } catch (Exception e) {
                logger.debug("Failed to parse a block from components", e);
            }
            break;
        }
        case BLOCK_HEADER: // deprecated
        default: {
            break;
        }
        }
    }

    private boolean isFastSyncSupported(Peer peer) {
        return Stream.of(peer.getCapabilities()).anyMatch(c -> Capability.FAST_SYNC.name().equals(c));
    }

    private boolean skipVotes(long blockNumber) {
        long interval = config.spec().getValidatorUpdateInterval();

        boolean isPivotBlock = (blockNumber % interval == 0);
        boolean isSafeBlock = (blockNumber < target.get() - interval);

        return !isPivotBlock && isSafeBlock;
    }

    private void download() {
        if (!isRunning()) {
            return;
        }

        synchronized (lock) {
            // filter all expired tasks
            long now = TimeUtil.currentTimeMillis();
            Iterator<Entry<Long, Long>> itr = toReceive.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Long, Long> entry = itr.next();

                if (entry.getValue() + DOWNLOAD_TIMEOUT < now) {
                    logger.debug("Failed to download block #{}, expired", entry.getKey());
                    toDownload.add(entry.getKey());
                    itr.remove();
                }
            }

            // quit if too many unfinished jobs
            if (toReceive.size() > MAX_PENDING_JOBS) {
                logger.trace("Max pending jobs reached");
                return;
            }

            // quit if no more tasks
            if (toDownload.isEmpty()) {
                return;
            }
            Long task = toDownload.first();

            // quit if too many pending blocks
            int pendingBlocks = toValidate.size() + toImport.size();
            if (pendingBlocks > MAX_PENDING_BLOCKS && task > toValidate.first().getKey().getNumber()) {
                logger.trace("Max pending blocks reached");
                return;
            }

            // get idle channels
            List<Channel> channels = channelMgr.getIdleChannels().stream()
                    .filter(channel -> {
                        Peer peer = channel.getRemotePeer();
                        // the peer has the block
                        return peer.getLatestBlockNumber() >= task
                                // AND is not banned
                                && !badPeers.contains(peer.getPeerId())
                        // AND supports FAST_SYNC if we enabled this protocol
                                && (!config.syncFastSync() || isFastSyncSupported(peer));
                    })
                    .collect(Collectors.toList());
            logger.trace("Qualified idle peers = {}", channels.size());

            // quit if no idle channels.
            if (channels.isEmpty()) {
                return;
            }
            // otherwise, pick a random channel
            Channel c = channels.get(random.nextInt(channels.size()));

            if (config.syncFastSync()) { // use FAST_SYNC protocol
                if (skipVotes(task)) {
                    logger.trace("Requesting block #{} from {}:{}, HEADER + TRANSACTIONS", task,
                            c.getRemoteIp(),
                            c.getRemotePort());
                    c.getMessageQueue().sendMessage(new GetBlockPartsMessage(task,
                            BlockPart.encode(BlockPart.HEADER, BlockPart.TRANSACTIONS)));
                } else {
                    logger.trace("Requesting block #{} from {}:{}, HEADER + TRANSACTIONS + VOTES", task,
                            c.getRemoteIp(), c.getRemotePort());
                    c.getMessageQueue().sendMessage(new GetBlockPartsMessage(task,
                            BlockPart.encode(BlockPart.HEADER, BlockPart.TRANSACTIONS, BlockPart.VOTES)));
                }
            } else { // use old protocol
                logger.trace("Requesting block #{} from {}:{}, FULL BLOCK", task, c.getRemoteIp(),
                        c.getRemotePort());
                c.getMessageQueue().sendMessage(new GetBlockMessage(task));
            }

            if (toDownload.remove(task)) {
                growToDownloadQueue();
            }
            toReceive.put(task, TimeUtil.currentTimeMillis());
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

        // find the check point
        long checkpoint = latest + 1;
        while (skipVotes(checkpoint)) {
            checkpoint++;
        }

        synchronized (lock) {
            // Add missing blocks to currentBlockBatch
            Iterator<Pair<Block, Channel>> iterator = toValidate.iterator();
            while (iterator.hasNext()) {
                Pair<Block, Channel> p = iterator.next();
                long n = p.getKey().getNumber();

                if (n <= latest) {
                    iterator.remove();
                } else if (n <= checkpoint) {
                    iterator.remove();
                    toImport.put(n, p);
                } else {
                    break;
                }
            }

            if (toImport.size() >= checkpoint - latest) {
                // Validate the block hashes
                boolean valid = validateBlockHashes(latest + 1, checkpoint);

                if (valid) {
                    for (long n = latest + 1; n <= checkpoint; n++) {
                        Pair<Block, Channel> p = toImport.remove(n);
                        boolean imported = chain.importBlock(p.getKey(), false);
                        if (!imported) {
                            // This should never happen
                            toDownload.add(n);
                            handleInvalidBlock(p.getKey(), p.getValue());
                        }
                    }
                    current.set(chain.getLatestBlockNumber() + 1);
                }
            }
        }
    }

    /**
     * Validate block hashes in the toImport set.
     *
     * Assuming that the whole block range is available in the set.
     *
     * @param from
     *            the start block number, inclusive
     * @param to
     *            the end block number, inclusive
     */
    protected boolean validateBlockHashes(long from, long to) {
        synchronized (lock) {
            // Validate votes for the last block in set
            Pair<Block, Channel> checkpoint = toImport.get(to);
            Block block = checkpoint.getKey();
            if (!chain.validateBlockVotes(block)) {
                handleInvalidBlock(block, checkpoint.getValue());
                return false;
            }

            for (long n = to - 1; n >= from; n--) {
                Pair<Block, Channel> current = toImport.get(n);
                Pair<Block, Channel> child = toImport.get(n + 1);

                if (!Arrays.equals(current.getKey().getHash(), child.getKey().getParentHash())) {
                    toImport.remove(n);
                    toDownload.add(n);

                    handleInvalidBlock(current.getKey(), current.getValue());
                    return false;
                }
            }

            return true;
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
        logger.info("Invalid block, peer = {}:{}, block # = {}", a.getAddress().getHostAddress(), a.getPort(),
                block.getNumber());
        synchronized (lock) {
            toDownload.add(block.getNumber());
            toReceive.remove(block.getNumber());
            toImport.remove(block.getNumber(), Pair.of(block, channel));
            toValidate.remove(Pair.of(block, channel));
        }

        badPeers.add(channel.getRemotePeer().getPeerId());

        if (config.syncDisconnectOnInvalidBlock()) {
            // disconnect if the peer sends us invalid block
            channel.getMessageQueue().disconnect(ReasonCode.BAD_PEER);
        }
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
