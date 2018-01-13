/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.Kernel;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.net.Channel;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.util.ArrayUtil;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Pending manager maintains all unconfirmed transactions, either from kernel or
 * network. All transactions are evaluated and propagated to peers if success.
 *
 * TODO: sort transaction queue by fee, and other metrics
 *
 */
public class PendingManager implements Runnable, BlockchainListener {

    private static final Logger logger = LoggerFactory.getLogger(PendingManager.class);

    private static final ThreadFactory factory = new ThreadFactory() {

        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "pending-" + cnt.getAndIncrement());
        }
    };

    public static final long ALLOWED_TIME_DRIFT = TimeUnit.HOURS.toMillis(2);

    private static final int QUEUE_MAX_SIZE = 64 * 1024;
    private static final int POOL_MAX_SIZE = 8 * 1024;
    private static final int DELAYED_MAX_SIZE = 16 * 1024;
    private static final int PROCESSED_MAX_SIZE = 16 * 1024;

    private Kernel kernel;
    private AccountState pendingAS;
    private DelegateState pendingDS;

    /**
     * Transaction queue.
     */
    private LinkedList<Transaction> queue = new LinkedList<>();

    /**
     * Transaction pool.
     */
    private Map<ByteArray, PendingTransaction> pool = new HashMap<>();
    private List<PendingTransaction> transactions = new ArrayList<>();

    /**
     * Transaction cache.
     */
    private Cache<ByteArray, Transaction> delayed = Caffeine.newBuilder().maximumSize(DELAYED_MAX_SIZE).build();
    private Cache<ByteArray, Transaction> processed = Caffeine.newBuilder().maximumSize(PROCESSED_MAX_SIZE).build();

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> validateFuture;

    private volatile boolean isRunning;

    /**
     * Creates a pending manager.
     */
    public PendingManager(Kernel kernel) {
        this.kernel = kernel;

        this.pendingAS = kernel.getBlockchain().getAccountState().track();
        this.pendingDS = kernel.getBlockchain().getDelegateState().track();

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
        return new ArrayList<>(queue);
    }

    /**
     * Adds a transaction to the queue, which will be validated later by the
     * background worker. Transaction may get rejected if the queue is full.
     *
     * @param tx
     */
    public synchronized void addTransaction(Transaction tx) {
        if (queue.size() < QUEUE_MAX_SIZE) {
            queue.add(tx);
        }
    }

    /**
     * Adds a transaction to the pool and waits until it's done.
     *
     * @param tx
     *            The transaction
     * @return The processing result
     */
    public synchronized ProcessTransactionResult addTransactionSync(Transaction tx) {
        return tx.validate(kernel.getConfig().network()) ? processTransaction(tx, true)
                : new ProcessTransactionResult(0, TransactionResult.Error.INVALID_FORMAT);
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
     * Returns pending transactions and corresponding results.
     *
     * @param limit
     * @return
     */
    public synchronized List<PendingTransaction> getPendingTransactions(int limit) {
        List<PendingTransaction> txs = new ArrayList<>();

        if (limit == -1) {
            // returns all transactions if there is no limit
            txs.addAll(transactions);
        } else {
            Iterator<PendingTransaction> it = transactions.iterator();

            int size = 0;
            while (it.hasNext()) {
                PendingTransaction tx = it.next();

                if (size + tx.transaction.size() > limit) {
                    break;
                } else {
                    txs.add(tx);
                    size += tx.transaction.size();
                }
            }
        }

        return txs;
    }

    /**
     * Returns transactions in the pool, with the given total size limit.
     *
     * @param limit
     * @return
     */
    public synchronized List<PendingTransaction> getTransactions(int limit) {
        return getPendingTransactions(limit);
    }

    /**
     * Returns all transactions in the pool.
     *
     * @return
     */
    public synchronized List<PendingTransaction> getTransactions() {
        return getPendingTransactions(-1);
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

        // clear transaction pool
        List<PendingTransaction> txs = new ArrayList<>(transactions);
        pool.clear();
        transactions.clear();

        return txs;
    }

    @Override
    public synchronized void onBlockAdded(Block block) {
        if (isRunning) {
            long t1 = System.currentTimeMillis();

            // clear transaction pool
            List<PendingTransaction> txs = reset();

            // update pending state
            long accepted = 0;
            for (PendingTransaction tx : txs) {
                accepted += processTransaction(tx.transaction, false).accepted;
            }

            long t2 = System.currentTimeMillis();
            logger.debug("Pending tx evaluation: # txs = {} / {},  time =  {} ms", accepted, txs.size(), t2 - t1);
        }
    }

    @Override
    public synchronized void run() {
        Transaction tx;

        while (pool.size() < POOL_MAX_SIZE
                && (tx = queue.poll()) != null
                && tx.getFee() >= kernel.getConfig().minTransactionFee()) {

            // reject already executed transactions
            ByteArray key = ByteArray.of(tx.getHash());
            if (processed.getIfPresent(key) != null) {
                continue;
            }

            if (tx.validate(kernel.getConfig().network()) && processTransaction(tx, true).accepted >= 1) {
                // exit after one success transaction
                return;
            }

            processed.put(key, tx);
        }
    }

    /**
     * Validates the given transaction and add to pool if success.
     *
     * @param tx
     *            transaction
     * @param relay
     *            whether to relay the transaction if success
     * @return the number of success transactions processed and the error that
     *         interrupted the process
     */
    protected ProcessTransactionResult processTransaction(Transaction tx, boolean relay) {

        // NOTE: assume transaction format is valid

        int cnt = 0;
        long now = System.currentTimeMillis();

        // reject transactions with a duplicated tx hash
        if (kernel.getBlockchain().hasTransaction(tx.getHash())) {
            return new ProcessTransactionResult(0, TransactionResult.Error.DUPLICATED_HASH);
        }

        // check transaction timestamp if this is a fresh transaction:
        // a time drift of 2 hours is allowed by default
        if (tx.getTimestamp() < now - kernel.getConfig().maxTransactionTimeDrift()
                || tx.getTimestamp() > now + kernel.getConfig().maxTransactionTimeDrift()) {
            return new ProcessTransactionResult(cnt, TransactionResult.Error.INVALID_TIMESTAMP);
        }

        // Check transaction nonce: pending transactions must be executed sequentially
        // by nonce in ascending order. In case of a nonce jump, the transaction is
        // delayed for the next event loop of PendingManager.
        while (tx != null && tx.getNonce() == getNonce(tx.getFrom())) {

            // execute transactions
            AccountState as = pendingAS.track();
            DelegateState ds = pendingDS.track();
            TransactionResult result = new TransactionExecutor(kernel.getConfig()).execute(tx, as, ds);

            if (result.isSuccess()) {
                // commit state updates
                as.commit();
                ds.commit();

                // Add the successfully processed transaction into the pool of transactions
                // which are ready to be proposed to the network.
                PendingTransaction pendingTransaction = new PendingTransaction(tx, result);
                transactions.add(pendingTransaction);
                pool.put(createKey(tx), pendingTransaction);
                cnt++;

                // relay transaction
                if (relay) {
                    List<Channel> channels = kernel.getChannelManager().getActiveChannels();
                    TransactionMessage msg = new TransactionMessage(tx);
                    int[] indices = ArrayUtil.permutation(channels.size());
                    for (int i = 0; i < indices.length && i < kernel.getConfig().netRelayRedundancy(); i++) {
                        Channel c = channels.get(indices[i]);
                        if (c.isActive()) {
                            c.getMessageQueue().sendMessage(msg);
                        }
                    }
                }
            } else {
                // exit immediately if invalid
                return new ProcessTransactionResult(cnt, result.getError());
            }

            tx = delayed.getIfPresent(createKey(tx.getFrom(), getNonce(tx.getFrom())));
        }

        // Delay the transaction for the next event loop of PendingManager. The delayed
        // transaction is expected to be processed once PendingManager has received all
        // of its preceding transactions from the same address.
        if (tx != null && tx.getNonce() > getNonce(tx.getFrom())) {
            delayed.put(createKey(tx), tx);
        }

        return new ProcessTransactionResult(cnt);
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

        public final TransactionResult transactionResult;

        private PendingTransaction(Transaction transaction, TransactionResult transactionResult) {
            this.transaction = transaction;
            this.transactionResult = transactionResult;
        }
    }

    /**
     * This object represents the number of accepted transactions and the cause of
     * rejection by ${@link PendingManager}.
     */
    public static class ProcessTransactionResult {

        public final int accepted;

        public final TransactionResult.Error error;

        public ProcessTransactionResult(int accepted, TransactionResult.Error error) {
            this.accepted = accepted;
            this.error = error;
        }

        public ProcessTransactionResult(int accepted) {
            this.accepted = accepted;
            this.error = null;
        }
    }
}
