/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.Config;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.utils.ArrayUtil;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pending manager maintains all unconfirmed transactions, either from kernel or
 * network. It also pre-evaluate transactions and broadcast them, if valid, to
 * its peers.
 * 
 * TODO: prevent transaction bombard
 * 
 * TODO: sort transactions by fee, and other metric
 *
 */
public class PendingManager implements Runnable, BlockchainListener {

    private static final Logger logger = LoggerFactory.getLogger(PendingManager.class);

    private static final ThreadFactory factory = new ThreadFactory() {

        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "pending-mgr-" + cnt.getAndIncrement());
        }
    };

    private static final int CACHE_SIZE = 100 * 1024;
    private static final int MAX_NONCE_JUMP = 1024;

    private Blockchain chain;
    private ChannelManager channelMgr;
    private AccountState pendingAS;
    private DelegateState pendingDS;

    /**
     * Transaction queue.
     */
    private LinkedList<Transaction> queue = new LinkedList<>();

    /**
     * Transaction pool.
     */
    private Object poolLock = new Object();
    private Map<ByteArray, Transaction> poolMap = new HashMap<>();
    private List<Transaction> transactions = new ArrayList<>();
    private List<TransactionResult> results = new ArrayList<>();

    /**
     * Transaction cache.
     */
    private Map<ByteArray, Transaction> cache = Collections.synchronizedMap(new LRUMap<>(CACHE_SIZE));

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> validateFuture;

    private volatile boolean isRunning;

    /**
     * Create a pending manager.
     */
    public PendingManager(Blockchain chain, ChannelManager channelMgr) {
        this.chain = chain;
        this.channelMgr = channelMgr;
        this.pendingAS = chain.getAccountState().track();
        this.pendingDS = chain.getDeleteState().track();

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Start this pending manager.
     */
    public void start() {
        if (!isRunning) {
            /*
             * Use a rate smaller than the message queue sending rate to prevent message
             * queues from hitting the NET_MAX_QUEUE_SIZE, especially when the network load
             * is heavy.
             */
            long rate = Config.NET_MAX_QUEUE_RATE * 2;
            this.validateFuture = exec.scheduleAtFixedRate(this, 0, rate, TimeUnit.MILLISECONDS);

            this.chain.addListener(this);

            logger.debug("Pending manager started");
            this.isRunning = true;
        }
    }

    /**
     * Shut down this pending manager.
     */
    public void stop() {
        if (isRunning) {
            validateFuture.cancel(true);

            logger.debug("Pending manager stopped");
            isRunning = false;
        }
    }

    /**
     * Check if the pending manager is running or not.
     * 
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get a copy of the queue, for test purpose only.
     * 
     * @return
     */
    public List<Transaction> getQueue() {
        synchronized (queue) {
            return new ArrayList<>(queue);
        }
    }

    /**
     * Add a transaction to the queue, which will be validated by a background
     * worker.
     * 
     * @param tx
     */
    public void addTransaction(Transaction tx) {
        queue.add(tx);
    }

    /**
     * Add a transaction to the pool.
     * 
     * @param tx
     * @return true if the transaction is successfully added to the pool, otherwise
     *         false
     */
    public boolean addTransactionSync(Transaction tx) {
        return processTransaction(tx, true) >= 1;
    }

    /**
     * Get the nonce of an account from the pending state.
     * 
     * @param address
     * @return
     */
    public long getNonce(byte[] address) {
        return pendingAS.getAccount(address).getNonce();
    }

    /**
     * Get pending transactions and corresponding results.
     * 
     * @param limit
     * @return
     */
    public Pair<List<Transaction>, List<TransactionResult>> getTransactionsAndResults(int limit) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionResult> res = new ArrayList<>();

        synchronized (poolLock) {
            if (transactions.size() > limit && limit != -1) {
                txs.addAll(transactions.subList(0, limit));
                res.addAll(results.subList(0, limit));
            } else {
                txs.addAll(transactions);
                res.addAll(results);
            }
        }

        return Pair.of(txs, res);
    }

    /**
     * Get a limited number of transactions in the pool.
     * 
     * @param limit
     * @return
     */
    public List<Transaction> getTransactions(int limit) {
        return getTransactionsAndResults(limit).getLeft();
    }

    /**
     * Get all transactions in the pool.
     * 
     * @return
     */
    public List<Transaction> getTransactions() {
        return getTransactionsAndResults(-1).getLeft();
    }

    @Override
    public void onBlockAdded(Block block) {
        if (isRunning) {
            long t1 = System.currentTimeMillis();

            // [1] reset state
            pendingAS = chain.getAccountState().track();
            pendingDS = chain.getDeleteState().track();

            synchronized (poolLock) {
                // [2] clear transaction pool
                List<Transaction> txs = new ArrayList<>(transactions);
                poolMap.clear();
                transactions.clear();
                results.clear();

                // [3] update pending state
                long accepted = 0;
                for (Transaction tx : txs) {
                    accepted += processTransaction(tx, false);
                }

                long t2 = System.currentTimeMillis();
                logger.debug("Pending tx evaluation: # txs = {} / {},  time =  {} ms", accepted, txs.size(), t2 - t1);
            }
        }
    }

    @Override
    public void run() {
        Transaction tx;

        while ((tx = queue.poll()) != null) {
            if (tx.validate() && processTransaction(tx, true) >= 1) {
                // break after one transaction
                return;
            }
        }
    }

    /**
     * Validate the given transaction. Add it to the pool if valid.
     * 
     * @param tx
     *            transaction
     * @param relay
     *            whether to relay the transaction if valid
     * @return the number of valid transactions processed
     */
    protected int processTransaction(Transaction tx, boolean relay) {

        // check timestamp. this is not part of validation protocol
        long now = System.currentTimeMillis();
        long twoHours = TimeUnit.HOURS.toMillis(2);
        if (tx.getTimestamp() < now - twoHours || tx.getTimestamp() > now + twoHours) {
            return 0;
        }

        AccountState as = pendingAS.track();
        DelegateState ds = pendingDS.track();
        TransactionExecutor exec = new TransactionExecutor();

        Account acc = as.getAccount(tx.getFrom());
        long nonce = acc.getNonce();
        int cnt = 0;
        while (tx != null && tx.getNonce() == nonce) {
            // execute transactions
            TransactionResult result = exec.execute(tx, as, ds, false);

            if (result.isValid()) {
                // commit state updates
                as.commit();
                ds.commit();

                // add transaction to pool
                synchronized (poolLock) {
                    poolMap.put(createKey(tx), tx);
                    transactions.add(tx);
                    results.add(result);
                }

                // relay transaction
                if (relay) {
                    List<Channel> channels = channelMgr.getActiveChannels();
                    int[] indexes = ArrayUtil.permutation(channels.size());
                    for (int i = 0; i < indexes.length && i < Config.NET_RELAY_REDUNDANCY; i++) {
                        if (channels.get(indexes[i]).isActive()) {
                            TransactionMessage msg = new TransactionMessage(tx);
                            channels.get(indexes[i]).getMessageQueue().sendMessage(msg);
                        }
                    }
                }
            } else {
                // exit immediately if invalid
                return cnt;
            }

            nonce++;
            cnt++;
            tx = cache.get(createKey(tx.getFrom(), nonce));
        }

        // add to cache
        if (tx != null && tx.getNonce() > nonce && tx.getNonce() < nonce + MAX_NONCE_JUMP) {
            cache.put(createKey(tx), tx);
        }

        return cnt;
    }

    private ByteArray createKey(Transaction tx) {
        return ByteArray.of(Bytes.merge(tx.getFrom(), Bytes.of(tx.getNonce())));
    }

    private ByteArray createKey(byte[] acc, long nonce) {
        return ByteArray.of(Bytes.merge(acc, Bytes.of(nonce)));
    }
}
