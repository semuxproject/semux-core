/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.Aes;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hash;
import org.semux.crypto.Key;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.IOUtil;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wallet {

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    private static final int VERSION = 2;

    private File file;
    private String password;

    private final List<Key> accounts = Collections.synchronizedList(new ArrayList<>());

    private final Map<ByteArray, String> aliases = new ConcurrentHashMap<>();

    /**
     * Creates a new wallet instance.
     * 
     * @param file
     *            the wallet file
     */
    public Wallet(File file) {
        this.file = file;
    }

    /**
     * Returns whether the wallet file exists and non-empty.
     * 
     * @return
     */
    public boolean exists() {
        return file.length() > 0;
    }

    /**
     * Deletes the wallet file.
     */
    public void delete() throws IOException {
        Files.delete(file.toPath());
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
     * Unlocks this wallet. If the wallet is unlocked, this method will reset the
     * account list on a successful trial.
     * 
     * @param password
     *            the wallet password
     * @return true if the wallet is successfully unlocked, otherwise false
     */
    public boolean unlock(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password can not be null");
        }

        try {
            byte[] key = Hash.h256(Bytes.of(password));

            if (exists()) {
                SimpleDecoder dec = new SimpleDecoder(IOUtil.readFile(file));
                int version = dec.readInt(); // version

                List<Key> newAccounts;
                Map<ByteArray, String> newAliases = new HashMap<>();
                switch (version) {
                case 1:
                    newAccounts = readAccounts(key, dec, false);
                    break;
                case 2:
                    newAccounts = readAccounts(key, dec, true);
                    newAliases = readAddressAliases(key, dec);
                    break;
                default:
                    throw new CryptoException("Unknown wallet version.");
                }

                synchronized (accounts) {
                    accounts.clear();
                    accounts.addAll(newAccounts);
                }
                synchronized (aliases) {
                    aliases.clear();
                    aliases.putAll(newAliases);
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
     * Reads the account keys.
     * 
     * @param dec
     * @param vlq
     * @return
     * @throws InvalidKeySpecException
     */
    protected List<Key> readAccounts(byte[] key, SimpleDecoder dec, boolean vlq) throws InvalidKeySpecException {
        List<Key> list = new ArrayList<>();
        int total = dec.readInt(); // size

        for (int i = 0; i < total; i++) {
            byte[] iv = dec.readBytes(vlq);
            byte[] publicKey = dec.readBytes(vlq);
            byte[] privateKey = Aes.decrypt(dec.readBytes(vlq), key, iv);

            list.add(new Key(privateKey, publicKey));
        }
        return list;
    }

    /**
     * Writes the account keys.
     * 
     * @param key
     * @param enc
     */
    protected void writeAccounts(byte[] key, SimpleEncoder enc) {
        synchronized (accounts) {
            enc.writeInt(accounts.size());
            for (Key a : accounts) {
                byte[] iv = Bytes.random(16);

                enc.writeBytes(iv);
                enc.writeBytes(a.getPublicKey());
                enc.writeBytes(Aes.encrypt(a.getPrivateKey(), key, iv));
            }
        }
    }

    /**
     * Reads the address book.
     * 
     * @param dec
     * @return
     */
    protected Map<ByteArray, String> readAddressAliases(byte[] key, SimpleDecoder dec) {
        byte[] iv = dec.readBytes();
        byte[] aliasesEncrypted = dec.readBytes();
        byte[] aliasesRaw = Aes.decrypt(aliasesEncrypted, key, iv);

        Map<ByteArray, String> map = new HashMap<>();
        SimpleDecoder d = new SimpleDecoder(aliasesRaw);
        int totalAddresses = d.readInt();
        for (int i = 0; i < totalAddresses; i++) {
            byte[] address = d.readBytes();
            String alias = d.readString();
            map.put(ByteArray.of(address), alias);
        }

        return map;
    }

    /**
     * Writes the address book.
     * 
     * @param enc
     */
    protected void writeAddressAliases(byte[] key, SimpleEncoder enc) {
        SimpleEncoder e = new SimpleEncoder();
        synchronized (aliases) {
            e.writeInt(aliases.size());
            for (Map.Entry<ByteArray, String> alias : aliases.entrySet()) {
                e.writeBytes(alias.getKey().getData());
                e.writeString(alias.getValue());
            }
        }

        byte[] iv = Bytes.random(16);
        byte[] aliasesRaw = e.toBytes();
        byte[] aliasesEncrypted = Aes.encrypt(aliasesRaw, key, iv);

        enc.writeBytes(iv);
        enc.writeBytes(aliasesEncrypted);
    }

    /**
     * Locks the wallet.
     */
    public void lock() {
        password = null;

        accounts.clear();
        aliases.clear();
    }

    /**
     * Returns whether the wallet is locked.
     * 
     * @return
     */
    public boolean isLocked() {
        return password == null;
    }

    /**
     * Returns if this wallet is unlocked.
     * 
     * @return true if the wallet is locked, otherwise false
     */
    public boolean isUnlocked() {
        return !isLocked();
    }

    /**
     * Returns the password.
     * 
     * @return
     * @throws WalletLockedException
     */
    public String getPassword() throws WalletLockedException {
        requireUnlocked();

        return password;
    }

    /**
     * Returns the number of accounts in the wallet.
     * 
     * @return
     * @throws WalletLockedException
     */
    public int size() {
        requireUnlocked();

        return accounts.size();
    }

    /**
     * Returns a copy of the accounts inside this wallet.
     * 
     * @return a list of accounts
     * @throws WalletLockedException
     */
    public List<Key> getAccounts() throws WalletLockedException {
        requireUnlocked();

        synchronized (accounts) {
            return new ArrayList<>(accounts);
        }
    }

    /**
     * Sets the accounts inside this wallet.
     * 
     * DANGER: this method will remove all accounts in this wallet.
     * 
     * @param list
     */
    public void setAccounts(List<Key> list) throws WalletLockedException {
        requireUnlocked();

        accounts.clear();
        for (Key key : list) {
            addAccount(key);
        }
    }

    /**
     * Returns account by index.
     * 
     * @param idx
     *            account index, starting from 0
     * @return
     * @throws WalletLockedException
     */
    public Key getAccount(int idx) throws WalletLockedException {
        requireUnlocked();

        return accounts.get(idx);
    }

    /**
     * Returns account by address.
     * 
     * @param address
     * @return
     * @throws WalletLockedException
     */
    public Key getAccount(byte[] address) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize account query
        synchronized (accounts) {
            for (Key key : accounts) {
                if (Arrays.equals(key.toAddress(), address)) {
                    return key;
                }
            }
        }

        return null;
    }

    /**
     * Adds a new account to the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     * 
     * @param newKey
     *            a new account
     * @return true if the new account was successfully added, false otherwise
     * @throws WalletLockedException
     * 
     */
    public boolean addAccount(Key newKey) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize duplicates check
        synchronized (accounts) {
            for (Key key : accounts) {
                if (Arrays.equals(key.getPublicKey(), newKey.getPublicKey())) {
                    return false;
                }
            }

            accounts.add(newKey);
            return true;
        }
    }

    /**
     * Adds a list of accounts to the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     *
     * @return the number accounts added
     * @throws WalletLockedException
     * 
     */
    public int addAccounts(List<Key> accounts) throws WalletLockedException {
        requireUnlocked();

        int n = 0;
        for (Key acc : accounts) {
            n += addAccount(acc) ? 1 : 0;
        }
        return n;
    }

    /**
     * Deletes an account in the wallet.
     * 
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     * 
     * @param key
     *            account to delete
     * @return true if the account was successfully deleted, false otherwise
     * @throws WalletLockedException
     * 
     */
    public boolean removeAccount(Key key) throws WalletLockedException {
        requireUnlocked();

        // TODO: optimize duplicates check
        synchronized (accounts) {
            for (Key k : accounts) {
                if (Arrays.equals(k.getPublicKey(), key.getPublicKey())) {
                    accounts.remove(key);
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Changes the password of the wallet.
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
     * Flushes this wallet into the disk.
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

            writeAccounts(key, enc);
            writeAddressAliases(key, enc);

            file.getParentFile().mkdirs();
            IOUtil.writeToFile(enc.toBytes(), file);
            return true;
        } catch (CryptoException e) {
            logger.error("Failed to encrypt the wallet");
        } catch (IOException e) {
            logger.error("Failed to write wallet to disk", e);
        }

        return false;
    }

    /**
     * Returns the alias of the given address.
     * 
     * @param address
     * @return
     */
    public Optional<String> getAddressAlias(byte[] address) throws WalletLockedException {
        requireUnlocked();

        return Optional.ofNullable(aliases.get(ByteArray.of(address)));
    }

    /**
     * Sets the alias of an address.
     * 
     * @param address
     * @param name
     * @throws WalletLockedException
     */
    public void setAddressAlias(byte[] address, String name) throws WalletLockedException {
        requireUnlocked();

        aliases.put(ByteArray.of(address), name);
    }

    /**
     * Returns the alias of an address.
     * 
     * @param address
     * @throws WalletLockedException
     */
    public void removeAddressAlias(byte[] address) throws WalletLockedException {
        requireUnlocked();

        aliases.remove(ByteArray.of(address));
    }

    /**
     * Returns all address aliases.
     * 
     * @return
     */
    public Map<ByteArray, String> getAddressAliases() throws WalletLockedException {
        requireUnlocked();

        return new HashMap<>(aliases);
    }

    private void requireUnlocked() throws WalletLockedException {
        if (!isUnlocked()) {
            throw new WalletLockedException();
        }
    }

    /**
     * Adds the addresses and aliases from another wallet
     *
     * @param w
     * @return
     */
    public int addWallet(Wallet w) {
        requireUnlocked();

        // import addresses
        int numImportedAddresses = addAccounts(w.getAccounts());

        // add address book entries
        Map<ByteArray, String> importedAliases = w.getAddressAliases();
        for (Map.Entry<ByteArray, String> alias : importedAliases.entrySet()) {
            byte[] address = alias.getKey().getData();

            // don't override existing wallet's aliases.
            if (!getAddressAlias(address).isPresent()) {
                setAddressAlias(address, alias.getValue());
            }
        }

        flush();
        return numImportedAddresses;
    }
}
