/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VMUtilTest {

    @Test
    public void testKeccak256() {
        byte[] msg = "testing".getBytes();
        byte[] keccak256 = VMUtil.keccak256(msg);

        String expected = "5f16f4c7f149ac4f9510d9cf8cf384038ad348b3bcdc01915f95de12df9d1b02";
        assertEquals(expected, HexUtil.toHexString(keccak256));
    }

    @Test
    public void testCalcNewAddress() {
        byte[] address = new byte[20];
        long nonce = 0;
        assertEquals(20, VMUtil.calcNewAddress(address, nonce).length);
    }
}
