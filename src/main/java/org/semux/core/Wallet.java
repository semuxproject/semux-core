/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.semux.Config;
import org.semux.crypto.AES;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.utils.Bytes;
import org.semux.utils.IOUtil;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wallet {

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    private static final String WALLET_FILE = "wallet.data";
    private static final int VERSION = 1;

    private List<EdDSA> accounts = Collections.synchronizedList(new ArrayList<>());
    private boolean isLocked = true;
    private String password = null;

    private static Wallet instance;

    /**
     * Get the singleton instance of wallet.
     * 
     * @return
     */
    public synchronized static Wallet getInstance() {
        if (instance == null) {
            instance = new Wallet();
        }
        return instance;
    }

    private Wallet() {
    }

    /**
     * Unlock this wallet.
     * 
     * @param password
     *            the wallet password
     * @return true if the wallet is successfully unlocked, otherwise false
     */
    public synchronized boolean unlock(String password) {
        if (!isLocked()) {
            return true;
        }

        try {
            File f = new File(Config.DATA_DIR, WALLET_FILE);
            byte[] key = Hash.h256(Bytes.of(password));

            if (f.exists()) {
                SimpleDecoder dec = new SimpleDecoder(IOUtil.readFile(f));
                dec.readInt(); // version
                int total = dec.readInt();

                for (int i = 0; i < total; i++) {
                    byte[] iv = dec.readBytes();
                    byte[] publicKey = dec.readBytes();
                    byte[] privateKey = AES.decrypt(dec.readBytes(), key, iv);

                    accounts.add(new EdDSA(publicKey, privateKey));
                }
            }

            this.isLocked = false;
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
     * Check whether the wallet file exists.
     * 
     * @return
     */
    public boolean exists() {
        File f = new File(Config.DATA_DIR, WALLET_FILE);

        return f.isFile();
    }

    /**
     * Lock the wallet.
     */
    public void lock() {
        if (!isLocked()) {
            accounts.clear();
            isLocked = true;
            password = null;
        }
    }

    /**
     * Check if this wallet is locked.
     * 
     * @return true if the wallet is locked, otherwise false
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Get the list of accounts inside this wallet.
     * 
     * @return a list of accounts
     * @throws WalletLockedException
     */
    public List<EdDSA> getAccounts() throws WalletLockedException {
        requireUnlocked();

        return new ArrayList<>(accounts);
    }

    /**
     * Set the accounts inside this wallet.
     * 
     * DANGER: this method will remove all accounts in this wallet.
     * 
     * @param accounts
     */
    public void setAccounts(List<EdDSA> accounts) throws WalletLockedException {
        requireUnlocked();

        this.accounts = new ArrayList<>(accounts);
    }

    /**
     * Get account by index.
     * 
     * @param idx
     *            index, starting from 0
     * @return
     * @throws WalletLockedException
     */
    public EdDSA getAccount(int idx) throws WalletLockedException {
        requireUnlocked();

        if (idx >= 0 && idx < accounts.size()) {
            return accounts.get(idx);
        } else {
            return null;
        }
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
     * @throws WalletLockedException
     * 
     */
    public void addAccount(EdDSA newKey) throws WalletLockedException {
        requireUnlocked();

        accounts.add(newKey);
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

        this.password = newPassword;
    }

    /**
     * Flush this wallet into the disk.
     * 
     * @param keys
     * @return true if the wallet has been flushed into disk successfully, otherwise
     *         false
     * @throws WalletLockedException
     */
    public synchronized boolean flush() throws WalletLockedException {
        requireUnlocked();

        try {
            File f = new File(Config.DATA_DIR, WALLET_FILE);
            byte[] key = Hash.h256(Bytes.of(password));

            SimpleEncoder enc = new SimpleEncoder();
            enc.writeInt(VERSION);
            enc.writeInt(accounts.size());

            for (EdDSA a : accounts) {
                byte[] iv = Bytes.random(16);

                enc.writeBytes(iv);
                enc.writeBytes(a.getPublicKey());
                enc.writeBytes(AES.encrypt(a.getPrivateKey(), key, iv));
            }

            IOUtil.writeToFile(enc.toBytes(), f);
            return true;
        } catch (CryptoException e) {
            logger.error("Failed to encrypt the  wallet");
        } catch (IOException e) {
            logger.error("Failed to write wallet to disk", e);
        }

        return false;
    }

    private void requireUnlocked() throws WalletLockedException {
        if (isLocked()) {
            throw new WalletLockedException();
        }
    }

    private static String createLine(int width) {
        char[] buf = new char[width];
        Arrays.fill(buf, '-');
        return new String(buf);
    }

    public static void doCreate(String password) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = Wallet.getInstance();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        System.out.println(createLine(80));
        System.out.println("Address     : " + key.toString());
        System.out.println("Public key  : " + Hex.encode(key.getPublicKey()));
        System.out.println("Private key : " + Hex.encode(key.getPrivateKey()));
        System.out.println(createLine(80));

        if (wallet.flush()) {
            System.out.println("The created account has been stored in your wallet.");
        }
    }

    public static void doList(String password) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = Wallet.getInstance();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();

        String line = String.format("+-%-3s-+-%-45s-+", createLine(3), createLine(45));
        System.out.println(line);
        for (int i = 0; i < accounts.size(); i++) {
            System.out.println(String.format("| %-3d | %-45s |", i, accounts.get(i).toString()));
            System.out.println(line);
        }
    }
}
