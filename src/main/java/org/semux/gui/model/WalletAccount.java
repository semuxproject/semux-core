/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import java.util.ArrayList;
import java.util.List;

import org.semux.core.Transaction;
import org.semux.core.state.Account;
import org.semux.crypto.EdDSA;

public class WalletAccount extends Account {
    private EdDSA key;
    private List<Transaction> transactions = new ArrayList<>();

    public WalletAccount(EdDSA key, Account acc) {
        super(acc.getAddress(), acc.getAvailable(), acc.getLocked(), acc.getNonce());
        this.key = key;
    }

    public EdDSA getKey() {
        return key;
    }

    public void setKey(EdDSA key) {
        this.key = key;
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