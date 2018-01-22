/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

public class DelegatesPanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    DelegatesPanel delegatesPanel;

    DelegatesPanelTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        delegatesPanel = new DelegatesPanel(gui, this);
        getContentPane().add(delegatesPanel);
    }
}