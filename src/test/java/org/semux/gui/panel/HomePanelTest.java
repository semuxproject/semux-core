/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.state.Delegate;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletDelegate;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.rules.KernelRule;
import org.semux.util.TimeUtil;

@RunWith(MockitoJUnitRunner.class)
public class HomePanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    HomePanelTestApplication application;

    FrameFixture window;

    @Override
    protected void onSetUp() {
        Block latestBlock = mock(Block.class);
        when(latestBlock.getNumber()).thenReturn(198L);
        when(latestBlock.getTimestamp()).thenReturn(1527206400000L);
        when(walletModel.getLatestBlock()).thenReturn(latestBlock);

        Key coinbase = new Key();
        when(walletModel.getCoinbase()).thenReturn(coinbase);

        when(walletModel.getStatus()).thenReturn(WalletModel.Status.NORMAL);
        when(walletModel.getTotalAvailable()).thenReturn(Amount.ZERO);
        when(walletModel.getTotalLocked()).thenReturn(Amount.ZERO);

        when(walletModel.getAccounts()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testConsensusTable() {
        // mock wallet model
        WalletDelegate primaryValidator = new WalletDelegate(
                new Delegate(new Key().toAddress(), "primary".getBytes(), 0, Amount.ZERO));
        when(walletModel.getPrimaryValidator()).thenReturn(Optional.of(primaryValidator));
        WalletDelegate nextPrimaryValidator = new WalletDelegate(
                new Delegate(new Key().toAddress(), "next".getBytes(), 0, Amount.ZERO));
        when(walletModel.getNextPrimaryValidator()).thenReturn(Optional.of(nextPrimaryValidator));
        when(walletModel.getNextValidatorSetUpdate()).thenReturn(Optional.of(200L));

        // mock kernel
        KernelMock kernelMock = spy(kernelRule1.getKernel());
        application = GuiActionRunner.execute(() -> new HomePanelTestApplication(walletModel, kernelMock));

        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();
        window.label("primaryValidator").requireVisible().requireText("primary");
        window.label("nextPrimaryValidator").requireVisible().requireText("next");
        window.label("nextValidatorSetUpdate").requireVisible().requireText(GuiMessages.get(
                "NextValidatorSetUpdateTime",
                200,
                TimeUtil.formatTimestamp(1527206400000L + 30L * 1000L)));
    }
}
