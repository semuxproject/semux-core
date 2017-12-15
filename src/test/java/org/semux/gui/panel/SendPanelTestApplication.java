/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.mockito.Mockito.spy;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGUI;
import org.semux.gui.model.WalletModel;

public class SendPanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SendPanel sendPanel;

    KernelMock kernelMock;

    SendPanelTestApplication(WalletModel walletModel) {
        super();

        // mock kernel
        kernelMock = spy(new KernelMock());

        // create gui
        SemuxGUI gui = new SemuxGUI(walletModel, kernelMock);
        sendPanel = new SendPanel(gui, this);
        getContentPane().add(sendPanel);
    }
}