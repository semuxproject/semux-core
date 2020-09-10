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

public class TransactionsPanelTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    TransactionsPanel transactionsPanel;

    TransactionsPanelTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        SemuxGui gui = new SemuxGui(walletModel, kernelMock);
        transactionsPanel = new TransactionsPanel(gui, this);
        getContentPane().add(transactionsPanel);
    }
}