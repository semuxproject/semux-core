/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.util.TimeUtil;

@RunWith(Parameterized.class)
public class SemuxBftValidateBlockTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {
                        "block in the future",
                        (Callable<?>) () -> null,
                        (Supplier<Blockchain>) () -> {
                            Blockchain blockchain = mock(Blockchain.class);
                            when(blockchain.getLatestBlock()).thenReturn(mock(Block.class));
                            return blockchain;
                        },
                        (Supplier<Config>) () -> {
                            Config config = mock(Config.class);
                            when(config.bftMaxBlockTimeDrift()).thenReturn(TimeUnit.MINUTES.toMillis(15));
                            return config;
                        },
                        (Supplier<BlockHeader>) () -> {
                            BlockHeader blockHeader = mock(BlockHeader.class);
                            when(blockHeader.getTimestamp())
                                    .thenReturn(TimeUtil.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                            return blockHeader;
                        },
                        (Supplier<List<Transaction>>) ArrayList::new,
                        false
                },
        });
    }

    SemuxBft semuxBFT;

    private BlockHeader blockHeader;

    private List<Transaction> transactions;

    private boolean result;

    public SemuxBftValidateBlockTest(
            String name,
            Callable<Void> setUp,
            Supplier<Blockchain> chain,
            Supplier<Config> config,
            Supplier<BlockHeader> blockHeader,
            Supplier<List<Transaction>> transactions,
            boolean result) throws Exception {
        setUp.call();

        semuxBFT = mock(SemuxBft.class);
        semuxBFT.chain = chain.get();
        semuxBFT.config = config.get();
        doCallRealMethod().when(semuxBFT).validateBlockProposal(any(), any());

        this.blockHeader = blockHeader.get();
        this.transactions = transactions.get();
        this.result = result;
    }

    @Test
    public void testValidateBlock() {
        Block block = spy(new Block(blockHeader, transactions));
        doReturn(true).when(block).validateHeader(any(), any());
        assertEquals(result, semuxBFT.validateBlockProposal(blockHeader, transactions));
    }
}
