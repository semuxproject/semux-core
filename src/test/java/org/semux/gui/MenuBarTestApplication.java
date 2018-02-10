/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;

import org.semux.KernelMock;
import org.semux.gui.model.WalletModel;

public class MenuBarTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    MenuBar menuBar;

    MenuBarTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        menuBar = new MenuBar(gui, this);
        this.setJMenuBar(menuBar);

        this.setMinimumSize(new Dimension(600, 400));
    }
}