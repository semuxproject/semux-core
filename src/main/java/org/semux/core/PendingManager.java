/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.semux.Config;
import org.semux.core.state.AccountState;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.utils.ArrayUtil;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pending transaction manager that organizes all the received transactions.
 * 
 * TODO: prevent transaction bombard
 * 
 * TODO: sort the pending transactions by fee, and other metric
 *
 */
public class PendingManager implements Runnable, BlockchainListener, Comparator<Transaction> {

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
    private AccountState accountState;

    /**
     * Transaction queue.
     */
    private LinkedList<Transaction> queue = new LinkedList<>();

    /**
     * Transaction pool.
     */
    private Map<ByteArray, Transaction> pool = new HashMap<>();

    /**
     * Transaction cache.
     */
    private Map<ByteArray, Transaction> cache = new LRUMap<>(CACHE_SIZE);

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> validateFuture;

    private volatile boolean isRunning;

    /**
     * Create a pending manager.
     */
    public PendingManager() {
        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Start this pending manager.
     */
    public synchronized void start(Blockchain chain, ChannelManager channelMgr) {
        if (!isRunning) {
            this.chain = chain;
            this.channelMgr = channelMgr;
            this.accountState = chain.getAccountState().track();

            /*
             * Use a rate smaller than the message queue sending rate to prevent message
             * queues from hitting the NET_MAX_QUEUE_SIZE, especially when the network load
             * is heavy.
             */
            long rate = Config.NET_MAX_QUEUE_RATE * 2;
            this.validateFuture = exec.scheduleAtFixedRate(this, 0, rate, TimeUnit.MILLISECONDS);

            logger.debug("Pending manager started");
            this.isRunning = true;
        }
    }

    /**
     * Shut down this pending manager.
     */
    public synchronized void stop() {
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
    public synchronized boolean isRunning() {
        return isRunning;
    }

    /**
     * Get a copy of the queue, for test purpose only.
     * 
     * @return
     */
    public synchronized List<Transaction> getQueue() {
        return new ArrayList<>(queue);
    }

    /**
     * Add a transaction to the queue, which will be validated by a background
     * worker.
     * 
     * @param tx
     */
    public synchronized void addTransaction(Transaction tx) {
        queue.add(tx);
    }

    /**
     * Get a limited number of transactions in the pool.
     * 
     * @param limit
     * @return
     */
    public synchronized List<Transaction> getTransactions(int limit) {
        List<Transaction> list = new ArrayList<>();
        synchronized (pool) {
            list.addAll(pool.values());
        }

        list.sort(this);

        if (limit >= 0) {
            return list.size() > limit ? list.subList(0, limit) : list;
        }
        return list;
    }

    /**
     * Get all transactions in the pool.
     * 
     * @return
     */
    public synchronized List<Transaction> getTransactions() {
        return getTransactions(-1);
    }

    /**
     * Remove transactions from the pool.
     * 
     * @param txs
     */
    public synchronized void removeTransactions(List<Transaction> txs) {
        for (Transaction tx : txs) {
            pool.remove(createKey(tx));
        }
    }

    /**
     * Remove a transaction from the pool.
     * 
     * @param tx
     */
    public synchronized void removeTransaction(Transaction tx) {
        pool.remove(createKey(tx));
    }

    /**
     * Get the nonce of an account.
     * 
     * @param address
     * @return
     */
    public synchronized long getAccountNonce(byte[] address) {
        return accountState.getAccount(address).getNonce();
    }

    @Override
    public synchronized void onBlockAdded(Block block) {
        if (isRunning) {
            long t1 = System.currentTimeMillis();

            // [1] remove included transaction
            removeTransactions(block.getTransactions());

            // [2] reset state
            accountState = chain.getAccountState().track();

            // [3] clear transaction pool
            List<Transaction> txs = new ArrayList<>(pool.values());
            txs.sort(this);
            pool.clear();
            long t2 = System.currentTimeMillis();

            // [4] update state
            long total = 0;
            for (Transaction tx : txs) {
                total += processTransaction(tx, false);
            }

            long t3 = System.currentTimeMillis();
            logger.debug("Pending tx evaluation: # txs = {} / {},  time = {} + {} ms", total, txs.size(), t2 - t1,
                    t3 - t2);
        }
    }

    @Override
    public synchronized void run() {
        for (Transaction tx; (tx = queue.poll()) != null;) {

            if (!pool.containsKey(createKey(tx)) // not in pool
                    && chain.getTransaction(tx.getHash()) == null // not in chain
                    && tx.validate()) { // valid

                // process transaction
                processTransaction(tx, true);

                // break after one transaction
                return;
            }
        }
    }

    private int processTransaction(Transaction tx, boolean relay) {
        Account acc = accountState.getAccount(tx.getFrom());

        // [1] check balance
        if (tx.getValue() > acc.getBalance()) {
            return 0;
        }

        // [2] check nonce
        long nonce = acc.getNonce() + 1;
        int cnt = 0;
        while (tx != null && tx.getNonce() == nonce) {
            // [3] increase nonce
            acc.setNonce(nonce);

            // [4] add transaction to pool
            pool.put(createKey(tx), tx);

            // [5] relay transaction
            if (relay) {
                List<Channel> channels = channelMgr.getActiveChannels();
                int[] indexes = ArrayUtil.permutation(Math.min(Config.NET_RELAY_REDUNDANCY, channels.size()));
                for (int idx : indexes) {
                    if (channels.get(idx).isActive()) {
                        TransactionMessage msg = new TransactionMessage(tx);
                        channels.get(idx).getMessageQueue().sendMessage(msg);
                    }
                }
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

    @Override
    public int compare(Transaction tx1, Transaction tx2) {
        int c = Long.compare(tx1.getTimestamp(), tx2.getTimestamp());
        return (c != 0) ? c : Long.compare(tx1.getNonce(), tx2.getNonce());
    }
}
