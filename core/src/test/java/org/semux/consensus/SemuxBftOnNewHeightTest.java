/**
 * Copyright (c) 2017-2018 The Semux Developers
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
public class SemuxBftOnNewHeightTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {

        // [ newHeight, height, activeValidators, target ]
        return Arrays.asList(new Object[][] {
                // 0 validator
                { 10L, 1L, Collections.emptyList(), null },
                // 1 validator
                { 10L, 1L, Collections.singletonList(mockValidator(0L)), null },
                // 1 validator
                { 10L, 1L, Collections.singletonList(mockValidator(10L)), 11L },
                // 2 validators
                { 100L, 1L, Arrays.asList(
                        mockValidator(100L),
                        mockValidator(100L)), 101L },
                { 100L, 99L, Arrays.asList(
                        mockValidator(100L),
                        mockValidator(100L)), 101L },
                // 2 validators, same height
                { 100L, 100L, Arrays.asList(
                        mockValidator(100L),
                        mockValidator(100L)), null },
                // 2 validators, greater height
                { 100L, 101L, Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L)), null },
                // 3 validators
                { 1000L, 1L, Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L)), 101L },
                // 4 validators
                { 10000L, 1L, Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L),
                        mockValidator(10000L)), 101L },
                // 5 validators
                { 1000000L, 1L, Arrays.asList(
                        mockValidator(10L),
                        mockValidator(100L),
                        mockValidator(1000L),
                        mockValidator(10000L),
                        mockValidator(100000L)), 101L },
                // Malicious validator with large height
                { Long.MAX_VALUE, 1L, Arrays.asList(
                        mockValidator(10L),
                        mockValidator(Long.MAX_VALUE - 1),
                        mockValidator(100L)), 101L },
                // 100 validators with height from 1 ~ 100
                { 100L, 1L, LongStream.range(1L, 100L).mapToObj(SemuxBftOnNewHeightTest::mockValidator)
                        .collect(Collectors.toList()), 35L }
        });
    }

    private Long newHeight;

    private Long height;

    private List<Channel> activeValidators;

    private Long target;

    public SemuxBftOnNewHeightTest(long newHeight, long height, List<Channel> activeValidators, Long target) {
        this.newHeight = newHeight;
        this.height = height;
        this.activeValidators = activeValidators;
        this.target = target;
    }

    @Test
    public void testOnLargeNewHeight() {
        // mock consensus
        SemuxBft semuxBFT = mock(SemuxBft.class);

        semuxBFT.chain = mock(Blockchain.class);
        semuxBFT.height = height;
        semuxBFT.validators = new ArrayList<>();

        semuxBFT.channelMgr = mock(ChannelManager.class);
        when(semuxBFT.channelMgr.getActiveChannels(any())).thenReturn(activeValidators);

        doCallRealMethod().when(semuxBFT).onNewHeight(anyLong());

        // start semuxBFT
        semuxBFT.onNewHeight(newHeight);

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
