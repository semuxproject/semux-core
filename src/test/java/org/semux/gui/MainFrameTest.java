/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.crypto.EdDSA;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

public class MainFrameTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    private SemuxGUI gui;
    private MainFrame frame;

    private FrameFixture window;

    @Override
    protected void onSetUp() {
        kernelRule.openBlockchain();

        EdDSA coinbase = new EdDSA();
        WalletModel model = new WalletModel();
        KernelMock kernel = kernelRule.getKernel();
        gui = new SemuxGUI(model, kernel);
        model.setLatestBlock(kernel.getBlockchain().getLatestBlock());
        model.setCoinbase(coinbase);

        frame = GuiActionRunner.execute(() -> new MainFrame(gui));

        // IMPORTANT: note the call to 'robot()'
        // we must use the Robot from AssertJSwingJUnitTestCase
        window = new FrameFixture(robot(), frame);
        window.show(); // shows the frame to test
    }

    @Override
    public void onTearDown() {
        kernelRule.closeBlockchain();
    }

    @Test
    public void testBasics() {
        window.requireVisible();
    }
}
