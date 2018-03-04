/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.awaitility.Awaitility.await;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsoleDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testBasicUse() throws InterruptedException {

        ConsoleDialogTestApplication application = GuiActionRunner
                .execute(() -> new ConsoleDialogTestApplication(walletModel, kernelRule1.getKernel()));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();
        DialogFixture console = window.dialog("Console").requireVisible();
        JTextComponentFixture consoleText = console.textBox("txtConsole");

        // check txtInput
        console.textBox("txtInput").click().requireFocused().requireEditable().requireVisible();

        // help
        console.textBox("txtInput").enterText("help\n");
        await().until(() -> consoleText.text().contains("transfer"));

        // listAccounts
        console.textBox("txtInput").enterText("listAccounts\n");
        String walletAddress = kernelRule1.getKernel().getWallet().getAccount(0).toAddressString();

        await().until(() -> consoleText.text().contains(walletAddress));
    }

    @Override
    protected void onSetUp() {

    }
}
