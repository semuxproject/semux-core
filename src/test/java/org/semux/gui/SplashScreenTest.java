/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.awaitility.Awaitility.await;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.core.event.WalletLoadingEvent;
import org.semux.event.KernelBootingEvent;
import org.semux.event.PubSub;
import org.semux.gui.event.MainFrameStartedEvent;
import org.semux.gui.event.WalletSelectionDialogShownEvent;
import org.semux.message.GuiMessages;

public class SplashScreenTest extends AssertJSwingJUnitTestCase {

    @Override
    protected void onSetUp() {

    }

    @Test
    public void testEvents() {
        SplashScreenTestApplication application = GuiActionRunner
                .execute(SplashScreenTestApplication::new);

        FrameFixture window = new FrameFixture(robot(), application.splashScreen);
        window.requireVisible().progressBar().requireVisible().requireText(GuiMessages.get("SplashLoading"));

        PubSub.getInstance().publish(new WalletLoadingEvent());
        await().until(
                () -> window.requireVisible().progressBar().text().equals(GuiMessages.get("SplashLoadingWallet")));

        PubSub.getInstance().publish(new WalletSelectionDialogShownEvent());
        await().until(() -> !window.target().isVisible());

        PubSub.getInstance().publish(new KernelBootingEvent());
        await().until(
                () -> window.requireVisible().progressBar().text().equals(GuiMessages.get("SplashStartingKernel")));

        // the splash screen should be disposed as soon as the mainframe starts
        PubSub.getInstance().publish(new MainFrameStartedEvent());
        await().until(() -> !window.target().isVisible());
    }
}
