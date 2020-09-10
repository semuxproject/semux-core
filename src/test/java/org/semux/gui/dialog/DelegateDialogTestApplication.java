/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

public class DelegateDialogTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    DelegateDialog delegateDialog;

    DelegateDialogTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        delegateDialog = new DelegateDialog(gui, this, walletModel.getDelegates().get(0));
        delegateDialog.setVisible(true);
    }
}
