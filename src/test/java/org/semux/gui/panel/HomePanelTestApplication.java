/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

public class HomePanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    HomePanel homePanel;

    HomePanelTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        SemuxGui gui = new SemuxGui(walletModel, kernelMock);
        homePanel = new HomePanel(gui);
        getContentPane().add(homePanel);
    }
}