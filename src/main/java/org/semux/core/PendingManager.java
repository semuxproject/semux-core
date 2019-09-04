/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.ethereum.vm.client.BlockStore;
import org.semux.Kernel;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.net.Channel;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Pending manager maintains all unconfirmed transactions, either from kernel or
 * network. All transactions are evaluated and propagated to peers if success.
 *
 * Note that: the transaction results in pending manager are not reliable for VM
 * transactions because these are evaluated against a dummy block. Nevertheless,
 * transactions included by the pending manager are eligible for inclusion in
 * block proposing phase.
 *
 * TODO: sort transaction queue by fee, and other metrics
 */
public class PendingManager implements Runnable, BlockchainListener {

    private static final Logger logger = LoggerFactory.getLogger(PendingManager.class);

    private static final ThreadFactory factory = new ThreadFactory() {

        private final AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "pending-" + cnt.getAndIncrement());
        }
    };

    public static final long ALLOWED_TIME_DRIFT = TimeUnit.HOURS.toMillis(2);

    private static final int QUEUE_SIZE_LIMIT = 128 * 1024;
    private static final int VALID_TXS_LIMIT = 16 * 1024;
    private static final int LARGE_NONCE_TXS_LIMIT = 32 * 1024;
    private static final int PROCESSED_TXS_LIMIT = 128 * 1024;

    private final Kernel kernel;
    private final BlockStore blockStore;
    private AccountState pendingAS;
    private DelegateState pendingDS;
    private SemuxBlock dummyBlock;

    // Transactions that haven't been processed
    private final LinkedHashMap<ByteArray, Transaction> queue = new LinkedHashMap<>();

    // Transactions that have been processed and are valid for block production
    private final ArrayList<PendingTransaction> validTxs = new ArrayList<>();

    // Transactions whose nonce is too large, compared to the sender's nonce
    private final Cache<ByteArray, Transaction> largeNonceTxs = Caffeine.newBuilder().maximumSize(LARGE_NONCE_TXS_LIMIT)
            .build();

    // Transactions that have been processed, including both valid and invalid ones
    private final Cache<ByteArray, Long> processedTxs = Caffeine.newBuilder().maximumSize(PROCESSED_TXS_LIMIT).build();

    private final ScheduledExecutorService exec;

    private ScheduledFuture<?> validateFuture;

    private volatile boolean isRunning;

    /**
     * Creates a pending manager.
     */
    public PendingManager(Kernel kernel) {
        this.kernel = kernel;
        this.blockStore = new SemuxBlockStore(kernel.getBlockchain());

        this.pendingAS = kernel.getBlockchain().getAccountState().track();
        this.pendingDS = kernel.getBlockchain().getDelegateState().track();
        this.dummyBlock = kernel.createEmptyBlock();

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Starts this pending manager.
     */
    public synchronized void start() {
        if (!isRunning) {
            /*
             * NOTE: a rate smaller than the message queue sending rate should be used to
             * prevent message queues from hitting the NET_MAX_QUEUE_SIZE, especially when
             * the network load is heavy.
             */
            this.validateFuture = exec.scheduleAtFixedRate(this, 2, 2, TimeUnit.MILLISECONDS);

            kernel.getBlockchain().addListener(this);

            logger.debug("Pending manager started");
            this.isRunning = true;
        }
    }

    /**
     * Shuts down this pending manager.
     */
    public synchronized void stop() {
        if (isRunning) {
            validateFuture.cancel(true);

            logger.debug("Pending manager stopped");
            isRunning = false;
        }
    }

    /**
     * Returns whether the pending manager is running or not.
     *
     * @return
     */
    public synchronized boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns a copy of the queue, for test purpose only.
     *
     * @return
     */
    public synchronized List<Transaction> getQueue() {
        return new ArrayList<>(queue.values());
    }

    /**
     * Adds a transaction to the queue, which will be validated later by the
     * background worker. Transaction may get rejected if the queue is full.
     *
     * @param tx
     */
    public synchronized void addTransaction(Transaction tx) {
        ByteArray hash = ByteArray.of(tx.getHash());

        if (queue.size() < QUEUE_SIZE_LIMIT
                && processedTxs.getIfPresent(hash) == null
                && tx.validate(kernel.getConfig().network())) {
            // NOTE: re-insertion doesn't affect item order
            queue.put(ByteArray.of(tx.getHash()), tx);
        }
    }

    /**
     * Adds a transaction to the pool and waits until it's done.
     *
     * @param tx
     *            The transaction
     * @return The processing result
     */
    public synchronized ProcessingResult addTransactionSync(Transaction tx) {
        // nonce check for transactions from this client
        if (tx.getNonce() != getNonce(tx.getFrom())) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_NONCE);
        }

        if (tx.validate(kernel.getConfig().network())) {
            // proceed with the tx, ignoring transaction queue size limit
            return processTransaction(tx, false, true);
        } else {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_FORMAT);
        }
    }

    /**
     * Returns the nonce of an account based on the pending state.
     *
     * @param address
     * @return
     */
    public synchronized long getNonce(byte[] address) {
        return pendingAS.getAccount(address).getNonce();
    }

    /**
     * Returns pending transactions, limited by the given total size in bytes.
     *
     *
     * @return
     */
    public synchronized List<PendingTransaction> getPendingTransactions(long blockGasLimit) {
        List<PendingTransaction> txs = new ArrayList<>();
        Iterator<PendingTransaction> it = validTxs.iterator();

        while (it.hasNext() && blockGasLimit > 0) {
            PendingTransaction tx = it.next();

            long gasUsage = tx.transaction.isVMTransaction() ? tx.result.getGasUsed()
                    : kernel.getConfig().spec().nonVMTransactionGasCost();
            if (blockGasLimit > gasUsage) {
                txs.add(tx);
                blockGasLimit -= gasUsage;
            }
        }

        return txs;
    }

    /**
     * Returns all pending transactions.
     *
     * @return
     */
    public synchronized List<PendingTransaction> getPendingTransactions() {
        return getPendingTransactions(Long.MAX_VALUE);
    }

    /**
     * Resets the pending state and returns all pending transactions.
     *
     * @return
     */
    public synchronized List<PendingTransaction> reset() {
        // reset state
        pendingAS = kernel.getBlockchain().getAccountState().track();
        pendingDS = kernel.getBlockchain().getDelegateState().track();
        dummyBlock = kernel.createEmptyBlock();

        // clear transaction pool
        List<PendingTransaction> txs = new ArrayList<>(validTxs);
        validTxs.clear();

        return txs;
    }

    @Override
    public synchronized void onBlockAdded(Block block) {
        if (isRunning) {
            long t1 = TimeUtil.currentTimeMillis();

            // clear transaction pool
            List<PendingTransaction> txs = reset();

            // update pending state
            long accepted = 0;
            for (PendingTransaction tx : txs) {
                accepted += processTransaction(tx.transaction, true, false).accepted;
            }

            long t2 = TimeUtil.currentTimeMillis();
            logger.debug("Execute pending transactions: # txs = {} / {},  time = {} ms", accepted, txs.size(), t2 - t1);
        }
    }

    @Override
    public synchronized void run() {
        Iterator<Map.Entry<ByteArray, Transaction>> iterator = queue.entrySet().iterator();

        while (validTxs.size() < VALID_TXS_LIMIT && iterator.hasNext()) {
            // the eldest entry
            Map.Entry<ByteArray, Transaction> entry = iterator.next();
            iterator.remove();

            // reject already executed transactions
            if (processedTxs.getIfPresent(entry.getKey()) != null) {
                continue;
            }

            // process the transaction
            int accepted = processTransaction(entry.getValue(), false, false).accepted;
            processedTxs.put(entry.getKey(), TimeUtil.currentTimeMillis());

            // include one tx per call
            if (accepted > 0) {
                break;
            }
        }
    }

    /**
     * Validates the given transaction and add to pool if success.
     *
     * @param tx
     *            a transaction
     * @param isIncludedBefore
     *            whether the transaction is included before
     * @param isFromThisNode
     *            whether the transaction is from this node
     * @return the number of transactions that have been included
     */
    protected ProcessingResult processTransaction(Transaction tx, boolean isIncludedBefore, boolean isFromThisNode) {

        int cnt = 0;
        long now = TimeUtil.currentTimeMillis();

        // reject VM transactions that come in before fork
        if (tx.isVMTransaction() && !kernel.getBlockchain().isForkActivated(Fork.VIRTUAL_MACHINE)) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_TYPE);
        }

        // reject VM transaction with low gas price
        if (tx.isVMTransaction() && tx.getGasPrice().lessThan(kernel.getConfig().poolMinGasPrice())) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_FEE);
        }

        // reject transactions with a duplicated tx hash
        if (kernel.getBlockchain().hasTransaction(tx.getHash())) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID);
        }

        // check transaction timestamp if this is a fresh transaction:
        // a time drift of 2 hours is allowed by default
        if (tx.getTimestamp() < now - kernel.getConfig().poolMaxTransactionTimeDrift()
                || tx.getTimestamp() > now + kernel.getConfig().poolMaxTransactionTimeDrift()) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_TIMESTAMP);
        }

        // report INVALID_NONCE error to prevent the transaction from being
        // silently ignored due to a low nonce
        if (tx.getNonce() < getNonce(tx.getFrom())) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_NONCE);
        }

        // Check transaction nonce: pending transactions must be executed sequentially
        // by nonce in ascending order. In case of a nonce jump, the transaction is
        // delayed for the next event loop of PendingManager.
        while (tx != null && tx.getNonce() == getNonce(tx.getFrom())) {

            // execute transactions
            AccountState as = pendingAS.track();
            DelegateState ds = pendingDS.track();
            TransactionResult result = new TransactionExecutor(kernel.getConfig(), blockStore).execute(tx,
                    as, ds, dummyBlock, kernel.getBlockchain().isVMEnabled(), 0);

            if (result.getCode().isAcceptable()) {
                // commit state updates
                as.commit();
                ds.commit();

                // Add the successfully processed transaction into the pool of transactions
                // which are ready to be proposed to the network.
                PendingTransaction pendingTransaction = new PendingTransaction(tx, result);
                validTxs.add(pendingTransaction);
                cnt++;

                // If a transaction is not included before, send it to the network now
                if (!isIncludedBefore) {
                    // if it is from myself, broadcast it to everyone
                    broadcastTransaction(tx, isFromThisNode);
                }
            } else {
                // exit immediately if invalid
                return new ProcessingResult(cnt, result.getCode());
            }

            tx = largeNonceTxs.getIfPresent(createKey(tx.getFrom(), getNonce(tx.getFrom())));
            isIncludedBefore = false; // A large-nonce transaction is not included before
        }

        // Delay the transaction for the next event loop of PendingManager. The delayed
        // transaction is expected to be processed once PendingManager has received
        // all of its preceding transactions from the same address.
        if (tx != null && tx.getNonce() > getNonce(tx.getFrom())) {
            largeNonceTxs.put(createKey(tx), tx);
        }

        return new ProcessingResult(cnt);
    }

    private void broadcastTransaction(Transaction tx, boolean toAllPeers) {
        List<Channel> channels = kernel.getChannelManager().getActiveChannels();

        // If not to all peers, randomly pick n channels
        int n = kernel.getConfig().netRelayRedundancy();
        if (!toAllPeers && channels.size() > n) {
            Collections.shuffle(channels);
            channels = channels.subList(0, n);
        }

        // Send the message
        TransactionMessage msg = new TransactionMessage(tx);
        for (Channel c : channels) {
            if (c.isActive()) {
                c.getMessageQueue().sendMessage(msg);
            }
        }
    }

    private ByteArray createKey(Transaction tx) {
        return ByteArray.of(Bytes.merge(tx.getFrom(), Bytes.of(tx.getNonce())));
    }

    private ByteArray createKey(byte[] acc, long nonce) {
        return ByteArray.of(Bytes.merge(acc, Bytes.of(nonce)));
    }

    /**
     * This object represents a transaction and its execution result against a
     * snapshot of local state that is not yet confirmed by the network.
     */
    public static class PendingTransaction {

        public final Transaction transaction;

        public final TransactionResult result;

        public PendingTransaction(Transaction transaction, TransactionResult result) {
            this.transaction = transaction;
            this.result = result;
        }
    }

    /**
     * This object represents the number of accepted transactions and the cause of
     * rejection by ${@link PendingManager}.
     */
    public static class ProcessingResult {

        public final int accepted;

        public final TransactionResult.Code error;

        public ProcessingResult(int accepted, TransactionResult.Code error) {
            this.accepted = accepted;
            this.error = error;
        }

        public ProcessingResult(int accepted) {
            this.accepted = accepted;
            this.error = null;
        }
    }
}
