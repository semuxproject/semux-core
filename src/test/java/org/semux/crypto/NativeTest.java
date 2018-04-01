/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.util.Bytes;

public class NativeTest {

    private byte[] MESSAGE = Bytes.of("test");
    private byte[] BLAKE2B_HASH = Hex.decode("928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202");
    private byte[] PUBLIC_KEY = Hex.decode("04dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private byte[] PRIVATE_KEY = Hex
            .decode("a73994d748b936ce63ae254508d909381fdbec3bbc11f4401630414f21fecb1704dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private byte[] ED25519_SIGNATURE = Hex
            .decode("cc52ae5b72af210073756f801cf8ffa36cefe96c5010b2cf25d04dfc5b0495e4ee3e14774c4607a4475f2b449a3181c9bd2c6aed46ed283debfebe19589f550e");

    @Test
    public void testBlake2bNull() {
        assertNull(Native.blake2b(null));
    }

    @Test
    public void testBlake2b() {
        assertArrayEquals(BLAKE2B_HASH, Native.blake2b(MESSAGE));
    }

    @Test
    public void testEd25519SignNull() {
        assertNull(Native.ed25519_sign(null, null));
    }

    @Test
    public void testEd25519Sign() {
        assertArrayEquals(ED25519_SIGNATURE, Native.ed25519_sign(MESSAGE, PRIVATE_KEY));
    }

    @Test
    public void testEd25519VerifyNull() {
        assertFalse(Native.ed25519_verify(null, null, null));
    }

    @Test
    public void testEd25519Verify() {
        assertTrue(Native.ed25519_verify(MESSAGE, ED25519_SIGNATURE, PUBLIC_KEY));
    }
}
