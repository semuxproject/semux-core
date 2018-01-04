/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semux.core.Blockchain;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.Peer;

@RunWith(Parameterized.class)
public class SemuxBFTOnNewHeightTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // 0 validator
                { Arrays.asList(), null },
                // 1 validator
                { Arrays.asList(
                        mockValidator(10L)), null },
                // 2 validators
                { Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L)), 11L },
                // 3 validators
                { Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L)), 101L },
                // 4 validators
                { Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L),
                        mockValidator(10000L)), 101L },
                // 5 validators
                { Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L),
                        mockValidator(10000L),
                        mockValidator(100000L)), 1001L },
                // Malicious validator with large height
                { Arrays.asList(
                        mockValidator(10L),
                        mockValidator(Long.MAX_VALUE),
                        mockValidator(100L)), 101L },
                // 100 validators with height from 1 ~ 100
                { LongStream.range(1L, 100L).mapToObj(SemuxBFTOnNewHeightTest::mockValidator)
                        .collect(Collectors.toList()), 67L }
        });
    }

    private List<Channel> validators;

    private Long target;

    public SemuxBFTOnNewHeightTest(List<Channel> validators, Long target) {
        this.validators = validators;
        this.target = target;
    }

    @Test
    public void testOnLargeNewHeight() {
        // mock consensus
        SemuxBFT semuxBFT = mock(SemuxBFT.class);

        semuxBFT.chain = mock(Blockchain.class);
        semuxBFT.channelMgr = mock(ChannelManager.class);

        when(semuxBFT.channelMgr.getActiveChannels(any())).thenReturn(validators);

        doCallRealMethod().when(semuxBFT).onNewHeight(anyLong());

        // start semuxBFT
        // the height property from BFT_NEW_HEIGHT message should not imply the actual
        // sync target
        semuxBFT.onNewHeight(Long.MAX_VALUE);

        if (target != null) {
            verify(semuxBFT).sync(target);
        } else {
            verify(semuxBFT, never()).sync(anyLong());
        }
    }

    private static Channel mockValidator(long latestBlockNumber) {
        Channel mockChannel = mock(Channel.class);
        Peer mockPeer = mock(Peer.class);
        when(mockPeer.getLatestBlockNumber()).thenReturn(latestBlockNumber);
        when(mockChannel.getRemotePeer()).thenReturn(mockPeer);
        return mockChannel;
    }
}
