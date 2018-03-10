/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.junit.Assert.assertEquals;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.consensus.SemuxSync;
import org.semux.message.GuiMessages;

public class StatusBarTest extends AssertJSwingJUnitTestCase {

    @Test
    public void testSyncProgress100() {
        // mock progress
        SemuxSync.SemuxSyncProgress progress = new SemuxSync.SemuxSyncProgress(1234L, 10000L);

        StatusBarTestApplication application = GuiActionRunner
                .execute(() -> new StatusBarTestApplication());

        GuiActionRunner.execute(() -> application.statusBar.setProgress(progress));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        window.requireVisible();
        window.progressBar().requireValue(1234).requireText(SwingUtil.formatPercentage(12.34, 2));

        window.cleanUp();
        application.dispose();
    }

    @Test
    public void testProgressFormatter() {
        assertEquals(GuiMessages.get("SyncFinished"),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 100L)));
        assertEquals(SwingUtil.formatPercentage(12.34, 2),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(1234L, 10000L)));
        assertEquals(SwingUtil.formatPercentage(0),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0L, 10000L)));
        assertEquals(GuiMessages.get("SyncStopped"),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(100L, 0L)));
        assertEquals(GuiMessages.get("SyncStopped"), StatusBar.SyncProgressFormatter.format(null));
    }

    @Override
    protected void onSetUp() {

    }
}
