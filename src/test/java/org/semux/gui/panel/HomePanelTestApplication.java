/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGUI;
import org.semux.gui.model.WalletModel;

public class HomePanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    HomePanel homePanel;

    HomePanelTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        SemuxGUI gui = new SemuxGUI(walletModel, kernelMock);
        homePanel = new HomePanel(gui);
        getContentPane().add(homePanel);
    }
}