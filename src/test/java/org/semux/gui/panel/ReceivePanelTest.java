/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Timeout;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.core.state.Account;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class ReceivePanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    ReceivePanelTestApplication application;

    FrameFixture window;

    @Override
    protected void onSetUp() {

    }

    @Test
    public void testCopyAddress() {
        EdDSA key1 = new EdDSA();
        EdDSA key2 = new EdDSA();
        WalletAccount acc1 = new WalletAccount(key1, new Account(key1.toAddress(), 1, 1, 1));
        WalletAccount acc2 = new WalletAccount(key2, new Account(key2.toAddress(), 2, 2, 2));

        // mock walletModel
        when(walletModel.getAccounts()).thenReturn(Arrays.asList(acc1, acc2));

        // mock kernel
        KernelMock kernelMock = spy(kernelRule1.getKernel());
        application = GuiActionRunner.execute(() -> new ReceivePanelTestApplication(walletModel, kernelMock));

        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        JTableFixture table = window.table("accountsTable").requireVisible().requireRowCount(2);
        table.cell(Hex.PREF + key2.toAddressString()).click();
        table.requireSelectedRows(1);
        window.button("btnCopyAddress").requireVisible().click();
        window.optionPane(Timeout.timeout(1000)).requireVisible()
                .requireMessage(GUIMessages.get("AddressCopied", Hex.PREF + key2.toAddressString()));

        assertEquals(Hex.PREF + key2.toAddressString(), GuiActionRunner
                .execute(() -> Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)));
    }
}
