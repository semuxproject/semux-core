/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.semux.core.Block;
import org.semux.core.SyncManager;
import org.semux.crypto.Key;
import org.semux.gui.Action;
import org.semux.gui.AddressBook;
import org.semux.net.Peer;
import org.semux.util.ByteArray;

/**
 * A Model stores all the data that GUI needs. The thread-safety of this class
 * is achieved by swapping pointers instead of synchronization.
 */
public class WalletModel {

    private List<ActionListener> listeners = new CopyOnWriteArrayList<>();

    private SyncManager.Progress syncProgress;

    private Block latestBlock;

    private Key coinbase;
    private Status status;

    private volatile Map<ByteArray, Integer> accountNo = new HashMap<>();
    private volatile List<WalletAccount> accounts = new ArrayList<>();
    private volatile List<WalletDelegate> delegates = new ArrayList<>();

    private Map<String, Peer> activePeers = new HashMap<>();

    private AddressBook addressBook;

    /**
     * Fires an model update event.
     */
    public void fireUpdateEvent() {
        updateView();
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
     * Getter for property ${@link #syncProgress}.
     *
     * @return Value to set for property ${@link #syncProgress}.
     */
    public SyncManager.Progress getSyncProgress() {
        return syncProgress;
    }

    /**
     * Setter for property ${@link #syncProgress}.
     *
     * @param syncProgress
     *            Value to set for property ${@link #syncProgress}.
     */
    public void setSyncProgress(SyncManager.Progress syncProgress) {
        this.syncProgress = syncProgress;
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
    public Key getCoinbase() {
        return coinbase;
    }

    /**
     * Set the coinbase.
     * 
     * @param coinbase
     */
    public void setCoinbase(Key coinbase) {
        this.coinbase = coinbase;
    }

    /**
     * Returns the account status.
     * 
     * @return
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the account status.
     * 
     * @param status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Get the total available.
     * 
     * @return
     */
    public long getTotalAvailable() {
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

    /**
     * Sets the address book.
     * 
     * @param addressBook
     */
    public void setAddressBook(AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    /**
     * Returns the address book.
     * 
     * @return
     */
    public AddressBook getAddressBook() {
        return addressBook;
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

    public List<WalletDelegate> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<WalletDelegate> delegates) {
        this.delegates = delegates;
    }

    public Map<String, Peer> getActivePeers() {
        return activePeers;
    }

    public void setActivePeers(Map<String, Peer> activePeers) {
        this.activePeers = activePeers;
    }

    /**
     * Updates MVC view.
     */
    protected void updateView() {
        for (ActionListener listener : listeners) {
            EventQueue.invokeLater(() -> listener.actionPerformed(new ActionEvent(this, 0, Action.REFRESH.name())));
        }
    }

    public static enum Status {
        NORMAL, DELEGATE, VALIDATOR
    }
}
