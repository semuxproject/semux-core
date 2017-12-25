/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
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
public class ReceivePanelTest {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    FrameFixture window;

    @After
    public void teardown() {
        if (window != null) {
            window.cleanUp();
        }
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
        ReceivePanelTestApplication application = GuiActionRunner
                .execute(() -> new ReceivePanelTestApplication(walletModel, kernelMock));

        window = new FrameFixture(application);
        window.show();
        window.requireVisible();

        window.table().selectRows(1);
        window.button("btnCopyAddress").click();
        window.dialog().requireVisible();

        window.dialog().optionPane()
                .requireMessage(GUIMessages.get("AddressCopied", Hex.PREF + key2.toAddressString()));
    }
}
