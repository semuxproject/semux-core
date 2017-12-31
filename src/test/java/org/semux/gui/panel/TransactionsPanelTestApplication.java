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

public class TransactionsPanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    TransactionsPanel transactionsPanel;

    TransactionsPanelTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        SemuxGUI gui = new SemuxGUI(walletModel, kernelMock);
        transactionsPanel = new TransactionsPanel(gui, this);
        getContentPane().add(transactionsPanel);
    }
}