/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.util.SystemUtil;

import com.google.common.collect.Sets;

public class WalletTest {

    private File file;
    private String pwd;
    private Wallet wallet;

    @Before
    public void setUp() {
        try {
            file = File.createTempFile("wallet", ".data");
            Files.deleteIfExists(file.toPath()); // Wallet only needs the file's path
            pwd = "password";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        wallet = new Wallet(file);
        wallet.unlock(pwd);
        wallet.setAccounts(Collections.singletonList(new Key()));
        wallet.flush();
        wallet.lock();
    }

    @Test
    public void testGetters() {
        wallet.unlock(pwd);
        assertEquals(file, wallet.getFile());
        assertEquals(pwd, wallet.getPassword());
    }

    @Test
    public void testUnlock() {
        assertFalse(wallet.isUnlocked());

        wallet.unlock(pwd);
        assertTrue(wallet.isUnlocked());

        assertEquals(1, wallet.getAccounts().size());
    }

    @Test
    public void testLock() {
        wallet.unlock(pwd);

        wallet.lock();
        assertFalse(wallet.isUnlocked());
    }

    @Test
    public void testAddAccounts() {
        wallet.unlock(pwd);
        wallet.setAccounts(Collections.emptyList());

        Key key1 = new Key();
        Key key2 = new Key();
        wallet.addAccounts(Arrays.asList(key1, key2));

        assertThat(wallet.getAccounts(), contains(key1, key2));
    }

    @Test
    public void testFlush() throws InterruptedException, IOException {
        long sz = file.length();
        Thread.sleep(500);

        wallet.unlock(pwd);
        wallet.setAccounts(Collections.emptyList());
        assertEquals(sz, file.length());

        wallet.flush();
        assertTrue(file.length() < sz);

        if (SystemUtil.isPosix()) {
            assertEquals(Sets.newHashSet(OWNER_READ, OWNER_WRITE), Files.getPosixFilePermissions(file.toPath()));
        }
    }

    @Test
    public void testAddWallet() throws IOException {

        File f = File.createTempFile("wallet2", ".data");
        Key key = new Key();

        Wallet wallet1 = new Wallet(f);
        wallet1.unlock(pwd);
        wallet1.addAccount(key);
        wallet1.setAddressAlias(key.toAddress(), "originalName");

        try {
            wallet.addWallet(wallet1);
            fail("Wallet adding should require unlocking");
        } catch (WalletLockedException e) {
            // expected
        }

        wallet.unlock(pwd);
        wallet.addWallet(wallet1);

        assertTrue(wallet.getAccount(key.toAddress()) != null);
        assertEquals("originalName", wallet.getAddressAlias(key.toAddress()).get());

        // change the name in wallet
        wallet.setAddressAlias(key.toAddress(), "newName");

        Key key2 = new Key();
        wallet1.addAccount(key2);

        // import again and make sure name did not change, but new key was added
        int added = wallet.addWallet(wallet1);
        assertEquals(1, added);
        assertTrue(wallet.getAccount(key2.toAddress()) != null);
        assertEquals("newName", wallet.getAddressAlias(key.toAddress()).get());
    }

    @Test
    public void testFlush2() throws IOException {
        File f = File.createTempFile("wallet", ".data");
        Wallet w = new Wallet(f);

        w.unlock(pwd);
        Key key = new Key();
        w.addAccount(key);
        w.flush();

        Wallet w2 = new Wallet(f);
        w2.unlock(pwd);
        assertEquals(key.toAddressString(), w2.getAccount(0).toAddressString());
    }

    @Test
    public void testChangePassword() {
        String pwd2 = "passw0rd2";

        wallet.unlock(pwd);
        wallet.changePassword(pwd2);
        wallet.flush();
        wallet.lock();

        assertFalse(wallet.unlock(pwd));
        assertTrue(wallet.unlock(pwd2));
    }

    @Test
    public void testDeleteAccountNotExist() {
        wallet.unlock(pwd);
        // add n new accounts;
        for (int i = 0; i < 10; i++) {
            Key key = new Key();
            wallet.addAccount(key);
        }
        wallet.flush();
        Key key = new Key();
        assertFalse(wallet.removeAccount(key));
    }

    @Test
    public void testDeleteAccountExist() {
        wallet.unlock(pwd);
        for (int i = 0; i < 3; i++) {
            Key k = new Key();
            wallet.addAccount(k);
        }
        Key key = new Key();
        wallet.addAccount(key);
        for (int i = 0; i < 7; i++) {
            Key k = new Key();
            wallet.addAccount(k);
        }
        wallet.flush();
        assertTrue(wallet.removeAccount(key));
    }

    @Test(expected = WalletLockedException.class)
    public void testDeleteWalletNotUnlock() {
        wallet.unlock(pwd);
        for (int i = 0; i < 4; i++) {
            Key k = new Key();
            wallet.addAccount(k);
        }
        Key key = new Key();
        wallet.addAccount(key);
        for (int i = 0; i < 6; i++) {
            Key k = new Key();
            wallet.addAccount(k);
        }
        wallet.flush();
        wallet.lock();
        wallet.removeAccount(key);
    }

    @Test
    public void testWalletNames() throws IOException {
        File tmp = File.createTempFile("wallet", ".data");

        Wallet wall = new Wallet(tmp);
        wall.unlock(pwd);
        Key k = new Key();

        wall.addAccount(k);
        wall.setAddressAlias(k.toAddress(), "name");
        wall.flush();

        wall = new Wallet(tmp);
        wall.unlock(pwd);
        Optional<String> name = wall.getAddressAlias(k.toAddress());
        assertEquals("name", name.get());
    }

    @Test
    public void testImport() throws IOException {
        File tmp = File.createTempFile("wallet", ".data");
        try {
            String[] names = { "backup_from_linux", "backup_from_macos", "backup_from_windows", };
            String[] addresses = {
                    "83b9e57d516fa79df1e33e9056a77703937a6825",
                    "9f1add63c9ef9e9ca7ba60d6a34ab2372f7ce6e9",
                    "e8146fc7e810c25ab87331b5190d23ef424a6557"
            };
            for (int i = 0; i < names.length; i++) {
                FileUtils.copyInputStreamToFile(WalletTest.class.getResourceAsStream("/wallet/" + names[i]), tmp);
                Wallet w = new Wallet(tmp);
                w.unlock("password");
                assertEquals(addresses[i], Hex.encode(Hash.h160(w.getAccount(0).getPublicKey())));
            }
        } finally {
            tmp.delete();
        }
    }

    @After
    public void tearDown() {
        file.delete();
    }
}
