/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.config.Constants;
import org.semux.config.MainNetConfig;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.EdDSA;
import org.semux.net.Channel;
import org.semux.net.Peer;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class SemuxBFTTest {

    private static final Logger logger = LoggerFactory.getLogger(SemuxBFTTest.class);

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDBRule temporaryDBRule = new TemporaryDBRule();

    @Test
    public void testIsPrimary() {
        List<String> validators = Arrays.asList("a", "b", "c", "d");
        int blocks = 1000;
        int repeat = 0;
        int last = -1;

        SemuxBFT bft = mock(SemuxBFT.class);
        bft.config = new MainNetConfig(Constants.DEFAULT_DATA_DIR);
        bft.validators = validators;
        when(bft.isPrimary(anyLong(), anyInt(), anyString())).thenCallRealMethod();

        Random r = new Random(System.nanoTime());
        for (int i = 0; i < blocks; i++) {
            int view = r.nextInt(2);
            for (int j = 0; j < validators.size(); j++) {
                if (bft.isPrimary(i, view, validators.get(j))) {
                    if (j == last) {
                        repeat++;
                    }
                    last = j;
                }
            }
        }
        logger.info("Consecutive validator probability: {}/{}", repeat, blocks);
        assertEquals(1.0 / validators.size(), (double) repeat / blocks, 0.05);
    }

    /**
     * https://github.com/bitcoin/bips/blob/master/bip-0030.mediawiki
     */
    @Test
    public void testDuplicatedTransaction() {
        // mock blockchain with a single transaction
        EdDSA to = new EdDSA();
        EdDSA from1 = new EdDSA();
        long time = System.currentTimeMillis();
        Transaction tx1 = new Transaction(
                kernelRule.getKernel().getConfig().networkId(),
                TransactionType.TRANSFER,
                to.toAddress(),
                10 * Unit.SEM,
                kernelRule.getKernel().getConfig().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from1);
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from1.toAddress(), 1000 * Unit.SEM);
        Block block1 = kernelRule.createBlock(Arrays.asList(tx1));
        kernelRule.getKernel().getBlockchain().addBlock(block1);
        SemuxBFT semuxBFT = new SemuxBFT(kernelRule.getKernel());

        // create a tx with the same hash with tx1 from a different signer in the second
        // block
        EdDSA from2 = new EdDSA();
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from2.toAddress(), 1000 * Unit.SEM);
        Transaction tx2 = new Transaction(
                kernelRule.getKernel().getConfig().networkId(),
                TransactionType.TRANSFER,
                to.toAddress(),
                10 * Unit.SEM,
                kernelRule.getKernel().getConfig().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from2);
        Block block2 = kernelRule.createBlock(Arrays.asList(tx2));

        // this test case is valid if and only if tx1 and tx2 have the same tx hash
        assert (Arrays.equals(tx1.getHash(), tx2.getHash()));

        // the block should be rejected because of the duplicated tx
        assertFalse(semuxBFT.validateBlock(block2.getHeader(), block2.getTransactions()));
    }

    @Test
    public void testOnLargeNewHeight() throws InterruptedException {
        kernelRule.getKernel().start();

        // mock consensus
        SemuxBFT semuxBFT = (SemuxBFT) kernelRule.getKernel().getConsensus();
        semuxBFT.activeValidators = Arrays.asList(
                mockValidator(10L),
                mockValidator(Long.MAX_VALUE),
                mockValidator(100L));

        // start semuxBFT
        new Thread(() -> semuxBFT.onNewHeight(Long.MAX_VALUE)).start();

        // 2/3th validator's height should be set as sync target
        await().until(() -> kernelRule.getKernel().getSyncManager().isRunning());
        assertEquals(100L, kernelRule.getKernel().getSyncManager().getProgress().getTargetHeight());
    }

    private Channel mockValidator(long latestBlockNumber) {
        Channel mockChannel = mock(Channel.class);
        Peer mockPeer = mock(Peer.class);
        when(mockPeer.getLatestBlockNumber()).thenReturn(latestBlockNumber);
        when(mockChannel.getRemotePeer()).thenReturn(mockPeer);
        return mockChannel;
    }
}
