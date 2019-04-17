/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.time.Duration;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.consensus.SemuxSync;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

public class MainFrameTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    private SemuxGui gui;
    private MainFrame frame;

    private FrameFixture window;

    @Override
    protected void onSetUp() {
        kernelRule.openBlockchain();

        Key coinbase = new Key();
        WalletModel model = new WalletModel(kernelRule.getKernel().getConfig());
        KernelMock kernel = kernelRule.getKernel();

        gui = new SemuxGui(model, kernel);
        model.setValidators(kernelRule.getKernel().getBlockchain().getValidators());
        model.setLatestBlock(kernel.getBlockchain().getLatestBlock());
        model.setCoinbase(coinbase);
        model.setSyncProgress(new SemuxSync.SemuxSyncProgress(0, 1, 1, Duration.ZERO));

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
