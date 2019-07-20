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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.TestUtils;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.net.Channel;
import org.semux.net.Peer;
import org.semux.net.msg.MessageQueue;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDatabaseRule;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SemuxSyncTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDatabaseRule temporaryDBRule = new TemporaryDatabaseRule();

    public long validatorInterval;

    @Test
    public void testFastSync() throws Exception {
        List<Key> keys = new ArrayList<>();
        List<String> validators = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            keys.add(new Key());
            validators.add(Hex.encode(keys.get(i).toAddress()));
        }

        BlockchainImpl chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        doNothing().when(chain).updateValidators(anyLong());
        kernelRule.getKernel().setBlockchain(chain);

        validatorInterval = kernelRule.getKernel().getConfig().spec().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);
        when(channel.getRemotePeer()).thenReturn(mock(Peer.class));

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

        AtomicBoolean isRunning = TestUtils.getInternalState(sync, "isRunning", SemuxSync.class);
        isRunning.set(true);
        TestUtils.setInternalState(sync, "toProcess", toProcess, SemuxSync.class);
        AtomicLong target = TestUtils.getInternalState(sync, "target", SemuxSync.class);
        target.set(validatorInterval + 1);

        sync.process();
        TreeSet<Pair<Block, Channel>> currentSet = TestUtils.getInternalState(sync, "currentSet", SemuxSync.class);
        Map<Long, Pair<Block, Channel>> toFinalize = TestUtils.getInternalState(sync, "toFinalize", SemuxSync.class);

        assertTrue(toProcess.isEmpty());
        assertTrue(currentSet.isEmpty());
        assertEquals(toFinalize.size(), validatorInterval);
        assertTrue(TestUtils.getInternalState(sync, "fastSync", SemuxSync.class));

        for (int i = 0; i < validatorInterval; i++) {
            sync.process();
        }

        assertTrue(toFinalize.isEmpty());
        assertEquals(chain.getLatestBlockNumber(), validatorInterval);
        assertFalse(TestUtils.getInternalState(sync, "fastSync", SemuxSync.class));
    }

    @Test
    public void testNormalSync() throws Exception {
        List<Key> keys = new ArrayList<>();
        List<String> validators = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            keys.add(new Key());
            validators.add(Hex.encode(keys.get(i).toAddress()));
        }

        BlockchainImpl chain = spy(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        doReturn(validators).when(chain).getValidators();
        kernelRule.getKernel().setBlockchain(chain);

        validatorInterval = kernelRule.getKernel().getConfig().spec().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);
        when(channel.getRemotePeer()).thenReturn(mock(Peer.class));

        TreeSet<Pair<Block, Channel>> toProcess = new TreeSet<>(
                Comparator.comparingLong(o -> o.getKey().getNumber()));

        AtomicBoolean isRunning = TestUtils.getInternalState(sync, "isRunning", SemuxSync.class);
        isRunning.set(true);
        TestUtils.setInternalState(sync, "toProcess", toProcess, SemuxSync.class);
        AtomicLong target = TestUtils.getInternalState(sync, "target", SemuxSync.class);
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
        sync.process();

        // when fastSync is not activated, votes are validated for each block
        assertTrue(toProcess.isEmpty());
        assertFalse(TestUtils.getInternalState(sync, "fastSync", SemuxSync.class));
        assertEquals(0, chain.getLatestBlockNumber());

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
        sync.process();

        assertTrue(toProcess.isEmpty());
        assertFalse(TestUtils.getInternalState(sync, "fastSync", SemuxSync.class));
        assertEquals(1, chain.getLatestBlockNumber());

        target.set(10 * validatorInterval); // fastSync is activated only at the beginning of a validator set
        sync.process();

        assertFalse(TestUtils.getInternalState(sync, "fastSync", SemuxSync.class));
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

        validatorInterval = kernelRule.getKernel().getConfig().spec().getValidatorUpdateInterval();

        SemuxSync sync = spy(new SemuxSync(kernelRule.getKernel()));
        TestUtils.setInternalState(sync, "lastBlockInSet", validatorInterval, SemuxSync.class);

        MessageQueue msgQueue = mock(MessageQueue.class);
        Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 5161));
        when(channel.getMessageQueue()).thenReturn(msgQueue);
        when(channel.getRemotePeer()).thenReturn(mock(Peer.class));

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

        TestUtils.setInternalState(sync, "currentSet", currentSet, SemuxSync.class);
        Map<Long, Pair<Block, Channel>> toFinalize = TestUtils.getInternalState(sync, "toFinalize", SemuxSync.class);
        TreeSet<Long> toDownload = TestUtils.getInternalState(sync, "toDownload", SemuxSync.class);

        sync.validateSetHashes();

        assertEquals(currentSet.size(), validatorInterval / 2);
        assertTrue(toFinalize.isEmpty());

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

        sync.validateSetHashes();

        assertEquals(currentSet.size(), validatorInterval - 1);
        assertTrue(toFinalize.isEmpty());
        assertTrue(toDownload.contains(validatorInterval));

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
        sync.validateSetHashes();

        assertEquals(currentSet.size(), validatorInterval / 2);
        assertEquals(toFinalize.size(), validatorInterval - currentSet.size() - 1);
        assertTrue(toDownload.contains(validatorInterval - toFinalize.size()));

        Channel channel2 = new Channel(null);
        currentSet.add(Pair.of(lastBlock, channel2));
        sync.validateSetHashes();

        assertEquals(currentSet.size(), validatorInterval / 2);
        assertEquals(toFinalize.size(), validatorInterval - currentSet.size() - 1);

        currentSet.add(Pair.of(validBlock, channel));
        sync.validateSetHashes();

        assertTrue(currentSet.isEmpty());
        assertEquals(toFinalize.size(), validatorInterval);
    }
}
