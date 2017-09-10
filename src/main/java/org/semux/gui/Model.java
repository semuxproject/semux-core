package org.semux.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.semux.core.Delegate;
import org.semux.core.Transaction;
import org.semux.crypto.EdDSA;

/**
 * A Model stores all the data that GUI needs. The thread-safety of this class
 * is achieved by swapping pointers instead of synchronization.
 */
public class Model {

    private List<ActionListener> listeners = new ArrayList<>();

    private long latestBlockNumber;

    private int coinbase;
    private boolean isDelegate;

    private volatile List<Account> accounts = new ArrayList<>();
    private volatile List<Delegate> delegates = new ArrayList<>();

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
        List<Account> list = new ArrayList<>();
        for (EdDSA key : keys) {
            list.add(new Account(key));
        }
        accounts = list;
    }

    /**
     * Get the latested block number.
     * 
     * @return
     */
    public long getLatestBlockNumber() {
        return latestBlockNumber;
    }

    /**
     * Set the latest block number.
     * 
     * @param latestBlockNumber
     */
    public void setLatestBlockNumber(long latestBlockNumber) {
        this.latestBlockNumber = latestBlockNumber;
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
        for (Account acc : accounts) {
            sum += acc.getBalance();
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
        for (Account acc : accounts) {
            sum += acc.getLocked();
        }
        return sum;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public List<Delegate> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<Delegate> delegates) {
        this.delegates = delegates;
    }

    public static class Account {
        private EdDSA key;
        private long nonce;
        private long balance;
        private long locked;
        private List<Transaction> transactions = new ArrayList<>();

        public Account(EdDSA key) {
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

        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
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
