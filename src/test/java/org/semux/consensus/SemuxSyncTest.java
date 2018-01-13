/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Key;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDbRule;
import org.semux.util.Bytes;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SemuxSyncTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDbRule temporaryDBRule = new TemporaryDbRule();

    @Test
    public void testDuplicatedTransaction() {
        // mock blockchain with a single transaction
        Key to = new Key();
        Key from1 = new Key();
        long time = System.currentTimeMillis();
        Transaction tx1 = new Transaction(
                kernelRule.getKernel().getConfig().network(),
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
        SemuxSync semuxSync = spy(new SemuxSync(kernelRule.getKernel()));
        doReturn(true).when(semuxSync).validateBlockVotes(any()); // we don't care about votes here

        // create a tx with the same hash with tx1 from a different signer in the second
        // block
        Key from2 = new Key();
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from2.toAddress(), 1000 * Unit.SEM);
        Transaction tx2 = new Transaction(
                kernelRule.getKernel().getConfig().network(),
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
        AccountState as = kernelRule.getKernel().getBlockchain().getAccountState().track();
        DelegateState ds = kernelRule.getKernel().getBlockchain().getDelegateState().track();
        assertFalse(semuxSync.validateBlock(block2, as, ds));
    }

}
