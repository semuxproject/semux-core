/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.semux.core.Transaction;
import org.semux.core.state.Account;
import org.semux.crypto.Key;

public class WalletAccount extends Account {
    private Key key;
    private String name;
    private List<Transaction> transactions = new ArrayList<>();

    public WalletAccount(Key key, Account acc, String name) {
        super(acc.getAddress(), acc.getAvailable(), acc.getLocked(), acc.getNonce());
        this.key = key;
        this.name = name;

        if (!Arrays.equals(key.toAddress(), acc.getAddress())) {
            throw new IllegalArgumentException("Key and account does not match");
        }
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return key.toString();
    }
}