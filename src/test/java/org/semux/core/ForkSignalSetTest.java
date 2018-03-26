/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.semux.consensus.ValidatorActivatedFork;
import org.semux.crypto.Hex;

public class ForkSignalSetTest {

    private static final ValidatorActivatedFork[] eightPendingForks = new ValidatorActivatedFork[8];

    private static final byte[] eightPendingForksEncoded = Hex.decode("0800010002000300040005000600070008");

    private static final ValidatorActivatedFork[] onePendingFork = new ValidatorActivatedFork[1];

    private static final byte[] onePendingForkEncoded = Hex.decode("010001");

    @BeforeClass
    public static void beforeClass() {
        for (short i = 1; i <= 8; i++) {
            ValidatorActivatedFork a = mock(ValidatorActivatedFork.class);
            Whitebox.setInternalState(a, "number", i);
            eightPendingForks[i - 1] = a;
        }

        onePendingFork[0] = ValidatorActivatedFork.UNIFORM_DISTRIBUTION;
    }

    @Test
    public void testForkSignalSetCodec_onePendingFork() {
        // test decoding
        BlockHeaderData.ForkSignalSet set = new BlockHeaderData.ForkSignalSet(onePendingForkEncoded);
        assertTrue(set.signalingFork(onePendingFork[0]));

        // test encoding
        assertArrayEquals(onePendingForkEncoded, new BlockHeaderData.ForkSignalSet(onePendingFork).toBytes());
    }

    @Test
    public void testForkSignalSetCodec_eightPendingForks() {
        // test decoding
        BlockHeaderData.ForkSignalSet set = new BlockHeaderData.ForkSignalSet(eightPendingForksEncoded);
        for (ValidatorActivatedFork f : eightPendingForks) {
            set.signalingFork(f);
        }

        // test encoding
        set = new BlockHeaderData.ForkSignalSet(eightPendingForks);
        assertThat(set.toBytes()).hasSize(BlockHeaderData.ForkSignalSet.MAX_SIZE).isEqualTo(eightPendingForksEncoded);
    }
}
