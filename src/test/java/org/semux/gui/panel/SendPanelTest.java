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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.Account;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;

public class SendPanelTest {

    @Captor
    ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

    @Test
    public void testSendTransferMany() throws InterruptedException {
        EdDSA key = new EdDSA();
        Account account = new Account(key.toAddress(), 1000 * Unit.SEM, 1500 * Unit.SEM, RandomUtils.nextInt(1, 100));
        WalletAccount walletAccount = new WalletAccount(key, account);
        List<WalletAccount> accountList = Collections.singletonList(walletAccount);
        WalletModel walletModel = mock(WalletModel.class);
        when(walletModel.getAccounts()).thenReturn(accountList);

        SendPanelTestApplication application = GuiActionRunner.execute(() -> new SendPanelTestApplication(walletModel));

        // mock pending manager
        PendingManager pendingManager = mock(PendingManager.class);
        when(pendingManager.getNonce(any())).thenReturn(RandomUtils.nextLong());
        when(application.kernelMock.getPendingManager()).thenReturn(pendingManager);

        // create window
        FrameFixture window = new FrameFixture(application);
        window.show();

        // fill form
        EdDSA recipient = new EdDSA();
        window.textBox("toText").setText(Hex.encode0x(recipient.toAddress()));
        window.textBox("amountText").setText("100");
        window.button("sendButton").click();

        // a confirmation dialog should be displayed
        window.dialog().requireVisible();
        window.dialog().button(withText("Yes")).requireVisible();

        // filled transaction should be sent to PendingManager once "Yes" button is
        // clicked
        window.dialog().button(withText("Yes")).click();
        verify(pendingManager).addTransactionSync(transactionArgumentCaptor.capture());
        Transaction tx = transactionArgumentCaptor.getValue();
        assertEquals(TransactionType.TRANSFER, tx.getType());
        assertArrayEquals(recipient.toAddress(), tx.getTo());
        assertEquals(100 * Unit.SEM, tx.getValue());
        assertEquals(application.kernelMock.getConfig().minTransactionFee() * 2, tx.getFee());

        // clean up
        window.cleanUp();
    }
}
