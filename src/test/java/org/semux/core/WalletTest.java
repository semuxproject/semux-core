/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.EdDSA;

public class WalletTest {

    private File file;
    private String pwd;
    private Wallet wallet;

    @Before
    public void setup() {
        try {
            file = File.createTempFile("wallet", ".data");
            pwd = "password";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        wallet = new Wallet(file);
        wallet.unlock(pwd);
        wallet.setAccounts(Collections.singletonList(new EdDSA()));
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

        EdDSA key1 = new EdDSA();
        EdDSA key2 = new EdDSA();
        wallet.addAccounts(Arrays.asList(key1, key2));

        assertThat(wallet.getAccounts(), contains(key1, key2));
    }

    @Test
    public void testFlush() throws InterruptedException {
        long sz = file.length();
        Thread.sleep(500);

        wallet.unlock(pwd);
        wallet.setAccounts(Collections.emptyList());
        assertEquals(sz, file.length());

        wallet.flush();
        assertTrue(file.length() < sz);
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
            EdDSA key = new EdDSA();
            wallet.addAccount(key);
        }
        wallet.flush();
        EdDSA key = new EdDSA();
        assertFalse(wallet.deleteAccount(key));
    }

    @Test
    public void testDeleteAccountExist() {
        wallet.unlock(pwd);
        for (int i = 0; i < 3; i++) {
            EdDSA k = new EdDSA();
            wallet.addAccount(k);
        }
        EdDSA key = new EdDSA();
        wallet.addAccount(key);
        for (int i = 0; i < 7; i++) {
            EdDSA k = new EdDSA();
            wallet.addAccount(k);
        }
        wallet.flush();
        assertTrue(wallet.deleteAccount(key));
    }

    @Test(expected = WalletLockedException.class)
    public void testDeleteWalletNotUnlock() {
        wallet.unlock(pwd);
        for (int i = 0; i < 4; i++) {
            EdDSA k = new EdDSA();
            wallet.addAccount(k);
        }
        EdDSA key = new EdDSA();
        wallet.addAccount(key);
        for (int i = 0; i < 6; i++) {
            EdDSA k = new EdDSA();
            wallet.addAccount(k);
        }
        wallet.flush();
        wallet.lock();
        wallet.deleteAccount(key);
    }

    @After
    public void teardown() {
        file.delete();
    }
}
