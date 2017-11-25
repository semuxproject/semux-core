/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.util.Bytes;

public class CryptoExceptionTest {

    @Test(expected = CryptoException.class)
    public void testCryptoException() throws CryptoException {
        AES.decrypt(Bytes.EMPTY_BYTES, Bytes.EMPTY_BYTES, Bytes.EMPTY_BYTES);
    }

    @Test
    public void testConstructor() {
        String msg = "test";
        Throwable th = new Throwable();
        CryptoException e = new CryptoException(msg, th);
        assertEquals(msg, e.getMessage());
        assertEquals(th, e.getCause());
    }
}
