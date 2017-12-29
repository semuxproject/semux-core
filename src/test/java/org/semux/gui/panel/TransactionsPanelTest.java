/**
 * Copyright (c) 2017 The Semux Developers
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
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.Account;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;

@RunWith(MockitoJUnitRunner.class)
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
    public void testCopyAddress() {
        EdDSA key = new EdDSA();
        WalletAccount acc = spy(new WalletAccount(key, new Account(key.toAddress(), 1, 1, 1)));

        Transaction tx = new Transaction(TransactionType.TRANSFER,
                Bytes.random(EdDSA.ADDRESS_LEN),
                1 * Unit.SEM,
                10 * Unit.MILLI_SEM,
                0,
                System.currentTimeMillis(),
                Bytes.EMPTY_BYTES);
        acc.setTransactions(Collections.singletonList(tx));

        // mock walletModel
        when(walletModel.getAccounts()).thenReturn(Collections.singletonList(acc));

        // mock kernel
        KernelMock kernelMock = spy(kernelRule.getKernel());
        Blockchain chain = mock(Blockchain.class);
        DelegateState ds = mock(DelegateState.class);
        when(ds.getDelegateByAddress(any())).thenReturn(null);
        when(chain.getDelegateState()).thenReturn(ds);
        when(kernelMock.getBlockchain()).thenReturn(chain);
        application = GuiActionRunner.execute(() -> new TransactionsPanelTestApplication(walletModel, kernelMock));

        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        assertEquals(1, window.table("transactionsTable").rowCount());
    }
}
