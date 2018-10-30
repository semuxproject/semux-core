/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.semux.core.Amount.Unit.SEM;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.semux.TestUtils;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Fork;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.net.Channel;
import org.semux.net.msg.MessageQueue;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SemuxSyncTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDatabaseRule temporaryDBRule = new TemporaryDatabaseRule();

    public long validatorInterval;

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
                SEM.of(10),
                kernelRule.getKernel().getConfig().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from1);
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from1.toAddress(), SEM.of(1000));
        Block block1 = kernelRule.createBlock(Collections.singletonList(tx1));
        kernelRule.getKernel().getBlockchain().addBlock(block1);
        SemuxSync semuxSync = spy(new SemuxSync(kernelRule.getKernel()));
        doReturn(true).when(semuxSync).validateBlockVotes(any()); // we don't care about votes here

        // create a tx with the same hash with tx1 from a different signer in the second
        // block
        Key from2 = new Key();
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from2.toAddress(), SEM.of(1000));
        Transaction tx2 = new Transaction(
                kernelRule.getKernel().getConfig().network(),
                TransactionType.TRANSFER,
                to.toAddress(),
                SEM.of(10),
                kernelRule.getKernel().getConfig().minTransactionFee(),
                0,
                time,
                Bytes.EMPTY_BYTES).sign(from2);
        Block block2 = kernelRule.createBlock(Collections.singletonList(tx2));

        // this test case is valid if and only if tx1 and tx2 have the same tx hash
        assert (Arrays.equals(tx1.getHash(), tx2.getHash()));

        // the block should be rejected because of the duplicated tx
        AccountState as = kernelRule.getKernel().getBlockchain().getAccountState().track();
        DelegateState ds = kernelRule.getKernel().getBlockchain().getDelegateState().track();
        assertFalse(semuxSync.validateBlock(block2, as, ds));
    }

    @Test
    public void testValidateCoinbaseMagic() {
        BlockchainImpl blockchain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        when(blockchain.isForkActivated(eq(Fork.UNIFORM_DISTRIBUTION), anyLong())).thenReturn(true);
        kernelRule.getKernel().setBlockchain(blockchain);

        // block.coinbase = coinbase magic account
        Block block = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                Constants.COINBASE_KEY,
                kernelRule.getKernel().getBlockchain().getLatestBlockNumber() + 1,
                Collections.emptyList(),
                Collections.emptyList());

        AccountState as = kernelRule.getKernel().getBlockchain().getAccountState().track();
        DelegateState ds = kernelRule.getKernel().getBlockchain().getDelegateState().track();
        SemuxSync semuxSync = new SemuxSync(kernelRule.getKernel());
        assertFalse(semuxSync.validateBlock(block, as, ds));

        // tx.to = coinbase magic account
        Transaction tx = TestUtils.createTransaction(kernelRule.getKernel().getConfig(), new Key(),
                Constants.COINBASE_KEY, Amount.ZERO);
        Block block2 = TestUtils.createBlock(
                kernelRule.getKernel().getBlockchain().getLatestBlock().getHash(),
                new Key(),
                kernelRule.getKernel().getBlockchain().getLatestBlockNumber() + 1,
                Collections.singletonList(tx),
                Collections.singletonList(new TransactionResult(true)));

        assertFalse(semuxSync.validateBlock(block2, as, ds));
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

        // mock sync manager
        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));

        // prepare block
        Block block = kernelRule.createBlock(Collections.emptyList());
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        byte[] encoded = vote.getEncoded();
        List<Key.Signature> votes = new ArrayList<>();
        block.setVotes(votes);

        // tests
        assertFalse(sync.validateBlockVotes(block));

        votes.add(key1.sign(encoded));
        votes.add(key1.sign(encoded));
        assertFalse(sync.validateBlockVotes(block));

        votes.add(key2.sign(encoded));
        assertTrue(sync.validateBlockVotes(block));

        votes.add(key3.sign(encoded));
        assertTrue(sync.validateBlockVotes(block));

        votes.clear();
        votes.add(new Key().sign(encoded));
        votes.add(new Key().sign(encoded));
        assertFalse(sync.validateBlockVotes(block));
    }

    @Test
    public void testCheckpoints() {
        Key key1 = new Key();
        List<String> validators = Collections.singletonList(Hex.encode(key1.toAddress()));

        // mock the chain
        BlockchainImpl chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        // mock sync manager
        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));

        // prepare block
        Block block = kernelRule.createBlock(Collections.emptyList());
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        block.setVotes(Collections.singletonList(vote.sign(key1).getSignature()));

        // mock checkpoints
        Map<Long, byte[]> checkpoints = new HashMap<>();
        checkpoints.put(block.getNumber(), RandomUtils.nextBytes(32));
        Config config = spy(kernelRule.getKernel().getConfig());
        when(config.checkpoints()).thenReturn(checkpoints);
        Whitebox.setInternalState(sync, "config", config);

        // tests
        assertFalse(sync.validateBlock(block, chain.getAccountState(), chain.getDelegateState()));
    }

    @Test
    public void testFastSync() throws Exception {
        List<Key> keys = new ArrayList<>();
        List<String> validators = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            keys.add(new Key());
            validators.add(Hex.encode(keys.get(i).toAddress()));
        }

        BlockchainImpl chain = PowerMockito
                .spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        PowerMockito.doNothing().when(chain, "updateValidators", anyLong());
        kernelRule.getKernel().setBlockchain(chain);

        validatorInterval = kernelRule.getKernel().getConfig().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);

        TreeSet<Pair<Block, Channel>> toProcess = new TreeSet<>(
                Comparator.comparingLong(o -> o.getKey().getNumber()));

        for (int i = 0; i < validatorInterval; i++) {
            Block block;
            if (i == 0) {
                block = kernelRule.createBlock(Collections.emptyList());
            } else {
                block = kernelRule.createBlock(Collections.emptyList(), toProcess.last().getKey().getHeader());
            }

            Vote vote;
            if (i == validatorInterval - 1) { // votes are validated for the last block in the set
                vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                        block.getHash());
            } else { // vote validation is skipped for all other blocks in the set
                vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_REJECT, block.getNumber(), block.getView(),
                        block.getHash());
            }
            List<Signature> votes = new ArrayList<>();
            for (Key key : keys) {
                votes.add(vote.sign(key).getSignature());
            }

            block.setVotes(votes);
            toProcess.add(Pair.of(block, channel));
            Thread.sleep(2); // to avoid duplicate time stamp
        }

        AtomicBoolean isRunning = Whitebox.getInternalState(sync, "isRunning");
        isRunning.set(true);
        Whitebox.setInternalState(sync, "toProcess", toProcess);
        AtomicLong target = Whitebox.getInternalState(sync, "target");
        target.set(validatorInterval + 1);

        Whitebox.invokeMethod(sync, "process");
        TreeSet<Pair<Block, Channel>> currentSet = Whitebox.getInternalState(sync, "currentSet");
        Map<Long, Pair<Block, Channel>> toFinalize = Whitebox.getInternalState(sync, "toFinalize");

        assert (toProcess.isEmpty());
        assert (currentSet.isEmpty());
        assert (toFinalize.size() == validatorInterval);
        assertTrue(Whitebox.getInternalState(sync, "fastSync"));

        for (int i = 0; i < validatorInterval; i++) {
            Whitebox.invokeMethod(sync, "process");
        }

        assert (toFinalize.isEmpty());
        assert (chain.getLatestBlockNumber() == validatorInterval);
        assertFalse(Whitebox.getInternalState(sync, "fastSync"));
    }

    @Test
    public void testNormalSync() throws Exception {
        List<Key> keys = new ArrayList<>();
        List<String> validators = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            keys.add(new Key());
            validators.add(Hex.encode(keys.get(i).toAddress()));
        }

        BlockchainImpl chain = PowerMockito
                .spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        validatorInterval = kernelRule.getKernel().getConfig().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);

        TreeSet<Pair<Block, Channel>> toProcess = new TreeSet<>(
                Comparator.comparingLong(o -> o.getKey().getNumber()));

        AtomicBoolean isRunning = Whitebox.getInternalState(sync, "isRunning");
        isRunning.set(true);
        Whitebox.setInternalState(sync, "toProcess", toProcess);
        AtomicLong target = Whitebox.getInternalState(sync, "target");
        target.set(validatorInterval - 1); // when the remaining number of blocks to sync < validatorInterval fastSync
                                           // is not activated

        Block block = kernelRule.createBlock(Collections.emptyList());
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_REJECT, block.getNumber(), block.getView(),
                block.getHash());
        for (int i = 0; i < 4; i++) {
            List<Signature> votes = new ArrayList<>();
            for (Key key : keys) {
                votes.add(vote.sign(key).getSignature());
            }
            block.setVotes(votes);
        }

        toProcess.add(Pair.of(block, channel));
        Whitebox.invokeMethod(sync, "process");

        // when fastSync is not activated, votes are validated for each block
        assert (toProcess.isEmpty());
        assertFalse(Whitebox.getInternalState(sync, "fastSync"));
        assert (chain.getLatestBlockNumber() == 0);

        vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        for (int i = 0; i < 4; i++) {
            List<Signature> votes = new ArrayList<>();
            for (Key key : keys) {
                votes.add(vote.sign(key).getSignature());
            }
            block.setVotes(votes);
        }

        toProcess.add(Pair.of(block, channel));
        Whitebox.invokeMethod(sync, "process");

        assert (toProcess.isEmpty());
        assertFalse(Whitebox.getInternalState(sync, "fastSync"));
        assert (chain.getLatestBlockNumber() == 1);

        target.set(10 * validatorInterval); // fastSync is activated only at the beginning of a validator set
        Whitebox.invokeMethod(sync, "process");

        assertFalse(Whitebox.getInternalState(sync, "fastSync"));
    }

    @Test
    public void testValidateSetHashes() throws Exception {
        List<Key> keys = new ArrayList<>();
        List<String> validators = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            keys.add(new Key());
            validators.add(Hex.encode(keys.get(i).toAddress()));
        }

        BlockchainImpl chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        validatorInterval = kernelRule.getKernel().getConfig().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        Whitebox.setInternalState(sync, "lastBlockInSet", validatorInterval);

        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);

        TreeSet<Pair<Block, Channel>> currentSet = new TreeSet<>(
                Comparator.comparingLong(o -> o.getKey().getNumber()));

        for (int i = 0; i < validatorInterval / 2; i++) {
            Block block;
            if (i == 0) {
                block = kernelRule.createBlock(Collections.emptyList());
            } else {
                block = kernelRule.createBlock(Collections.emptyList(), currentSet.last().getKey().getHeader());
            }
            currentSet.add(Pair.of(block, channel));
        }

        Whitebox.setInternalState(sync, "currentSet", currentSet);
        Map<Long, Pair<Block, Channel>> toFinalize = Whitebox.getInternalState(sync, "toFinalize");
        TreeSet<Long> toDownload = Whitebox.getInternalState(sync, "toDownload");

        Whitebox.invokeMethod(sync, "validateSetHashes");

        assert (currentSet.size() == validatorInterval / 2);
        assert (toFinalize.isEmpty());

        Block invalidBlock = kernelRule.createBlock(Collections.emptyList(), currentSet.last().getKey().getHeader());
        Block validBlock = kernelRule.createBlock(Collections.emptyList(), currentSet.last().getKey().getHeader());
        currentSet.add(Pair.of(validBlock, channel));

        for (int i = 0; i < (int) (Math.ceil(validatorInterval / 2)) - 2; i++) {
            Block block = kernelRule.createBlock(Collections.emptyList(), currentSet.last().getKey().getHeader());
            currentSet.add(Pair.of(block, channel));
        }

        currentSet.remove(Pair.of(validBlock, channel));
        currentSet.add(Pair.of(invalidBlock, channel));

        Block lastBlock = kernelRule.createBlock(Collections.emptyList(), currentSet.last().getKey().getHeader());
        currentSet.add(Pair.of(lastBlock, channel));

        Whitebox.invokeMethod(sync, "validateSetHashes"); // last block votes are validated

        assert (currentSet.size() == validatorInterval - 1);
        assert (toFinalize.isEmpty());
        assert (toDownload.contains(validatorInterval));

        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, lastBlock.getNumber(), lastBlock.getView(),
                lastBlock.getHash());
        for (int i = 0; i < 4; i++) {
            List<Signature> votes = new ArrayList<>();
            for (Key key : keys) {
                votes.add(vote.sign(key).getSignature());
            }
            lastBlock.setVotes(votes);
        }

        currentSet.add(Pair.of(lastBlock, channel));
        Whitebox.invokeMethod(sync, "validateSetHashes"); // invalid block is added back to the download queue

        assert (currentSet.size() == validatorInterval / 2);
        assert (toFinalize.size() == validatorInterval - currentSet.size() - 1);
        assert (toDownload.contains(validatorInterval - toFinalize.size()));

        Channel channel2 = new Channel(null);
        currentSet.add(Pair.of(lastBlock, channel2));
        Whitebox.invokeMethod(sync, "validateSetHashes"); // only one block with the same height is added to toFinalize

        assert (currentSet.size() == validatorInterval / 2);
        assert (toFinalize.size() == validatorInterval - currentSet.size() - 1);

        currentSet.add(Pair.of(validBlock, channel));
        Whitebox.invokeMethod(sync, "validateSetHashes");

        assert (currentSet.isEmpty());
        assert (toFinalize.size() == validatorInterval);
    }
}
