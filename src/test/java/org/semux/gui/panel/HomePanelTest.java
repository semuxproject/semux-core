/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.consensus.SemuxSync;
import org.semux.core.Block;
import org.semux.crypto.EdDSA;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class HomePanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testSyncProgress100() {
        // mock progress
        SemuxSync.SemuxSyncProgress progress = new SemuxSync.SemuxSyncProgress(100L, 100L);

        // mock walletModel
        when(walletModel.getLatestBlock()).thenReturn(mock(Block.class));
        when(walletModel.getCoinbase()).thenReturn(new EdDSA());
        when(walletModel.getSyncProgress()).thenReturn(progress);

        // mock kernel
        KernelMock kernelMock = spy(kernelRule1.getKernel());
        HomePanelTestApplication application = GuiActionRunner
                .execute(() -> new HomePanelTestApplication(walletModel, kernelMock));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        window.requireVisible();
        window.label("syncProgress").requireText(HomePanel.SyncProgressFormatter.format(progress));

        window.cleanUp();
        application.dispose();
    }

    @Test
    public void testProgressFormatter() {
        assertEquals(GUIMessages.get("SyncFinished"),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 100L)));
        assertEquals(SwingUtil.formatPercentage(12.3),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(1234L, 10000L)));
        assertEquals(SwingUtil.formatPercentage(0),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0L, 10000L)));
        assertEquals(GUIMessages.get("SyncStopped"),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 0L)));
        assertEquals(GUIMessages.get("SyncStopped"), HomePanel.SyncProgressFormatter.format(null));
    }

    @Override
    protected void onSetUp() {

    }
}
