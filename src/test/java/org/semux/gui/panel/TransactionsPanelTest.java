/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.semux.core.Unit.MILLI_SEM;
import static org.semux.core.Unit.SEM;

import java.util.Collections;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.core.Amount;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.state.Account;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionsPanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    TransactionsPanelTestApplication application;

    FrameFixture window;

    @Override
    protected void onSetUp() {

    }

    @Test
    public void testTransactions() 
    {
    	KernelMock kernel = kernelRule.getKernel();
    	kernel.start();
    	
        Key key = new Key();
        Amount $1 = Amount.of(1);
        WalletAccount acc = spy(new WalletAccount(key, new Account(key.toAddress(), $1, $1, 1), null));

        Transaction tx = new Transaction(kernel.getConfig().network(),
                TransactionType.TRANSFER,
                Bytes.random(Key.ADDRESS_LEN),
                key.toAddress(),
                Amount.of(1, SEM),
                Amount.of(10, MILLI_SEM),
                0,
                TimeUtil.currentTimeMillis(),
                Bytes.EMPTY_BYTES,
                kernel.getConfig().forkEd25519ContractEnabled());
        tx.sign(new Key());
        acc.setTransactions(Collections.singletonList(tx));

        // mock walletModel
        when(walletModel.getAccounts()).thenReturn(Collections.singletonList(acc));

        // mock kernel
        KernelMock kernelMock = spy(kernel);
        Blockchain chain = mock(Blockchain.class);
        DelegateState ds = mock(DelegateState.class);
        PendingManager pendingManager = mock(PendingManager.class);
        when(ds.getDelegateByAddress(any())).thenReturn(null);
        when(chain.getDelegateState()).thenReturn(ds);
        when(kernelMock.getBlockchain()).thenReturn(chain);
        when(kernelMock.getPendingManager()).thenReturn(pendingManager);
        application = GuiActionRunner.execute(() -> new TransactionsPanelTestApplication(walletModel, kernelMock));

        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        assertEquals(1, window.table("transactionsTable").rowCount());

        window.table("transactionsTable").cell(TransactionType.TRANSFER.name()).doubleClick();
        window.dialog().requireVisible();
        assertEquals(GuiMessages.get("Transaction"), window.dialog().target().getTitle());
    }
}
