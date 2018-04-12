/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.core.event.BlockchainDatabaseUpgradingEvent;
import org.semux.core.event.WalletLoadingEvent;
import org.semux.event.KernelBootingEvent;
import org.semux.event.PubSub;
import org.semux.event.PubSubFactory;
import org.semux.gui.event.MainFrameStartedEvent;
import org.semux.gui.event.WalletSelectionDialogShownEvent;
import org.semux.message.GuiMessages;

public class SplashScreenTest extends AssertJSwingJUnitTestCase {

    private static final PubSub pubSub = PubSubFactory.getDefault();

    @Override
    protected void onSetUp() {
        pubSub.start();
    }

    @Override
    protected void onTearDown() {
        pubSub.stop();
    }

    @Test
    public void testEvents() {
        SplashScreenTestApplication application = GuiActionRunner
                .execute(SplashScreenTestApplication::new);

        FrameFixture window = new FrameFixture(robot(), application.splashScreen);
        window.requireVisible().progressBar().requireVisible().requireText(GuiMessages.get("SplashLoading"));

        // WalletLoadingEvent
        pubSub.publish(new WalletLoadingEvent());
        await().until(
                () -> window.requireVisible().progressBar().text().equals(GuiMessages.get("SplashLoadingWallet")));

        // WalletSelectionDialogShownEvent
        pubSub.publish(new WalletSelectionDialogShownEvent());
        await().until(() -> !window.target().isVisible());

        // KernelBootingEvent
        pubSub.publish(new KernelBootingEvent());
        await().until(
                () -> window.progressBar().text().equals(GuiMessages.get("SplashStartingKernel")));

        // BlockchainDatabaseUpgradingEvent
        pubSub.publish(new BlockchainDatabaseUpgradingEvent(50L, 100L));
        window.progressBar().waitUntilIsDeterminate();
        window.requireVisible().progressBar().requireText(GuiMessages.get("SplashUpgradingDatabase", 50, 100));
        assertEquals(50 / 100, window.progressBar().target().getValue() / window.progressBar().target().getMaximum());

        // the splash screen should be disposed as soon as the mainframe starts
        pubSub.publish(new MainFrameStartedEvent());
        await().until(() -> !window.target().isVisible());
    }
}
