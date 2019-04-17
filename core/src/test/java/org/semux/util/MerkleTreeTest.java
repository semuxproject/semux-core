/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;

public class MerkleTreeTest {

    private byte[] hash1 = Hex.decode0x("0x1111111111111111111111111111111111111111111111111111111111111111");
    private byte[] hash2 = Hex.decode0x("0x2222222222222222222222222222222222222222222222222222222222222222");
    private byte[] hash3 = Hex.decode0x("0x3333333333333333333333333333333333333333333333333333333333333333");

    @Test
    public void testEmpty() {
        MerkleTree tree = new MerkleTree(Collections.emptyList());
        assertEquals(0, tree.size());
        assertArrayEquals(new byte[32], tree.getRootHash());

        List<byte[]> proof = tree.getProof(0);
        assertEquals(1, proof.size());
        assertArrayEquals(new byte[32], proof.get(0));
    }

    @Test
    public void testOneElement() {
        MerkleTree tree = new MerkleTree(Collections.singletonList(hash1));
        assertEquals(1, tree.size());
        assertArrayEquals(hash1, tree.getRootHash());

        List<byte[]> proof = tree.getProof(0);
        assertEquals(1, proof.size());
        assertArrayEquals(hash1, proof.get(0));
    }

    @Test
    public void testTwoElements() {
        byte[] hash12 = Hash.h256(hash1, hash2);

        MerkleTree tree = new MerkleTree(Arrays.asList(hash1, hash2));
        assertEquals(2, tree.size());
        assertArrayEquals(hash12, tree.getRootHash());

        List<byte[]> proof = tree.getProof(1);
        assertEquals(2, proof.size());
        assertArrayEquals(hash12, proof.get(0));
        assertArrayEquals(hash2, proof.get(1));
    }

    @Test
    public void testThreeElements() {
        byte[] hash12 = Hash.h256(hash1, hash2);
        byte[] hash33 = hash3;
        byte[] hash1233 = Hash.h256(hash12, hash33);

        MerkleTree tree = new MerkleTree(Arrays.asList(hash1, hash2, hash3));
        assertEquals(3, tree.size());
        assertArrayEquals(hash1233, tree.getRootHash());

        List<byte[]> proof = tree.getProof(2);
        assertEquals(3, proof.size());
        assertArrayEquals(hash1233, proof.get(0));
        assertArrayEquals(hash33, proof.get(1));
        assertArrayEquals(hash3, proof.get(2));
    }
}
