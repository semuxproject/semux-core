/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.semux.crypto.AES;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.util.Bytes;
import org.semux.util.IOUtil;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wallet {

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    private static final int VERSION = 1;

    private File file;
    private String password;

    private final List<EdDSA> accounts = Collections.synchronizedList(new ArrayList<>());

    /**
     * Create a new wallet instance.
     * 
     * @param file
     *            the wallet file
     */
    public Wallet(File file) {
        this.file = file;
    }

    /**
     * Unlock this wallet.
     * 
     * @param password
     *            the wallet password
     * @return true if the wallet is successfully unlocked, otherwise false
     */
    public boolean unlock(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password can not be null");
        }

        if (unlocked()) {
            return true;
        }

        try {
            byte[] key = Hash.h256(Bytes.of(password));

            if (exists()) {
                SimpleDecoder dec = new SimpleDecoder(IOUtil.readFile(file));
                dec.readInt(); // version
                int total = dec.readInt();

                for (int i = 0; i < total; i++) {
                    byte[] iv = dec.readBytes();
                    byte[] publicKey = dec.readBytes();
                    byte[] privateKey = AES.decrypt(dec.readBytes(), key, iv);

                    accounts.add(new EdDSA(publicKey, privateKey));
                }
            }
            this.password = password;
            return true;
        } catch (CryptoException | InvalidKeySpecException e) {
            logger.error("Failed to decrypt the wallet data");
        } catch (IOException e) {
            logger.error("Failed to open wallet", e);
        }

        return false;
    }

    /**
     * Lock the wallet.
     */
    public void lock() {
        accounts.clear();
        password = null;
    }

    /**
     * Check whether the wallet file exists.
     * 
     * @return
     */
    public boolean exists() {
        return file.isFile() && file.length() > 0;
    }

    /**
     * Delete the wallet file.
     */
    public void delete() throws IOException {
        Files.delete(file.toPath());
    }

    /**
     * Check if this wallet is unlocked.
     * 
     * @return true if the wallet is locked, otherwise false
     */
    public boolean unlocked() {
        return password != null;
    }

    /**
     * Get the number of accounts in the wallet.
     * 
     * @return
     */
    public int size() {
        return accounts.size();
    }

    /**
     * Returns the file where the wallet is persisted.
     * 
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the password.
     * 
     * @return
     * @throws WalletLockedException
     */
    public String getPassword() throws WalletLockedException {
        requireUnlocked();

        return password;
    }

    /**
     * Get a copy of the accounts inside this wallet.
     * 
     * @return a list of accounts
     * @throws WalletLockedException
     */
    public List<EdDSA> getAccounts() throws WalletLockedException {
        requireUnlocked();

        synchronized (accounts) {
            return new ArrayList<>(accounts);
        }
    }

    /**
     * Set the accounts inside this wallet.
     * 
     * DANGER: this method will remove all accounts in this wallet.
     * 
     * @param list
     */
    public void setAccounts(List<EdDSA> list) throws WalletLockedException {
        requireUnlocked();

        synchronized (accounts) {
            accounts.clear();
            accounts.addAll(list);
        }
    }

    /**
     * Get account by index.
     * 
     * @param idx
     *            account index, starting from 0
     * @return
     * @throws WalletLockedException
     */
    public EdDSA getAccount(int idx) throws WalletLockedException {
        requireUnlocked();

        return accounts.get(idx);
    }

    /**
     * Get account by address.
     * 
     * @param address
     * @return
     * @throws WalletLockedException
     */
    public EdDSA getAccount(byte[] address) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize account query
        synchronized (accounts) {
            for (EdDSA key : accounts) {
                if (Arrays.equals(key.toAddress(), address)) {
                    return key;
                }
            }
        }

        return null;
    }

    /**
     * Add a new account to the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     * 
     * @param newKey
     *            a new account
     * @return true if the new account was successfully added, false otherwise
     * @throws WalletLockedException
     * 
     */
    public boolean addAccount(EdDSA newKey) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize duplicates check
        synchronized (accounts) {
            for (EdDSA key : accounts) {
                if (Arrays.equals(key.getPublicKey(), newKey.getPublicKey())) {
                    return false;
                }
            }

            accounts.add(newKey);
            return true;
        }
    }

    /**
     * Add a list of accounts to the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     *
     * @return the number accounts added
     * @throws WalletLockedException
     * 
     */
    public int addAccounts(List<EdDSA> accounts) throws WalletLockedException {
        int n = 0;
        for (EdDSA acc : accounts) {
            n += addAccount(acc) ? 1 : 0;
        }
        return n;
    }

    /**
     * Delete an account in the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     * 
     * @param key
     *            account to delete
     * @return true if the account was successfully deleted, false otherwise
     * @throws WalletLockedException
     * 
     */
    public boolean deleteAccount(EdDSA key) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize duplicates check
        synchronized (accounts) {
            for (EdDSA k : accounts) {
                if (Arrays.equals(k.getPublicKey(), key.getPublicKey())) {
                    accounts.remove(key);
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Change the password of the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     * 
     * @param newPassword
     *            a new password
     * @throws WalletLockedException
     */
    public void changePassword(String newPassword) throws WalletLockedException {
        requireUnlocked();

        if (newPassword == null) {
            throw new IllegalArgumentException("Password can not be null");
        }

        this.password = newPassword;
    }

    /**
     * Flush this wallet into the disk.
     *
     * @return true if the wallet has been flushed into disk successfully, otherwise
     *         false
     * @throws WalletLockedException
     */
    public boolean flush() throws WalletLockedException {
        requireUnlocked();

        try {
            byte[] key = Hash.h256(Bytes.of(password));

            SimpleEncoder enc = new SimpleEncoder();
            enc.writeInt(VERSION);
            enc.writeInt(accounts.size());

            synchronized (accounts) {
                for (EdDSA a : accounts) {
                    byte[] iv = Bytes.random(16);

                    enc.writeBytes(iv);
                    enc.writeBytes(a.getPublicKey());
                    enc.writeBytes(AES.encrypt(a.getPrivateKey(), key, iv));
                }
            }

            IOUtil.writeToFile(enc.toBytes(), file);
            return true;
        } catch (CryptoException e) {
            logger.error("Failed to encrypt the  wallet");
        } catch (IOException e) {
            logger.error("Failed to write wallet to disk", e);
        }

        return false;
    }

    private void requireUnlocked() throws WalletLockedException {
        if (!unlocked()) {
            throw new WalletLockedException();
        }
    }
}
