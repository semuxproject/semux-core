/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.mockito.Mockito.when;
import static org.semux.core.Amount.Unit.SEM;

import java.util.Collections;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.state.Delegate;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletDelegate;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;

@RunWith(MockitoJUnitRunner.class)
public class DelegateDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testDisplayVotes() {
        Delegate delegate = new Delegate(new Key().toAddress(), Bytes.of("delegate1"), 0L,
                SEM.of(123));
        WalletDelegate walletDelegate = new WalletDelegate(delegate);
        when(walletModel.getDelegates()).thenReturn(Collections.singletonList(walletDelegate));

        kernelRule1.getKernel().start();

        DelegateDialogTestApplication application = GuiActionRunner
                .execute(() -> new DelegateDialogTestApplication(walletModel, kernelRule1.getKernel()));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront()
                .dialog("DelegateDialog").requireVisible()
                .label("votes").requireText("123");
    }

    @Override
    protected void onSetUp() {

    }
}
