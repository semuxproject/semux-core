package org.semux.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.crypto.EdDSA;

public class WalletTest {

    private static File file = new File("wallet_test.data");
    private static String pwd = "passw0rd";

    private Wallet wallet;

    @Before
    public void setup() {
        wallet = new Wallet(file);
        wallet.unlock(pwd);
        wallet.setAccounts(Collections.singletonList(new EdDSA()));
        wallet.flush();
        wallet.lock();
    }

    @Test
    public void testUnlock() {
        assertFalse(wallet.unlocked());

        wallet.unlock(pwd);
        assertTrue(wallet.unlocked());

        assertEquals(1, wallet.getAccounts().size());
    }

    @Test
    public void testLock() {
        wallet.unlock(pwd);

        wallet.lock();
        assertFalse(wallet.unlocked());
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

    @After
    public void teardown() {
        file.delete();
    }
}
