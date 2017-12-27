/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.rules.KernelRule;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.Bytes;

public class SemuxSyncTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Rule
    public TemporaryDBRule temporaryDBRule = new TemporaryDBRule();

    @Test
    public void testDuplicatedTransaction() {
        // mock blockchain with a single transaction
        EdDSA from = new EdDSA();
        Transaction tx = new Transaction(
                TransactionType.TRANSFER, //
                new EdDSA().toAddress(), //
                10 * Unit.SEM, //
                kernelRule.getKernel().getConfig().minTransactionFee(), //
                0, //
                System.currentTimeMillis(), //
                Bytes.EMPTY_BYTES //
        ).sign(from);
        kernelRule.getKernel().setBlockchain(new BlockchainImpl(kernelRule.getKernel().getConfig(), temporaryDBRule));
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from.toAddress(), 1000 * Unit.SEM);
        Block block1 = kernelRule.createBlock(Arrays.asList(tx));
        kernelRule.getKernel().getBlockchain().addBlock(block1);
        SemuxSync semuxSync = new SemuxSync(kernelRule.getKernel());

        // create a duplicated tx in the second block
        Block block2 = kernelRule.createBlock(Arrays.asList(tx));

        // the block should be rejected because of the duplicated tx
        AccountState as = kernelRule.getKernel().getBlockchain().getAccountState().track();
        DelegateState ds = kernelRule.getKernel().getBlockchain().getDelegateState().track();
        assertFalse(semuxSync.validateBlock(block2, as, ds));
    }

}
