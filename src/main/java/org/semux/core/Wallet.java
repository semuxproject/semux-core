/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.util.FileUtil.POSIX_SECURED_PERMISSIONS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.crypto.generators.BCrypt;
import org.semux.core.exception.WalletLockedException;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.crypto.Aes;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hash;
import org.semux.crypto.Key;
import org.semux.crypto.bip32.CoinType;
import org.semux.crypto.bip32.HdKeyPair;
import org.semux.crypto.bip32.key.KeyVersion;
import org.semux.crypto.bip39.Language;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.semux.crypto.bip44.Bip44;
import org.semux.message.CliMessages;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.FileUtil;
import org.semux.util.IOUtil;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wallet {

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    private static final int VERSION = 4;
    private static final int SALT_LENGTH = 16;
    private static final int BCRYPT_COST = 12;
    private static final Bip44 BIP_44 = new Bip44();
    private static final int MAX_HD_WALLET_SCAN_AHEAD = 64;

    // the BIP-44 path prefix for semux addresses
    public static final String PATH_PREFIX = "m/44'/7562605'/0'/0'";
    public static final String MNEMONIC_PASS_PHRASE = "";
    public static final Language MNEMONIC_LANGUAGE = Language.ENGLISH;
    public static final int MNEMONIC_ENTROPY_LENGTH = 128;
    // always use mainnet to avoid confusion, since the generated key is stored
    public static final KeyVersion KEY_VERSION = KeyVersion.MAINNET;
    public static final CoinType COIN_TYPE = CoinType.SEMUX_SLIP10; // TODO: consider BIP32-ED25519

    private final File file;
    private final org.semux.Network network;

    private final Map<ByteArray, Key> accounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<ByteArray, String> aliases = new ConcurrentHashMap<>();

    private String password;

    // hd wallet key
    private String mnemonicPhrase = "";
    private int nextAccountIndex = 0;

    /**
     * Creates a new wallet instance.
     * 
     * @param file
     *            the wallet file
     */
    public Wallet(File file, org.semux.Network network) {
        this.file = file;
        this.network = network;
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
            byte[] key;
            byte[] salt;

            if (exists()) {
                SimpleDecoder dec = new SimpleDecoder(IOUtil.readFile(file));
                int version = dec.readInt(); // version

                Set<Key> newAccounts;
                Map<ByteArray, String> newAliases = new HashMap<>();

                switch (version) {
                case 1:
                    key = Hash.h256(Bytes.of(password));
                    newAccounts = readAccounts(key, dec, false, version);
                    break;
                case 2:
                    key = Hash.h256(Bytes.of(password));
                    newAccounts = readAccounts(key, dec, true, version);
                    newAliases = readAddressAliases(key, dec);
                    break;
                case 3:
                    salt = dec.readBytes();
                    key = BCrypt.generate(Bytes.of(password), salt, BCRYPT_COST);
                    newAccounts = readAccounts(key, dec, true, version);
                    newAliases = readAddressAliases(key, dec);
                    break;
                case 4:
                    salt = dec.readBytes();
                    key = BCrypt.generate(Bytes.of(password), salt, BCRYPT_COST);
                    newAccounts = readAccounts(key, dec, true, version);
                    newAliases = readAddressAliases(key, dec);
                    mnemonicPhrase = dec.readString();
                    nextAccountIndex = dec.readInt();
                    break;
                default:
                    throw new CryptoException("Unknown wallet version.");
                }

                synchronized (accounts) {
                    accounts.clear();
                    for (Key account : newAccounts) {
                        accounts.put(ByteArray.of(account.toAddress()), account);
                    }
                }
                synchronized (aliases) {
                    aliases.clear();
                    aliases.putAll(newAliases);
                }
            }
            this.password = password;
            return true;
        } catch (CryptoException e) {
            logger.debug(e.getMessage());
        } catch (InvalidKeySpecException e) {
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
    protected LinkedHashSet<Key> readAccounts(byte[] key, SimpleDecoder dec, boolean vlq, int version)
            throws InvalidKeySpecException {
        LinkedHashSet<Key> keys = new LinkedHashSet<>();
        int total = dec.readInt(); // size

        for (int i = 0; i < total; i++) {
            byte[] iv = dec.readBytes(vlq);
            if (version < 3) {
                dec.readBytes(vlq); // public key
            }
            byte[] privateKey = Aes.decrypt(dec.readBytes(vlq), key, iv);
            Key addressKey = new Key(privateKey);
            keys.add(new Key(privateKey, addressKey.getPublicKey()));
        }
        return keys;
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
            for (Key a : accounts.values()) {
                byte[] iv = Bytes.random(16);

                enc.writeBytes(iv);
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
            return new ArrayList<>(accounts.values());
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

        synchronized (accounts) {
            return getAccounts().get(idx);
        }
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

        synchronized (accounts) {
            return accounts.get(ByteArray.of(address));
        }
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

        synchronized (accounts) {
            ByteArray address = ByteArray.of(newKey.toAddress());
            if (accounts.containsKey(address)) {
                return false;
            }

            accounts.put(address, newKey);
            return true;
        }
    }

    /**
     * Add an account with randomly generated key.
     *
     * @return the generated key
     * @throws WalletLockedException
     */
    public Key addAccountRandom() throws WalletLockedException {
        Key key = new Key();
        addAccount(key);
        return key;
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
        return removeAccount(key.toAddress());
    }

    /**
     * Deletes an account in the wallet.
     *
     * NOTE: you need to call {@link #flush()} to update the wallet on disk.
     *
     * @param address
     *            address bytes of the account to delete
     * @return true if the account was successfully deleted, false otherwise
     * @throws WalletLockedException
     *
     */
    public boolean removeAccount(byte[] address) throws WalletLockedException {
        requireUnlocked();

        synchronized (accounts) {

            boolean removed = accounts.remove(ByteArray.of(address)) != null;
            if (removed && isHdWalletInitialized()) {
                // remove the alias for the account
                removeAddressAlias(address);
            }
            return removed;
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
            SimpleEncoder enc = new SimpleEncoder();
            enc.writeInt(VERSION);

            byte[] salt = Bytes.random(SALT_LENGTH);
            enc.writeBytes(salt);

            byte[] key = BCrypt.generate(Bytes.of(password), salt, BCRYPT_COST);

            writeAccounts(key, enc);
            writeAddressAliases(key, enc);

            enc.writeString(mnemonicPhrase);
            enc.writeInt(nextAccountIndex);

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                logger.error("Failed to create the directory for wallet");
                return false;
            }

            // set posix permissions
            if (SystemUtil.isPosix() && !file.exists()) {
                Files.createFile(file.toPath());
                Files.setPosixFilePermissions(file.toPath(), POSIX_SECURED_PERMISSIONS);
            }

            IOUtil.writeToFile(enc.toBytes(), file);
            return true;
        } catch (CryptoException e) {
            logger.error("Failed to encrypt the wallet");
        } catch (IOException e) {
            logger.error("Failed to write wallet to disk", e);
        }

        return false;
    }

    public boolean isPosixPermissionSecured() throws IOException {
        return FileUtil.isPosixPermissionSecured(getFile());
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

        return numImportedAddresses;
    }

    /**
     * Returns the targeted network.
     *
     * @return
     */
    public org.semux.Network getNetwork() {
        return network;
    }

    private void requireUnlocked() throws WalletLockedException {
        if (!isUnlocked()) {
            throw new WalletLockedException();
        }
    }

    // ================
    // HD wallet
    // ================

    /**
     * Returns whether the HD seed is initialized.
     *
     * @return true if set, otherwise false
     */
    public boolean isHdWalletInitialized() {
        requireUnlocked();
        return mnemonicPhrase != null && !mnemonicPhrase.isEmpty();
    }

    /**
     * Initialize the HD wallet.
     * 
     * @param mnemonicPhrase
     *            the mnemonic word list
     */
    public void initializeHdWallet(String mnemonicPhrase) {
        this.mnemonicPhrase = mnemonicPhrase;
        this.nextAccountIndex = 0;
    }

    /**
     * Returns the HD seed.
     * 
     * @return seed
     */
    public byte[] getSeed() {
        MnemonicGenerator generator = new MnemonicGenerator();
        return generator.getSeedFromWordlist(mnemonicPhrase, MNEMONIC_PASS_PHRASE, MNEMONIC_LANGUAGE);
    }

    /**
     * Derives a key based on the current HD account index, and put it into the
     * wallet.
     *
     * @return the derived key
     */
    public Key addAccountWithNextHdKey() {
        requireUnlocked();
        requireHdWalletInitialized();

        synchronized (accounts) {
            byte[] seed = getSeed();
            HdKeyPair rootKey = BIP_44.getRootKeyPairFromSeed(seed, KEY_VERSION, COIN_TYPE);
            HdKeyPair childKey = BIP_44.getChildKeyPair(rootKey, nextAccountIndex++);

            Key key = Key.fromRawPrivateKey(childKey.getPrivateKey().getKeyData());
            ByteArray address = ByteArray.of(key.toAddress());

            // put the accounts into
            accounts.put(address, key);

            // set a default alias
            if (!aliases.containsKey(address)) {
                setAddressAlias(address.getData(), getAliasFromPath(childKey.getPath()));
            }

            return key;
        }
    }

    /**
     * Scans for used HD keys from the 0-th index.
     *
     * Add any found used keys to this wallet.
     *
     * Increase the `nextAccountIndex` if a larger index is discovered.
     *
     * @return the number of accounts found
     */
    public int scanForHdKeys(AccountState accountState) {
        requireUnlocked();
        requireHdWalletInitialized();

        HdKeyPair rootAddress = BIP_44.getRootKeyPairFromSeed(getSeed(), KEY_VERSION, COIN_TYPE);

        int start = 0;
        int endIndex = start + MAX_HD_WALLET_SCAN_AHEAD;
        int found = 0;
        for (int i = start; i < endIndex; i++) {
            HdKeyPair childKey = BIP_44.getChildKeyPair(rootAddress, i);
            Key key = Key.fromRawPrivateKey(childKey.getPrivateKey().getKeyData());
            ByteArray address = ByteArray.of(key.toAddress());

            boolean isUsedAccount = isUsedAccount(accountState, key.toAddress());

            // if we find an account that has been used, we push forward our end search.
            // an account exists if its in our wallet, has balance, or has made transactions
            if (isUsedAccount || accounts.containsKey(address)) {
                endIndex += MAX_HD_WALLET_SCAN_AHEAD;
                if (addAccount(key)) {
                    if (!aliases.containsKey(address)) {
                        setAddressAlias(address.getData(), getAliasFromPath(childKey.getPath()));
                    }
                    found++;
                }
                if (i >= nextAccountIndex) {
                    nextAccountIndex = i + 1;
                }
            }
        }

        return found;
    }

    /**
     * the full BIP-44 derivation path is confusing to new users
     *
     * So, we adapt this name to be simpler, and mostly concentrate on the index of
     * the address.
     *
     * This method converts from a derivation path to a simplified form for default
     * wallet alias
     *
     * @param path
     * @return
     */
    private String getAliasFromPath(String path) {
        return path.replace(PATH_PREFIX, CliMessages.get("HdWalletAliasPrefix"));
    }

    private boolean isUsedAccount(AccountState accountState, byte[] bytes) {
        if (accountState == null) {
            return false;
        }
        Account account = accountState.getAccount(bytes);
        return account.getNonce() > 0 || account.getAvailable().gt0();
    }

    private void requireHdWalletInitialized() throws WalletLockedException {
        if (!isHdWalletInitialized()) {
            throw new IllegalArgumentException("HD Seed is not initialized");
        }
    }
}
