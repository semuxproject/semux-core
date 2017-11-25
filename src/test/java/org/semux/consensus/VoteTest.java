/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.util.Bytes;

public class VoteTest {

    @Test
    public void testVote() {
        long height = Long.MAX_VALUE;
        int view = Integer.MAX_VALUE;
        Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, Hash.EMPTY_H256);

        assertFalse(vote.validate());
        vote.sign(new EdDSA());
        assertTrue(vote.validate());

        Vote vote2 = Vote.fromBytes(vote.toBytes());

        assertEquals(VoteType.COMMIT, vote2.getType());
        assertEquals(height, vote2.getHeight());
        assertEquals(view, vote2.getView());
        assertArrayEquals(Hash.EMPTY_H256, vote2.getBlockHash());
    }

    @Test
    public void testValidate() {
        VoteType type = VoteType.COMMIT;

        long height = 1;
        int view = 0;
        byte[] blockHash = Bytes.EMPTY_HASH;

        Vote v = new Vote(type, false, height, view, blockHash);
        assertFalse(v.validate());
        v.sign(new EdDSA());
        assertTrue(v.validate());
    }
}
