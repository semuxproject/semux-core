package org.semux.gui;

import java.util.ArrayList;
import java.util.List;

import org.semux.core.Delegate;
import org.semux.core.Transaction;
import org.semux.crypto.EdDSA;

/**
 * A Model stores all the data that GUI needs.
 */
public class Model {

    private List<Account> accounts = new ArrayList<>();

    /**
     * Construct a new model.
     */
    public Model() {

    }

    /**
     * Initialize model with the given list of accounts.
     * 
     * @param keys
     */
    public void init(List<EdDSA> keys) {
        for (EdDSA key : keys) {
            accounts.add(new Account(key));
        }
    }

    /**
     * Get all accounts.
     * 
     * @return
     */
    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Get the i-th account.
     * 
     * @param idx
     * @return
     */
    public Account getAccount(int idx) {
        return accounts.get(idx);
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

    public interface ModelListener {
        /**
         * Notify the listener when model updates.
         */
        public void onUpdate();
    }

    public static class Account {
        private EdDSA address;
        private Delegate delegate;
        private long balance;
        private long locked;
        private List<Transaction> incomingTxs = new ArrayList<>();
        private List<Transaction> outgoingTxs = new ArrayList<>();

        public Account(EdDSA address) {
            this.address = address;
        }

        public EdDSA getAddress() {
            return address;
        }

        public void setAddress(EdDSA address) {
            this.address = address;
        }

        public Delegate getDelegate() {
            return delegate;
        }

        public void setDelegate(Delegate delegate) {
            this.delegate = delegate;
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

        public List<Transaction> getIncomingTxs() {
            return incomingTxs;
        }

        public void setIncomingTxs(List<Transaction> incomingTxs) {
            this.incomingTxs = incomingTxs;
        }

        public List<Transaction> getOutgoingTxs() {
            return outgoingTxs;
        }

        public void setOutgoingTxs(List<Transaction> outgoingTxs) {
            this.outgoingTxs = outgoingTxs;
        }
    }
}
