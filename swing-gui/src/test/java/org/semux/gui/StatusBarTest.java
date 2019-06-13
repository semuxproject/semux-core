/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.consensus.SemuxSync;
import org.semux.gui.message.GuiMessages;

public class StatusBarTest extends AssertJSwingJUnitTestCase {

    @Test
    public void testSyncProgress1() {
        testStatusBar(1234L, 10000L, Duration.ofSeconds(370200), 1234,
                "12.34 % (>= 1 month)");
    }

    @Test
    public void testSyncProgress2() {
        testStatusBar(1234L, 10000L, Duration.ofSeconds(300000), 1234,
                "12.34 % (24 days 15 hours 58 minutes 31 seconds)");
    }

    @Test
    public void testSyncProgress100() {
        testStatusBar(100L, 100L, Duration.ZERO, 10000, GuiMessages.get("SyncFinished"));
    }

    private void testStatusBar(long currentHeight, long targetHeight, Duration estimation, int requireValue,
            String requireText) {
        // mock progress
        SemuxSync.SemuxSyncProgress progress = new SemuxSync.SemuxSyncProgress(0, currentHeight, targetHeight,
                estimation);

        StatusBarTestApplication application = GuiActionRunner.execute(StatusBarTestApplication::new);

        GuiActionRunner.execute(() -> application.statusBar.setProgress(progress));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        window.requireVisible();
        window.progressBar().requireValue(requireValue).requireText(requireText);

        window.cleanUp();
        application.dispose();
    }

    @Test
    public void testProgressFormatter() {
        assertEquals(GuiMessages.get("SyncFinished"),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0, 100L, 100L, Duration.ZERO)));
        assertEquals(SwingUtil.formatPercentage(12.34, 2),
                StatusBar.SyncProgressFormatter
                        .format(new SemuxSync.SemuxSyncProgress(0, 1234L, 10000L, Duration.ZERO)));
        assertEquals(SwingUtil.formatPercentage(0),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0, 0L, 10000L, Duration.ZERO)));
        assertEquals(GuiMessages.get("SyncStopped"),
                StatusBar.SyncProgressFormatter.format(new SemuxSync.SemuxSyncProgress(0, 100L, 0L, Duration.ZERO)));
        assertEquals(GuiMessages.get("SyncStopped"), StatusBar.SyncProgressFormatter.format(null));
    }

    @Override
    protected void onSetUp() {

    }
}
