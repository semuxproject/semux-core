/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.db.MemoryDB.MemoryDBFactory;
import org.semux.net.ChannelManager;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxBFTTest {

    private static final Logger logger = LoggerFactory.getLogger(SemuxBFTTest.class);

    private static KernelMock kernel;
    private static SemuxBFT bft;

    @BeforeClass
    public static void setup() throws InterruptedException {
        kernel = new KernelMock();

        kernel.setBlockchain(new BlockchainImpl(kernel.getConfig(), new MemoryDBFactory()));
        kernel.setChannelManager(new ChannelManager());
        kernel.setPendingManager(new PendingManager(kernel));

        kernel.setSyncManager(new SemuxSync(kernel));
        kernel.setConsensus(bft = new SemuxBFT(kernel));

        kernel.getPendingManager().start();

        new Thread(() -> kernel.getConsensus().start(), "cons").start();
        Thread.sleep(200);
    }

    @Test
    public void testStart() {
        Assert.assertTrue(kernel.getConsensus().isRunning());
    }

    @Test
    public void testVotesSerialization() {
        EdDSA key1 = new EdDSA();
        EdDSA key2 = new EdDSA();

        List<Transaction> transactions = new ArrayList<>();
        List<TransactionResult> results = new ArrayList<>();

        long number = 1;
        byte[] coinbase = key1.toAddress();
        byte[] prevHash = kernel.getBlockchain().getGenesis().getHash();
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};
        int view = 1;

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results);

        List<Signature> votes = new ArrayList<>();
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), view, block.getHash())
                .sign(key1);
        votes.add(vote.getSignature());
        vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), view, block.getHash()).sign(key2);
        votes.add(vote.getSignature());

        block.setView(view);
        block.setVotes(votes);
        block = Block.fromBytes(block.toBytesHeader(), block.toBytesTransactions(), block.toBytesResults(),
                block.toBytesVotes());

        for (Signature sig : block.getVotes()) {
            ByteArray addr = ByteArray.of(Hash.h160(sig.getPublicKey()));

            assertTrue(addr.equals(ByteArray.of(key1.toAddress())) || addr.equals(ByteArray.of(key2.toAddress())));
            assertTrue(EdDSA.verify(vote.getEncoded(), sig));
        }
    }

    @Test
    public void testIsPrimary() {
        List<String> validators = kernel.getBlockchain().getValidators();
        int blocks = 1000;
        int repeat = 0;
        int last = -1;

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
        logger.info("consecutive validator probability: {}/{}", repeat, blocks);
        assertEquals(1.0 / validators.size(), (double) repeat / blocks, 0.05);
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        bft.stop();

        Thread.sleep(200);
    }
}
