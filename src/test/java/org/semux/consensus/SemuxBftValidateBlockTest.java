/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.rules.KernelRule;
import org.semux.util.TimeUtil;

public class SemuxBftValidateBlockTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(15160, 15170);

    @Test
    public void testBlockInTheFuture() {
        kernelRule.getKernel().start();

        SemuxBft semuxBFT = mock(SemuxBft.class);
        semuxBFT.chain = kernelRule.getKernel().getBlockchain();
        semuxBFT.config = mock(Config.class);
        doCallRealMethod().when(semuxBFT).validateBlockProposal(any(), any());

        when(semuxBFT.config.bftMaxBlockTimeDrift()).thenReturn(TimeUnit.MINUTES.toMillis(15));

        BlockHeader blockHeader = mock(BlockHeader.class);
        when(blockHeader.getTimestamp())
                .thenReturn(TimeUtil.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        List<Transaction> transactions = new ArrayList<>();

        Block block = spy(new Block(blockHeader, transactions));
        doReturn(true).when(block).validateHeader(any(), any());
        assertEquals(false, semuxBFT.validateBlockProposal(blockHeader, transactions));
    }
}
