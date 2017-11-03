package org.semux.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.semux.core.Block;
import org.semux.core.Transaction;
import org.semux.core.state.Delegate;
import org.semux.crypto.EdDSA;
import org.semux.net.Peer;
import org.semux.utils.ByteArray;

/**
 * A Model stores all the data that GUI needs. The thread-safety of this class
 * is achieved by swapping pointers instead of synchronization.
 */
public class Model {

    private List<ActionListener> listeners = new CopyOnWriteArrayList<>();

    private Block latestBlock;

    private int coinbase;
    private boolean isDelegate;

    private volatile Map<ByteArray, Integer> accountNo = new HashMap<>();
    private volatile List<WalletAccount> accounts = new ArrayList<>();
    private volatile List<Delegate> delegates = new ArrayList<>();

    private Map<String, Peer> activePeers = new HashMap<>();

    /**
     * Construct a new model.
     */
    public Model() {
    }

    /**
     * Add a listener.
     * 
     * @param listener
     */
    public void addListener(ActionListener listener) {
        listeners.add(listener);
    }

    /**
     * Fire an update event to all listeners.
     */
    public void fireUpdateEvent() {
        for (ActionListener listener : listeners) {
            EventQueue.invokeLater(() -> {
                listener.actionPerformed(new ActionEvent(this, 0, Action.REFRESH.name()));
            });
        }
    }

    /**
     * Initialize model with the given list of accounts.
     * 
     * @param keys
     */
    public void init(List<EdDSA> keys) {
        List<WalletAccount> list = new ArrayList<>();
        for (EdDSA key : keys) {
            list.add(new WalletAccount(key));
        }
        accounts = list;
    }

    /**
     * Get the latest block.
     * 
     * @return
     */
    public Block getLatestBlock() {
        return latestBlock;
    }

    /**
     * Set the latest block.
     * 
     * @param latestBlock
     */
    public void setLatestBlock(Block latestBlock) {
        this.latestBlock = latestBlock;
    }

    /**
     * Get the coinbase.
     * 
     * @return
     */
    public int getCoinbase() {
        return coinbase;
    }

    /**
     * Set the coinbase.
     * 
     * @param coinbase
     */
    public void setCoinbase(int coinbase) {
        this.coinbase = coinbase;
    }

    /**
     * Check if the coinbase account is a delegate.
     * 
     * @return
     */
    public boolean isDelegate() {
        return isDelegate;
    }

    /**
     * Set whether the coinbase account is a delegate.
     * 
     * @param isDelegate
     */
    public void setDelegate(boolean isDelegate) {
        this.isDelegate = isDelegate;
    }

    /**
     * Get the total balance.
     * 
     * @return
     */
    public long getTotalBalance() {
        long sum = 0;
        for (WalletAccount acc : accounts) {
            sum += acc.getAvailable();
        }
        return sum;
    }

    /**
     * Get the total locked.
     * 
     * @return
     */
    public long getTotalLocked() {
        long sum = 0;
        for (WalletAccount acc : accounts) {
            sum += acc.getLocked();
        }
        return sum;
    }

    public List<WalletAccount> getAccounts() {
        return accounts;
    }

    public int getAccountNumber(byte[] address) {
        Integer n = accountNo.get(ByteArray.of(address));
        return n == null ? -1 : n;
    }

    public void setAccounts(List<WalletAccount> accounts) {
        Map<ByteArray, Integer> map = new HashMap<>();
        for (int i = 0; i < accounts.size(); i++) {
            map.put(ByteArray.of(accounts.get(i).getKey().toAddress()), i);
        }
        this.accountNo = map;
        this.accounts = accounts;
    }

    public List<Delegate> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<Delegate> delegates) {
        this.delegates = delegates;
    }

    public Map<String, Peer> getActivePeers() {
        return activePeers;
    }

    public void setActivePeers(Map<String, Peer> activePeers) {
        this.activePeers = activePeers;
    }

    public static class WalletAccount {
        private EdDSA key;
        private long nonce;
        private long available;
        private long locked;
        private List<Transaction> transactions = new ArrayList<>();

        public WalletAccount(EdDSA key) {
            this.key = key;
        }

        public EdDSA getKey() {
            return key;
        }

        public void setKey(EdDSA key) {
            this.key = key;
        }

        public long getNonce() {
            return nonce;
        }

        public void setNonce(long nonce) {
            this.nonce = nonce;
        }

        public long getAvailable() {
            return available;
        }

        public void setBalance(long balance) {
            this.available = balance;
        }

        public long getLocked() {
            return locked;
        }

        public void setLocked(long locked) {
            this.locked = locked;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public String toString() {
            return key.toString();
        }
    }
}
