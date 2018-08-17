/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bouncycastle.jcajce.provider.digest.Keccak;

public class VMUtil {

    /**
     * Computes the Keccak-256 hash digest.
     *
     * @param input
     *            the input data
     * @return a 32 bytes digest
     */
    public static byte[] keccak256(byte[] input) {
        Keccak.Digest256 digest = new Keccak.Digest256();
        digest.update(input);
        return digest.digest();
    }

    /**
     * Computes the address of a deployed contract.
     *
     * @param address
     *            the sender's address
     * @param nonce
     *            the sender's nonce
     * @return an 20 bytes array
     * @implNote the implementation is slightly different from Ethereum's specs;
     *           we're not using RLP codec to encode the address and nonce.
     */
    public static byte[] calcNewAddress(byte[] address, long nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(20 + 8);
        buffer.put(address).putLong(nonce);
        byte[] keccak256 = keccak256(buffer.array());

        return Arrays.copyOfRange(keccak256, 12, 32);
    }
}
