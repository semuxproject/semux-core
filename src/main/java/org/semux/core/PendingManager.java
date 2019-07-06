/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import org.semux.crypto.Key;
import org.semux.net.Channel;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.util.ArrayUtil;
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
 * TODO: sort transaction queue by fee, and other metrics
 *
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

    private static final int QUEUE_MAX_SIZE = 128 * 1024;
    private static final int TRANSACTIONS_MAX_SIZE = 16 * 1024;
    private static final int DELAYED_MAX_SIZE = 32 * 1024;
    private static final int PROCESSED_MAX_SIZE = 32 * 1024;

    private final Kernel kernel;
    private final BlockStore blockStore;
    private AccountState pendingAS;
    private DelegateState pendingDS;

    /**
     * Transaction queue.
     */
    private final LinkedList<Transaction> queue = new LinkedList<>();
    private final HashSet<Transaction> queueSet = new HashSet<>();

    /**
     * Transaction pool.
     */
    private final List<PendingTransaction> transactions = new ArrayList<>();

    /**
     * Transaction cache.
     */
    private final Cache<ByteArray, Transaction> delayed = Caffeine.newBuilder().maximumSize(DELAYED_MAX_SIZE).build();
    private final Cache<ByteArray, Transaction> processed = Caffeine.newBuilder().maximumSize(PROCESSED_MAX_SIZE)
            .build();

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
        if (queue.size() < QUEUE_MAX_SIZE
                && tx.validate(kernel.getConfig().network())
                && !queueSet.contains(tx)) {

            queue.add(tx);
            queueSet.add(tx);
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

        if (/* queue/transactions limits are ignored */ tx.validate(kernel.getConfig().network())) {
            return processTransaction(tx, true);
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
        int byteLimit = 1024 * 1024;

        // TODO: include transactions based on the block gas limit

        List<PendingTransaction> txs = new ArrayList<>();
        Iterator<PendingTransaction> it = transactions.iterator();

        int size = 0;
        while (it.hasNext()) {
            PendingTransaction tx = it.next();

            size += tx.transaction.size();
            if (size > byteLimit) {
                break;
            } else {
                txs.add(tx);
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

        // clear transaction pool
        List<PendingTransaction> txs = new ArrayList<>(transactions);
        transactions.clear();

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
                accepted += processTransaction(tx.transaction, false).accepted;
            }

            long t2 = TimeUtil.currentTimeMillis();
            logger.debug("Execute pending transactions: # txs = {} / {},  time = {} ms", accepted, txs.size(), t2 - t1);
        }
    }

    @Override
    public synchronized void run() {
        Transaction tx;

        while (transactions.size() < TRANSACTIONS_MAX_SIZE && (tx = queue.poll()) != null) {

            queueSet.remove(tx);
            // reject already executed transactions
            ByteArray key = ByteArray.of(tx.getHash());
            if (processed.getIfPresent(key) != null) {
                continue;
            }

            // process the transaction
            boolean accepted = processTransaction(tx, true).accepted >= 1;
            processed.put(key, tx);

            // quit after one accepted transaction
            if (accepted) {
                return;
            }
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
    protected ProcessingResult processTransaction(Transaction tx, boolean relay) {

        int cnt = 0;
        long now = TimeUtil.currentTimeMillis();
        boolean isVMTransaction = tx.getType() == TransactionType.CALL || tx.getType() == TransactionType.CREATE;

        // reject VM transactions that come in before fork
        if (isVMTransaction && !kernel.getBlockchain().isForkActivated(Fork.VIRTUAL_MACHINE)) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_TYPE);
        }
        // reject VM transaction with low gas price
        if (isVMTransaction && tx.getGasPrice().lt(kernel.getConfig().poolMinGasPrice())) {
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
        // silently
        // ignored due to a low nonce
        if (tx.getNonce() < getNonce(tx.getFrom())) {
            return new ProcessingResult(0, TransactionResult.Code.INVALID_NONCE);
        }

        // Check transaction nonce: pending transactions must be executed sequentially
        // by nonce in ascending order. In case of a nonce jump, the transaction is
        // delayed for the next event loop of PendingManager.
        while (tx != null && tx.getNonce() == getNonce(tx.getFrom())) {

            // TODO: introduce block state (open, closed, signed, imported)

            // create a dummy block (Note: VM transaction results may depends on the block)
            Blockchain chain = kernel.getBlockchain();
            Block prevBlock = chain.getLatestBlock();
            BlockHeader blockHeader = new BlockHeader(
                    prevBlock.getNumber() + 1,
                    new Key().toAddress(), prevBlock.getHash(), System.currentTimeMillis(), new byte[0],
                    new byte[0], new byte[0], new byte[0]);
            SemuxBlock block = new SemuxBlock(blockHeader, kernel.getConfig().spec().maxBlockGasLimit());

            // execute transactions
            AccountState as = pendingAS.track();
            DelegateState ds = pendingDS.track();
            TransactionResult result = new TransactionExecutor(kernel.getConfig(), blockStore).execute(tx,
                    as, ds, block, chain);

            if (result.getCode().isAcceptable()) {
                // commit state updates
                as.commit();
                ds.commit();

                // Add the successfully processed transaction into the pool of transactions
                // which are ready to be proposed to the network.
                PendingTransaction pendingTransaction = new PendingTransaction(tx, result);
                transactions.add(pendingTransaction);
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
                return new ProcessingResult(cnt, result.getCode());
            }

            tx = delayed.getIfPresent(createKey(tx.getFrom(), getNonce(tx.getFrom())));
        }

        // Delay the transaction for the next event loop of PendingManager. The delayed
        // transaction is expected to be processed once PendingManager has received all
        // of its preceding transactions from the same address.
        if (tx != null && tx.getNonce() > getNonce(tx.getFrom())) {
            delayed.put(createKey(tx), tx);
        }

        return new ProcessingResult(cnt);
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
