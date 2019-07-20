/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.semux.core.Unit.SEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.semux.TestUtils;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.consensus.Vote;
import org.semux.consensus.VoteType;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

public class BlockchainImportTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDatabaseRule temporaryDBRule = new TemporaryDatabaseRule();

    @Test
    public void testDuplicatedTransaction() {
        // mock blockchain with a single transaction
        Key to = new Key();
        Key from1 = new Key();
        long time = TimeUtil.currentTimeMillis();
        Transaction tx1 = new Transaction(
                kernelRule.getKernel().getConfig().network(),
                TransactionType.TRANSFER,
                to.toAddress(),
                Amount.of(10, SEM),
                kernelRule.getKernel().getConfig().spec().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from1);
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from1.toAddress(), Amount.of(
                1000, SEM));
        Block block1 = kernelRule.createBlock(Collections.singletonList(tx1));
        kernelRule.getKernel().getBlockchain().addBlock(block1);
        // create a tx with the same hash with tx1 from a different signer in the second
        // block
        Key from2 = new Key();
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from2.toAddress(), Amount.of(
                1000, SEM));
        Transaction tx2 = new Transaction(
                kernelRule.getKernel().getConfig().network(),
                TransactionType.TRANSFER,
                to.toAddress(),
                Amount.of(10, SEM),
                kernelRule.getKernel().getConfig().spec().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from2);
        Block block2 = kernelRule.createBlock(Collections.singletonList(tx2));

        // this test case is valid if and only if tx1 and tx2 have the same tx hash
        assert (Arrays.equals(tx1.getHash(), tx2.getHash()));

        // the block should be rejected because of the duplicated tx
        assertFalse(kernelRule.getKernel().getBlockchain().importBlock(block2, false));
    }

    @Test
    public void testValidateCoinbaseMagic() {
        BlockchainImpl blockchain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.enableForks(Fork.UNIFORM_DISTRIBUTION);
        kernelRule.getKernel().setBlockchain(blockchain);

        // block.coinbase = coinbase magic account
        Block block = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                Constants.COINBASE_KEY,
                kernelRule.getKernel().getBlockchain().getLatestBlockNumber() + 1,
                Collections.emptyList(),
                Collections.emptyList());

        assertFalse(kernelRule.getKernel().getBlockchain().importBlock(block, true));

        // tx.to = coinbase magic account
        Transaction tx = TestUtils.createTransaction(kernelRule.getKernel().getConfig(), new Key(),
                Constants.COINBASE_KEY, Amount.ZERO);
        Block block2 = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                new Key(),
                kernelRule.getKernel().getBlockchain().getLatestBlockNumber() + 1,
                Collections.singletonList(tx),
                Collections.singletonList(new TransactionResult()));

        assertFalse(kernelRule.getKernel().getBlockchain().importBlock(block2, true));
    }

    @Test
    public void testValidateBlockVotes() {
        Key key1 = new Key();
        Key key2 = new Key();
        Key key3 = new Key();
        List<String> validators = Arrays.asList(Hex.encode(key1.toAddress()),
                Hex.encode(key2.toAddress()),
                Hex.encode(key3.toAddress()));

        // mock the chain
        BlockchainImpl chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        // prepare block
        Block block = kernelRule.createBlock(Collections.emptyList());
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        byte[] encoded = vote.getEncoded();
        List<Key.Signature> votes = new ArrayList<>();
        block.setVotes(votes);

        // tests
        assertFalse(chain.validateBlockVotes(block));

        votes.add(key1.sign(encoded));
        votes.add(key1.sign(encoded));
        assertFalse(chain.validateBlockVotes(block));

        votes.add(key2.sign(encoded));
        assertTrue(chain.validateBlockVotes(block));

        votes.add(key3.sign(encoded));
        assertTrue(chain.validateBlockVotes(block));

        votes.clear();
        votes.add(new Key().sign(encoded));
        votes.add(new Key().sign(encoded));
        assertFalse(chain.validateBlockVotes(block));
    }

    @Test
    public void testCheckpoints() {
        Key key1 = new Key();
        List<String> validators = Collections.singletonList(Hex.encode(key1.toAddress()));

        // mock checkpoints
        Map<Long, byte[]> checkpoints = new HashMap<>();
        checkpoints.put(1L, RandomUtils.nextBytes(32));
        Config config = spy(kernelRule.getKernel().getConfig());
        when(config.checkpoints()).thenReturn(checkpoints);

        // mock the chain
        BlockchainImpl chain = spy(new BlockchainImpl(config, temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        // prepare block
        Block block = kernelRule.createBlock(Collections.emptyList());
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        block.setVotes(Collections.singletonList(vote.sign(key1).getSignature()));

        // tests
        assertFalse(chain.importBlock(block, false));
    }
}
