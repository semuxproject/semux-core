/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.Amount.ZERO;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.semux.Network;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Hash;
import org.semux.crypto.Key;

public class MerkleUtilTest {
    @Test
    public void testComputeTransactionsRoot() {
        Network network = Network.DEVNET;
        TransactionType type = TransactionType.TRANSFER;
        byte[] to = Bytes.random(20);
        Amount value = SEM.of(1);
        Amount fee = ZERO;
        long nonce = 1;
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = Bytes.random(128);
        Transaction tx1 = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(new Key());
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(new Key());
        byte[] b1 = tx1.getHash();
        byte[] b2 = tx2.getHash();
        byte[] root = new MerkleTree(Arrays.asList(b1, b2)).getRootHash();

        byte[] merkle = MerkleUtil.computeTransactionsRoot(Arrays.asList(tx1, tx2));

        assertThat(merkle, equalTo(root));
    }

    @Test
    public void testComputeResultsRoot() {
        TransactionResult res1 = new TransactionResult(true, Bytes.random(20), Collections.emptyList());
        TransactionResult res2 = new TransactionResult(false, Bytes.random(20), Collections.emptyList());
        byte[] b1 = Hash.h256(res1.toBytes());
        byte[] b2 = Hash.h256(res2.toBytes());
        byte[] root = new MerkleTree(Arrays.asList(b1, b2)).getRootHash();

        byte[] merkle = MerkleUtil.computeResultsRoot(Arrays.asList(res1, res2));

        assertThat(merkle, equalTo(root));
    }
}
