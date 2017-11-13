/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.semux.util.Bytes;

public class AESTest {
    private static byte[] raw = Bytes.of("test");
    private static byte[] key = Hex.decode("1122334455667788112233445566778811223344556677881122334455667788");
    private static byte[] iv = Hex.decode("11223344556677881122334455667788");
    private static byte[] encrypted = Hex.decode("182b93aa58d6291381660e5bad673dd4");

    @Test
    public void testEncrypt() throws CryptoException {
        byte[] bytes = AES.encrypt(raw, key, iv);

        assertTrue(Arrays.equals(encrypted, bytes));
    }

    @Test
    public void testDecrypt() throws CryptoException {
        byte[] bytes = AES.decrypt(encrypted, key, iv);

        assertTrue(Arrays.equals(raw, bytes));
    }
}
