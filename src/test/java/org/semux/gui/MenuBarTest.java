/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.junit.Assert.assertEquals;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mock;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.rules.KernelRule;

/**
 * TODO: complete the action tests
 */
public class MenuBarTest extends AssertJSwingJUnitTestCase {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    MenuBarTestApplication frame;

    FrameFixture window;

    @Override
    protected void onSetUp() {
        frame = GuiActionRunner.execute(() -> new MenuBarTestApplication(walletModel, kernelRule.getKernel()));

        // IMPORTANT: note the call to 'robot()'
        // we must use the Robot from AssertJSwingJUnitTestCase
        window = new FrameFixture(robot(), frame);
        window.show(); // shows the frame to test
    }

    @Override
    public void onTearDown() {

    }

    @Test
    public void testChangePassword() {
        window.menuItem("itemChangePassword").click();

        window.dialog().requireVisible();
        assertEquals(GUIMessages.get("ChangePassword"), window.dialog().target().getTitle());
    }

    @Test
    public void testRecoverAccounts() {
        window.menuItem("itemRecover").click();

        window.fileChooser().requireVisible().cancel();
    }

    @Test
    public void testBackupWallet() {
        window.menuItem("itemBackupWallet").click();

        window.fileChooser().requireVisible().cancel();
    }

    @Test
    public void testImportPrivateKey() {
        window.menuItem("itemImportPrivateKey").click();

        window.dialog().requireVisible();
        window.dialog().close();
    }

    @Test
    public void testExportPrivateKey() {
        window.menuItem("itemExportPrivateKey").click();

        window.dialog().requireVisible();
        assertEquals(GUIMessages.get("ExportPrivateKey"), window.dialog().target().getTitle());

        window.dialog().close();
    }

    @Test
    public void testAbout() {
        window.menuItem("itemAbout").click();

        window.optionPane().requireMessage(frame.gui.getKernel().getConfig().getClientId());
    }
}
