/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.message.GuiMessages;

public class SplashScreenTest extends AssertJSwingJUnitTestCase {

    @Test
    public void testEvents() {
        SplashScreenTestApplication application = GuiActionRunner.execute(SplashScreenTestApplication::new);

        FrameFixture window = new FrameFixture(robot(), application.splashScreen);
        window.progressBar().requireVisible().requireText(GuiMessages.get("SplashLoading"));

        application.walletModel.fireSemuxEvent(SemuxEvent.WALLET_LOADING);
        window.progressBar().requireVisible().requireText(GuiMessages.get("SplashLoadingWallet"));

        application.walletModel.fireSemuxEvent(SemuxEvent.KERNEL_STARTING);
        window.progressBar().requireVisible().requireText(GuiMessages.get("SplashStartingKernel"));

        // the splash screen should be disposed as soon as the mainframe starts
        application.walletModel.fireSemuxEvent(SemuxEvent.GUI_MAINFRAME_STARTED);
        assertThatExceptionOfType(ComponentLookupException.class).isThrownBy(window::progressBar);
    }

    @Override
    protected void onSetUp() {

    }
}
