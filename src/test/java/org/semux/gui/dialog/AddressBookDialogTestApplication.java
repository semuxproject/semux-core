/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

public class AddressBookDialogTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    AddressBookDialog addressBookDialog;

    AddressBookDialogTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        addressBookDialog = new AddressBookDialog(this, walletModel);
        addressBookDialog.setVisible(true);
    }
}
