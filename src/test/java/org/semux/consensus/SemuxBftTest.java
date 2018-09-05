/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.semux.consensus.ValidatorActivatedFork.UNIFORM_DISTRIBUTION;
import static org.semux.core.Amount.Unit.SEM;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.TestUtils;
import org.semux.config.Constants;
import org.semux.config.MainnetConfig;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Key;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class SemuxBftTest {

    private static final Logger logger = LoggerFactory.getLogger(SemuxBftTest.class);

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDatabaseRule temporaryDBRule = new TemporaryDatabaseRule();

    @Test
    public void testIsPrimaryH256() {
        List<String> validators = IntStream.range(1, 100).boxed().map(i -> String.format("v%d", i))
                .collect(Collectors.toList());

        SemuxBft bft = mock(SemuxBft.class);
        bft.config = new MainnetConfig(Constants.DEFAULT_DATA_DIR);
        bft.validators = validators;
        bft.chain = mock(Blockchain.class);
        when(bft.chain.forkActivated(anyLong(), eq(UNIFORM_DISTRIBUTION))).thenReturn(false);
        when(bft.isPrimary(anyLong(), anyInt(), anyString())).thenCallRealMethod();

        testIsPrimaryConsecutiveValidatorProbability(bft);
    }

    @Test
    public void testIsPrimaryUniformDist() {
        List<String> validators = IntStream.range(1, 100).boxed().map(i -> String.format("v%d", i))
                .collect(Collectors.toList());

        SemuxBft bft = mock(SemuxBft.class);
        bft.config = new MainnetConfig(Constants.DEFAULT_DATA_DIR);
        bft.validators = validators;
        bft.chain = mock(Blockchain.class);
        when(bft.chain.forkActivated(anyLong(), eq(UNIFORM_DISTRIBUTION))).thenReturn(true);
        when(bft.isPrimary(anyLong(), anyInt(), anyString())).thenCallRealMethod();

        testIsPrimaryConsecutiveValidatorProbability(bft);
    }

    private void testIsPrimaryConsecutiveValidatorProbability(SemuxBft bft) {
        int blocks = 1000;
        int repeat = 0;
        int last = -1;

        Random r = new Random(System.nanoTime());
        for (int i = 0; i < blocks; i++) {
            int view = r.nextDouble() < 0.05 ? 1 : 0;
            for (int j = 0; j < bft.validators.size(); j++) {
                if (bft.isPrimary(i, view, bft.validators.get(j))) {
                    if (j == last) {
                        repeat++;
                    }
                    last = j;
                }
            }
        }

        logger.info("Consecutive validator probability: {}/{}", repeat, blocks);
        assertEquals(1.0 / bft.validators.size(), (double) repeat / blocks, 0.05);
    }

    /**
     * https://github.com/bitcoin/bips/blob/master/bip-0030.mediawiki
     */
    @Test
    public void testDuplicatedTransaction() {
        // mock blockchain with a single transaction
        Key to = new Key();
        Key from1 = new Key();
        long time = TimeUtil.currentTimeMillis();
        Transaction tx1 = createTransaction(to, from1, time, 0);
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from1.toAddress(), SEM.of(1000));
        Block block1 = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                from1,
                kernelRule.getKernel().getBlockchain().getLatestBlock().getNumber() + 1,
                Collections.singletonList(tx1),
                Collections.singletonList(new TransactionResult(true)));
        kernelRule.getKernel().getBlockchain().addBlock(block1);
        SemuxBft semuxBFT = new SemuxBft(kernelRule.getKernel());

        // create a tx with the same hash with tx1 from a different signer in the second
        // block
        Key from2 = new Key();
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from2.toAddress(), SEM.of(1000));
        Transaction tx2 = createTransaction(to, from2, time, 0);
        Block block2 = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                from2,
                kernelRule.getKernel().getBlockchain().getLatestBlock().getNumber() + 1,
                Collections.singletonList(tx2),
                Collections.singletonList(new TransactionResult(true)));

        // this test case is valid if and only if tx1 and tx2 have the same tx hash
        assertTrue(Arrays.equals(tx1.getHash(), tx2.getHash()));

        // the block should be rejected because of the duplicated tx
        semuxBFT.proposal = new Proposal(new Proof(block2.getNumber(), 0), block2.getHeader(), Collections.emptyList());
        semuxBFT.proposal.sign(from2);
        assertFalse(semuxBFT.validateBlock(block2.getHeader(), block2.getTransactions()));
    }

    @Test
    public void testFilterPendingTransactions() {
        Key to = new Key();
        Key from = new Key();
        long time = TimeUtil.currentTimeMillis();
        Transaction tx1 = createTransaction(to, from, time, 0);
        Transaction tx2 = createTransaction(to, from, time, 1);

        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));

        // pending manager has only tx1 (validated)
        PendingManager.PendingTransaction pending = new PendingManager.PendingTransaction(tx1,
                new TransactionResult(true));
        when(kernelRule.getKernel().getPendingManager().getPendingTransactions())
                .thenReturn(Collections.singletonList(pending));

        SemuxBft semuxBFT = new SemuxBft(kernelRule.getKernel());

        // tx1 should filter out
        assertTrue(semuxBFT.getUnvalidatedTransactions(Collections.singletonList(tx1)).isEmpty());

        // other transactions should remain
        assertFalse(semuxBFT.getUnvalidatedTransactions(Collections.singletonList(tx2)).isEmpty());

        // test that invalid pending are not filtered
        when(kernelRule.getKernel().getPendingManager().getPendingTransactions(anyInt()))
                .thenReturn(Collections.singletonList(new PendingManager.PendingTransaction(tx2,
                        new TransactionResult(false))));

        assertFalse(semuxBFT.getUnvalidatedTransactions(Collections.singletonList(tx2)).isEmpty());
    }

    @Test
    public void testValidateBlockCoinbaseMagic() {
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));

        Block block = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                Constants.COINBASE_KEY,
                1,
                Collections.emptyList(),
                Collections.emptyList());

        SemuxBft semuxBFT = new SemuxBft(kernelRule.getKernel());
        assertFalse(semuxBFT.validateBlock(block.getHeader(), Collections.emptyList()));
    }

    @Test
    public void testValidateBlockCoinbaseProposer() {
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));

        Block block = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                new Key(),
                1,
                Collections.emptyList(),
                Collections.emptyList());

        SemuxBft semuxBFT = new SemuxBft(kernelRule.getKernel());
        semuxBFT.proposal = new Proposal(new Proof(block.getNumber(), 0), block.getHeader(), Collections.emptyList());
        semuxBFT.proposal.sign(new Key());
        assertFalse(semuxBFT.validateBlock(block.getHeader(), Collections.emptyList()));
    }

    @Test
    public void testValidateTransactionRecipient() {
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));

        Key blockForger = new Key();
        Transaction tx = TestUtils.createTransaction(kernelRule.getKernel().getConfig(), new Key(),
                Constants.COINBASE_KEY, Amount.ZERO);
        Block block = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                blockForger,
                1,
                Collections.singletonList(tx),
                Collections.singletonList(new TransactionResult(true)));

        SemuxBft semuxBFT = new SemuxBft(kernelRule.getKernel());
        semuxBFT.proposal = new Proposal(new Proof(block.getNumber(), 0), block.getHeader(),
                Collections.singletonList(tx));
        semuxBFT.proposal.sign(blockForger);
        assertFalse(semuxBFT.validateBlock(block.getHeader(), Collections.singletonList(tx)));
    }

    @Test
    public void testProposeBlock() {
        Blockchain chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().setBlockchain(chain);

        long timestamp = TimeUtil.currentTimeMillis() + 10000;
        long number = chain.getLatestBlockNumber() + 1;
        Block previousBlock = TestUtils.createBlock(
                timestamp,
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                new Key(),
                number,
                Collections.emptyList(),
                Collections.emptyList());
        chain.addBlock(previousBlock);

        SemuxBft bft = new SemuxBft(kernelRule.getKernel());
        bft.height = number + 1;
        Block block = bft.proposeBlock();
        assertEquals(timestamp + 1, block.getTimestamp());
    }

    private Transaction createTransaction(Key to, Key from, long time, long nonce) {
        return new Transaction(
                kernelRule.getKernel().getConfig().network(),
                TransactionType.TRANSFER,
                to.toAddress(),
                SEM.of(10),
                kernelRule.getKernel().getConfig().minTransactionFee(),
                nonce,
                time,
                Bytes.EMPTY_BYTES, Amount.ZERO, Amount.ZERO).sign(from);
    }
}
