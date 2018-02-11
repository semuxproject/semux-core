/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Block.class, System.class })
@PowerMockRunnerDelegate(Parameterized.class)
@PowerMockIgnore({ "jdk.internal.*", "javax.management.*" })
public class SemuxBftValidateBlockTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {
                        "block in the future",
                        (Callable<?>) () -> {
                            mockStatic(Block.class);
                            when(Block.validateHeader(any(), any())).thenReturn(true);
                            return null;
                        },
                        (Supplier<Blockchain>) () -> {
                            Blockchain blockchain = mock(Blockchain.class);
                            when(blockchain.getLatestBlock()).thenReturn(mock(Block.class));
                            return blockchain;
                        },
                        (Supplier<Config>) () -> {
                            Config config = mock(Config.class);
                            when(config.maxBlockTimeDrift()).thenReturn(TimeUnit.MINUTES.toMillis(15));
                            return config;
                        },
                        (Supplier<BlockHeader>) () -> {
                            BlockHeader blockHeader = mock(BlockHeader.class);
                            when(blockHeader.getTimestamp())
                                    .thenReturn(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
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
        doCallRealMethod().when(semuxBFT).validateBlock(any(), any());

        this.blockHeader = blockHeader.get();
        this.transactions = transactions.get();
        this.result = result;
    }

    @Test
    public void testValidateBlock() {
        assertEquals(result, semuxBFT.validateBlock(blockHeader, transactions));
    }
}
