/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.semux.core.state.Account;
import org.semux.crypto.EdDSA;

public class WalletAccountTest {

    @Test
    public void testKey() {
        EdDSA key = new EdDSA();
        Account acc = new Account(key.toAddress(), 1, 2, 3);
        WalletAccount wa = new WalletAccount(key, acc);

        assertThat(wa.getAddress(), equalTo(key.toAddress()));
        assertThat(wa.getAvailable(), equalTo(1L));
        assertThat(wa.getLocked(), equalTo(2L));
        assertThat(wa.getNonce(), equalTo(3L));

        assertTrue(wa.getTransactions().isEmpty());

        wa.setTransactions(Collections.singletonList(null));
        assertFalse(wa.getTransactions().isEmpty());

        assertEquals(key, wa.getKey());

        EdDSA key2 = new EdDSA();
        wa.setKey(key2);
        assertEquals(key2, wa.getKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatch() {
        EdDSA key = new EdDSA();
        Account acc = new Account(new EdDSA().toAddress(), 1, 2, 3);
        new WalletAccount(key, acc);
    }

}
