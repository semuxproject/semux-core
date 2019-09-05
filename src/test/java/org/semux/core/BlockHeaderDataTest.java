/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.crypto.Hex;

public class BlockHeaderDataTest {

    private static final Fork[] onePendingFork = new Fork[1];
    private static final Fork[] eightPendingForks = new Fork[8];

    private static final byte[] v1_0xPendingForkEncoded = Hex.decode("010100");

    private static final byte[] v1_1xPendingForkEncoded = Hex.decode("0103010001");

    private static final byte[] v1_8xPendingForksEncoded = Hex.decode("01110800010002000300040005000600070008");

    @BeforeClass
    public static void beforeClass() {
        onePendingFork[0] = Fork.UNIFORM_DISTRIBUTION;

        for (short i = 1; i <= 8; i++) {
            Fork a = mock(Fork.class);
            when(a.id()).thenReturn(i);
            eightPendingForks[i - 1] = a;
        }
    }

    @Test
    public void testV0HeaderData() {
        BlockHeaderData blockHeaderData = new BlockHeaderData();
        assertThat(blockHeaderData.toBytes()).hasSize(0);
        assertFalse(blockHeaderData.parseForkSignals().contains(Fork.UNIFORM_DISTRIBUTION));
        assertFalse(blockHeaderData.parseForkSignals().contains(Fork.VIRTUAL_MACHINE));
    }

    @Test
    public void testV1HeaderDataEncoding() {
        // zero pending fork
        BlockHeaderData blockHeaderData = new BlockHeaderData(ForkSignalSet.of());
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_0xPendingForkEncoded).hasSize(3);
        // writeByte(1) +
        // writeSize(1) +
        // writeByte(1)

        // one pending fork
        blockHeaderData = new BlockHeaderData(ForkSignalSet.of(onePendingFork));
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_1xPendingForkEncoded).hasSize(5);
        // writeByte(1) +
        // writeSize(1) +
        // writeByte(1) +
        // writeShort(2)

        // eight pending forks
        blockHeaderData = new BlockHeaderData(ForkSignalSet.of(eightPendingForks));
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_8xPendingForksEncoded).hasSize(19);
        // writeByte(1) +
        // writeSize(1) +
        // writeByte(1) +
        // writeShort(2) * 8
    }
}
