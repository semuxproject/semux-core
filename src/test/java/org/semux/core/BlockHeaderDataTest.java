/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.semux.consensus.ValidatorActivatedFork;
import org.semux.crypto.Hex;

public class BlockHeaderDataTest {

    private static final ValidatorActivatedFork[] eightPendingForks = new ValidatorActivatedFork[8];

    private static final byte[] v1_8xPendingForksEncoded = Hex.decode("01110800010002000300040005000600070008");

    private static final byte[] v1_0xPendingForkEncoded = Hex.decode("010100");

    private static final ValidatorActivatedFork[] onePendingFork = new ValidatorActivatedFork[1];

    private static final byte[] v1_1xPendingForkEncoded = Hex.decode("0103010001");

    private static final byte[] unrecognized_reservedDataEncoded = Hex
            .decode("f1a253afc2ae97cd1a562d8a829f26fa876bb48e264fdfb1d18df3c84271");
    private static final byte[] unrecognized_headerDataEncoded = Hex
            .decode("ff1ef1a253afc2ae97cd1a562d8a829f26fa876bb48e264fdfb1d18df3c84271");

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
    public void testV0HeaderData() {
        BlockHeaderData blockHeaderData = BlockHeaderData.v0();
        assertThat(blockHeaderData.toBytes()).hasSize(0);
        assertFalse(blockHeaderData.signalingFork(ValidatorActivatedFork.UNIFORM_DISTRIBUTION));
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x00);
        assertThat(BlockHeaderData.fromBytes(null).version).isEqualTo((byte) 0x00);
        assertThat(BlockHeaderData.fromBytes(new byte[0]).version).isEqualTo((byte) 0x00);
    }

    @Test
    public void testV1HeaderDataEncoding() {
        // one pending fork
        BlockHeaderData blockHeaderData = BlockHeaderData.v1(new BlockHeaderData.ForkSignalSet(onePendingFork));
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_1xPendingForkEncoded).hasSize(5); // writeByte(1) +
                                                                                             // writeSize(1) +
                                                                                             // writeByte(1) +
                                                                                             // writeShort(2)

        // zero pending fork
        blockHeaderData = BlockHeaderData.v1(new BlockHeaderData.ForkSignalSet());
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_0xPendingForkEncoded).hasSize(3); // writeByte(1) +
                                                                                             // writeSize(1) +
                                                                                             // writeByte(1)

        // eight pending forks
        blockHeaderData = BlockHeaderData.v1(new BlockHeaderData.ForkSignalSet(eightPendingForks));
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        assertThat(blockHeaderData.toBytes()).isEqualTo(v1_8xPendingForksEncoded).hasSize(19); // writeByte(1) +
                                                                                               // writeSize(1) +
                                                                                               // writeByte(1) +
                                                                                               // writeShort(2) * 8
    }

    @Test
    public void testV1HeaderDataDecoding() {
        // one pending fork
        BlockHeaderData blockHeaderData = BlockHeaderData.fromBytes(v1_1xPendingForkEncoded);
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        assertTrue(blockHeaderData.signalingFork(onePendingFork[0]));

        // zero pending fork
        blockHeaderData = BlockHeaderData.fromBytes(v1_0xPendingForkEncoded);
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        assertFalse(blockHeaderData.signalingFork(onePendingFork[0]));

        // eight pending forks
        blockHeaderData = BlockHeaderData.fromBytes(v1_8xPendingForksEncoded);
        assertThat(blockHeaderData.version).isEqualTo((byte) 0x01);
        for (ValidatorActivatedFork f : eightPendingForks) {
            assertTrue(blockHeaderData.signalingFork(f));
        }
    }

    @Test
    public void testUnrecognizedHeaderDataEncoding() {
        BlockHeaderData blockHeaderData = mock(BlockHeaderData.class, withSettings()
                .useConstructor((byte) 0xff, unrecognized_reservedDataEncoded));
        when(blockHeaderData.toBytes()).thenCallRealMethod();
        assertThat(blockHeaderData.toBytes()).isEqualTo(unrecognized_headerDataEncoded).hasSize(32);
        assertFalse(blockHeaderData.signalingFork(ValidatorActivatedFork.UNIFORM_DISTRIBUTION));
    }

    @Test
    public void testUnrecognizedHeaderDataDecoding() {
        BlockHeaderData blockHeaderData = BlockHeaderData.fromBytes(unrecognized_headerDataEncoded);
        assertThat(blockHeaderData.version).isEqualTo((byte) 0xff);
        assertThat((byte[]) Whitebox.getInternalState(blockHeaderData, "reserved"))
                .isEqualTo(unrecognized_reservedDataEncoded);
    }
}
