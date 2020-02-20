/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import org.semux.KernelMock;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

public class TransactionResultDialogTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    TransactionResultDialog dialog;

    TransactionResultDialogTestApplication(WalletModel walletModel, KernelMock kernelMock,
            Transaction tx, TransactionResult result) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        dialog = new TransactionResultDialog(this, tx, result);
        dialog.setVisible(true);
    }
}
