/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.assertj.swing.core.matcher.JButtonMatcher.withText;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Point;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.annotation.RunsInEDT;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.WalletModelRule;
import org.semux.message.GUIMessages;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class SendPanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    public WalletModelRule walletRule = new WalletModelRule(1000, 1000);

    @Mock
    PendingManager pendingManager;

    @Captor
    ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

    SendPanelTestApplication application;

    FrameFixture window;

    EdDSA recipient;

    KernelMock kernelMock;

    @Override
    protected void onSetUp() {
        recipient = new EdDSA();
    }

    @Test
    @GUITest
    @RunsInEDT
    public void testSendSuccessfully() {
        testSend(100, new PendingManager.ProcessTransactionResult(1));

        // 1. a confirmation dialog should be displayed
        window.dialog().requireVisible().button(withText("Yes")).requireVisible().click();

        // 2. filled transaction should be sent to PendingManager once "Yes" button is
        // clicked
        assertEquals(GUIMessages.get("SuccessDialogTitle"), window.dialog().target().getTitle());
        verify(pendingManager).addTransactionSync(transactionArgumentCaptor.capture());

        // verify transaction
        Transaction tx = transactionArgumentCaptor.getValue();
        assertEquals(TransactionType.TRANSFER, tx.getType());
        assertArrayEquals(recipient.toAddress(), tx.getTo());
        assertEquals(100 * Unit.SEM, tx.getValue());
        assertEquals(kernelMock.getConfig().minTransactionFee(), tx.getFee());
    }

    @Test
    @GUITest
    @RunsInEDT
    public void testSendFailure() {
        testSend(10000, new PendingManager.ProcessTransactionResult(0, TransactionResult.Error.INSUFFICIENT_AVAILABLE));
        window.dialog().requireVisible();
        assertEquals(GUIMessages.get("ErrorDialogTitle"), window.dialog().target().getTitle());
    }

    private void testSend(int toSendSEM, PendingManager.ProcessTransactionResult mockResult) {
        kernelMock = spy(kernelRule1.getKernel());
        application = GuiActionRunner.execute(() -> new SendPanelTestApplication(walletRule.walletModel, kernelMock));

        // mock pending manager
        when(pendingManager.getNonce(any())).thenReturn(RandomUtils.nextLong());
        when(pendingManager.addTransactionSync(any())).thenReturn(mockResult);
        when(kernelMock.getPendingManager()).thenReturn(pendingManager);

        // create window
        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveTo(new Point(0, 0)).moveToFront();

        // fill form
        window.textBox("toText").requireEditable().setText(Hex.encode0x(recipient.toAddress()));
        window.textBox("amountText").requireEditable().setText(String.valueOf(toSendSEM));
        window.button("sendButton").requireVisible().click();
    }
}
