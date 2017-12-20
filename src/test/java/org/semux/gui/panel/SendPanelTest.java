/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.swing.core.matcher.JButtonMatcher.withText;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.WalletRule;
import org.semux.message.GUIMessages;

@RunWith(MockitoJUnitRunner.class)
public class SendPanelTest {

    @Rule
    public WalletRule walletRule = new WalletRule(1000, 1000);

    @Captor
    ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

    SendPanelTestApplication application;

    FrameFixture window;

    @Mock
    PendingManager pendingManager;

    EdDSA recipient;

    @Before
    public void setUp() {
        recipient = new EdDSA();
    }

    @After
    public void tearDown() {
        window.cleanUp();
    }

    @Test
    public void testSendSuccessfully() {
        testSend(100, new PendingManager.ProcessTransactionResult(1));

        // a confirmation dialog should be displayed
        window.dialog().requireVisible();
        window.dialog().button(withText("Yes")).requireVisible();

        // filled transaction should be sent to PendingManager once "Yes" button is
        // clicked
        window.dialog().button(withText("Yes")).click();
        verify(pendingManager).addTransactionSync(transactionArgumentCaptor.capture());

        // verify transaction
        Transaction tx = transactionArgumentCaptor.getValue();
        assertEquals(TransactionType.TRANSFER, tx.getType());
        assertArrayEquals(recipient.toAddress(), tx.getTo());
        assertEquals(100 * Unit.SEM, tx.getValue());
        assertEquals(application.kernelMock.getConfig().minTransactionFee(), tx.getFee());
    }

    @Test
    public void testSendFailure() {
        testSend(10000, new PendingManager.ProcessTransactionResult(0, TransactionResult.Error.INSUFFICIENT_AVAILABLE));
        window.dialog().requireVisible();
        assertEquals(GUIMessages.get("ErrorDialogTitle"), window.dialog().target().getTitle());
    }

    private void testSend(int toSendSEM, PendingManager.ProcessTransactionResult mockResult) {
        application = GuiActionRunner.execute(() -> new SendPanelTestApplication(walletRule.walletModel));

        // mock pending manager
        when(pendingManager.getNonce(any())).thenReturn(RandomUtils.nextLong());
        when(pendingManager.addTransactionSync(any())).thenReturn(mockResult);
        when(application.kernelMock.getPendingManager()).thenReturn(pendingManager);

        // create window
        window = new FrameFixture(application);
        window.show();

        // fill form
        window.textBox("toText").setText(Hex.encode0x(recipient.toAddress()));
        window.textBox("amountText").setText(String.valueOf(toSendSEM));
        window.button("sendButton").click();
    }
}
