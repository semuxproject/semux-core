/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.semux.core.Amount.ZERO;
import static org.semux.core.Amount.sum;

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
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.SyncManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.util.ByteArray;
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

    private static final long MAX_DOWNLOAD_TIME = 10L * 1000L; // 10 seconds

    private static final int MAX_UNFINISHED_JOBS = 16;

    private static final int MAX_QUEUED_BLOCKS = 8192;
    private static final int MAX_PENDING_BLOCKS = 512;

    private static final Random random = new Random();

    private Kernel kernel;
    private Config config;

    private Blockchain chain;
    private ChannelManager channelMgr;

    // task queues
    private AtomicLong latestQueuedTask = new AtomicLong();
    private TreeSet<Long> toDownload = new TreeSet<>();
    private Map<Long, Long> toComplete = new HashMap<>();
    private TreeSet<Pair<Block, Channel>> toProcess = new TreeSet<>(
            Comparator.comparingLong(o -> o.getKey().getNumber()));
    private final Object lock = new Object();

    // current and target heights
    private AtomicLong begin = new AtomicLong();
    private AtomicLong current = new AtomicLong();
    private AtomicLong target = new AtomicLong();

    private Instant beginningInstant;
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
            beginningInstant = Instant.now();

            logger.info("Syncing started, best known block = {}", targetHeight - 1);

            // [1] set up queues
            synchronized (lock) {
                toDownload.clear();
                toComplete.clear();
                toProcess.clear();

                begin.set(chain.getLatestBlockNumber() + 1);
                current.set(chain.getLatestBlockNumber() + 1);
                target.set(targetHeight);
                latestQueuedTask.set(chain.getLatestBlockNumber());
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

    @Override
    public void onMessage(Channel channel, Message msg) {
        if (!isRunning()) {
            return;
        }

        switch (msg.getCode()) {
        case BLOCK: {
            BlockMessage blockMsg = (BlockMessage) msg;
            Block block = blockMsg.getBlock();
            synchronized (lock) {
                if (toDownload.remove(block.getNumber())) {
                    growToDownloadQueue();
                }
                toComplete.remove(block.getNumber());
                toProcess.add(Pair.of(block, channel));
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

        synchronized (lock) {
            // filter all expired tasks
            long now = System.currentTimeMillis();
            Iterator<Entry<Long, Long>> itr = toComplete.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Long, Long> entry = itr.next();

                if (entry.getValue() + MAX_DOWNLOAD_TIME < now) {
                    logger.debug("Downloading of block #{} has expired", entry.getKey());
                    toDownload.add(entry.getKey());
                    itr.remove();
                }
            }

            // quit if too many unfinished jobs
            if (toComplete.size() > MAX_UNFINISHED_JOBS) {
                logger.trace("Max unfinished jobs reached");
                return;
            }

            // quit if no more tasks
            if (toDownload.isEmpty()) {
                return;
            }
            Long task = toDownload.first();

            // quit if too many pending blocks
            if (toProcess.size() > MAX_PENDING_BLOCKS && task > toProcess.first().getKey().getNumber()) {
                logger.trace("Pending block queue is full");
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
                logger.debug("Request block #{} from channel = {}", task, c.getId());
                c.getMessageQueue().sendMessage(new GetBlockMessage(task));

                if (toDownload.remove(task)) {
                    growToDownloadQueue();
                }
                toComplete.put(task, System.currentTimeMillis());
            }
        }
    }

    /**
     * Queue new tasks sequentially starting from
     * ${@link SemuxSync#latestQueuedTask} until the size of
     * ${@link SemuxSync#toDownload} queue is greater than or equal to
     * ${@value MAX_QUEUED_BLOCKS}
     */
    private void growToDownloadQueue() {
        // To avoid overhead, this method doesn't add new tasks before the queue is less
        // than half-filled
        if (toDownload.size() >= MAX_QUEUED_BLOCKS / 2) {
            return;
        }

        for (long task = latestQueuedTask.get() + 1; //
                task < target.get() && toDownload.size() < MAX_QUEUED_BLOCKS; //
                task++) {
            latestQueuedTask.accumulateAndGet(task, (prev, next) -> next > prev ? next : prev);
            if (!chain.hasBlock(task)) {
                toDownload.add(task);
            }
        }
    }

    private void process() {
        if (!isRunning()) {
            return;
        }

        long latest = chain.getLatestBlockNumber();
        if (latest + 1 >= target.get()) {
            stop();
            return; // This is important because stop() only notify
        }

        Pair<Block, Channel> pair = null;
        synchronized (lock) {
            Iterator<Pair<Block, Channel>> iterator = toProcess.iterator();
            while (iterator.hasNext()) {
                Pair<Block, Channel> p = iterator.next();

                if (p.getKey().getNumber() <= latest) {
                    iterator.remove();
                } else if (p.getKey().getNumber() == latest + 1) {
                    iterator.remove();
                    pair = p;
                    break;
                } else {
                    break;
                }
            }
        }

        if (pair != null) {
            logger.info("{}", pair.getKey());

            if (validateApplyBlock(pair.getKey())) {
                synchronized (lock) {
                    if (toDownload.remove(pair.getKey().getNumber())) {
                        growToDownloadQueue();
                    }
                    toComplete.remove(pair.getKey().getNumber());
                }
            } else {
                InetSocketAddress a = pair.getValue().getRemoteAddress();
                logger.info("Invalid block from {}:{}", a.getAddress().getHostAddress(), a.getPort());

                synchronized (lock) {
                    toDownload.add(pair.getKey().getNumber());
                    toComplete.remove(pair.getKey().getNumber());
                }

                // disconnect if the peer sends us invalid block
                pair.getValue().getMessageQueue().disconnect(ReasonCode.BAD_PEER);
            }
        }
    }

    /**
     * Check if a block is valid, and apply to the chain if yes.
     *
     * @param block
     * @return
     */
    protected boolean validateApplyBlock(Block block) {
        AccountState as = chain.getAccountState().track();
        DelegateState ds = chain.getDelegateState().track();

        return validateBlock(block, as, ds) && applyBlock(block, as, ds);
    }

    protected boolean validateBlock(Block block, AccountState asSnapshot, DelegateState dsSnapshot) {
        BlockHeader header = block.getHeader();
        List<Transaction> transactions = block.getTransactions();

        // [1] check block header
        Block latest = chain.getLatestBlock();
        if (!Block.validateHeader(latest.getHeader(), header)) {
            logger.error("Invalid block header");
            return false;
        }

        // validate checkpoint
        if (config.checkpoints().containsKey(header.getNumber()) &&
                !Arrays.equals(ArrayUtils.toObject(header.getHash()), config.checkpoints().get(header.getNumber()))) {
            logger.error("Checkpoint validation failed, checkpoint is {} => {}, getting {}", header.getNumber(),
                    Hex.encode0x(ArrayUtils.toPrimitive(config.checkpoints().get(header.getNumber()))),
                    Hex.encode0x(header.getHash()));
            return false;
        }

        // blocks should never be forged by coinbase magic account
        if (Arrays.equals(header.getCoinbase(), Constants.COINBASE_ADDRESS)) {
            logger.error("A block forged by the coinbase magic account is not allowed");
            return false;
        }

        // [2] check transactions and results
        if (!Block.validateTransactions(header, transactions, config.network())
                || transactions.stream().mapToInt(Transaction::size).sum() > config.maxBlockTransactionsSize()) {
            logger.error("Invalid block transactions");
            return false;
        }
        if (!Block.validateResults(header, block.getResults())) {
            logger.error("Invalid results");
            return false;
        }

        if (transactions.stream().anyMatch(tx -> chain.hasTransaction(tx.getHash()))) {
            logger.error("Duplicated transaction hash is not allowed");
            return false;
        }

        // [3] evaluate transactions
        TransactionExecutor transactionExecutor = new TransactionExecutor(config);
        List<TransactionResult> results = transactionExecutor.execute(transactions, asSnapshot, dsSnapshot);
        if (!Block.validateResults(header, results)) {
            logger.error("Invalid transactions");
            return false;
        }

        // [4] evaluate votes
        return validateBlockVotes(block);
    }

    protected boolean validateBlockVotes(Block block) {
        Set<String> validators = new HashSet<>(chain.getValidators());
        int twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);

        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        byte[] encoded = vote.getEncoded();

        // check validity of votes
        if (!block.getVotes().stream()
                .allMatch(sig -> Key.verify(encoded, sig) && validators.contains(Hex.encode(sig.getAddress())))) {
            logger.debug("Block votes are invalid");
            return false;
        }

        // at least two thirds voters
        if (block.getVotes().stream()
                .map(sig -> new ByteArray(sig.getA()))
                .collect(Collectors.toSet()).size() < twoThirds) {
            logger.debug("Not enough votes, needs 2/3+");
            return false;
        }

        return true;
    }

    protected boolean applyBlock(Block block, AccountState asSnapshot, DelegateState dsSnapshot) {
        // [5] apply block reward and tx fees
        Amount txsReward = block.getTransactions().stream().map(Transaction::getFee).reduce(ZERO, Amount::sum);
        Amount reward = sum(config.getBlockReward(block.getNumber()), txsReward);

        if (reward.gt0()) {
            asSnapshot.adjustAvailable(block.getCoinbase(), reward);
        }

        // [6] commit the updates
        asSnapshot.commit();
        dsSnapshot.commit();

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

        current.set(block.getNumber() + 1);
        return true;
    }

    @Override
    public SemuxSyncProgress getProgress() {
        return new SemuxSyncProgress(
                begin.get(),
                current.get(),
                target.get(),
                Duration.between(beginningInstant != null ? beginningInstant : Instant.now(), Instant.now()));
    }

    public static class SemuxSyncProgress implements SyncManager.Progress {

        final long beginHeight;

        final long currentHeight;

        final long targetHeight;

        final Duration duration;

        public SemuxSyncProgress(long beginHeight, long currentHeight, long targetHeight, Duration duration) {
            this.beginHeight = beginHeight;
            this.currentHeight = currentHeight;
            this.targetHeight = targetHeight;
            this.duration = duration;
        }

        @Override
        public long getBeginHeight() {
            return beginHeight;
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
            Long downloadedBlocks = currentHeight - beginHeight;
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
