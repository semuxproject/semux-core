/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.consensus.SemuxSync;
import org.semux.core.Block;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;

@RunWith(MockitoJUnitRunner.class)
public class HomePanelTest {

    @Mock
    WalletModel walletModel;

    @Test
    public void testSyncProgress100() {
        // mock walletModel
        when(walletModel.getLatestBlock()).thenReturn(mock(Block.class));
        when(walletModel.getSyncProgress()).thenReturn(new SemuxSync.SemuxSyncProgress(100L, 100L));

        // mock kernel
        KernelMock kernelMock = spy(new KernelMock());
        HomePanelTestApplication application = GuiActionRunner
                .execute(() -> new HomePanelTestApplication(walletModel, kernelMock));

        FrameFixture window = new FrameFixture(application);
        window.show();

        window.requireVisible();
        window.label("syncProgress").requireText(SwingUtil.formatPercentage(100d));

        window.cleanUp();
    }

    @Test
    public void testProgressFormatter() {
        assertEquals(GUIMessages.get("SyncFinished"),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 100L)));
        assertEquals("12.3 %", HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(1234L, 10000L)));
        assertEquals("0.0 %", HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0L, 10000L)));
        assertEquals(GUIMessages.get("SyncStopped"),
                HomePanel.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 0L)));
        assertEquals(GUIMessages.get("SyncStopped"), HomePanel.SyncProgressFormatter.format(null));
    }
}
