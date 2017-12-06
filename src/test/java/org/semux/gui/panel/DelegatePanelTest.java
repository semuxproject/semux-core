/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.Test;
import org.semux.gui.model.WalletDelegate;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;

public class DelegatePanelTest {

    @Test
    public void testSelectDelegate() throws InterruptedException {
        WalletModel walletModel = mock(WalletModel.class);
        List<WalletDelegate> walletDelegates = new ArrayList<>();

        WalletDelegate delegate1 = mock(WalletDelegate.class, "delegate 1");
        when(delegate1.getNameString()).thenReturn("delegate 1");
        when(delegate1.getAddressString()).thenReturn("1111");
        walletDelegates.add(delegate1);

        WalletDelegate delegate2 = mock(WalletDelegate.class, "delegate 2");
        when(delegate2.getNameString()).thenReturn("delegate 2");
        when(delegate2.getAddressString()).thenReturn("2222");
        walletDelegates.add(delegate2);

        when(walletModel.getDelegates()).thenReturn(walletDelegates);
        DelegatePanelTestApplication application = GuiActionRunner
                .execute(() -> new DelegatePanelTestApplication(walletModel));
        FrameFixture window = new FrameFixture(application);
        window.show();

        // the initial label of selected delegate should be PleaseSelectDelegate
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("PleaseSelectDelegate"));

        // click on the first delegate
        window.table("DelegatesTable").cell("delegate 1").click();

        // the label of selected delegate should display the first delegate's name
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("SelectedDelegate", "delegate 1"));

        // click on the second delegate
        window.table("DelegatesTable").cell("delegate 2").click();

        // the label of selected delegate should display the second delegate's name
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("SelectedDelegate", "delegate 2"));

        // clean up
        window.cleanUp();
    }

}
