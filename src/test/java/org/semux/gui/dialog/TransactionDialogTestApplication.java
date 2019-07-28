/**
 * Copyright (c) 2017-2018 The Semux Developers
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

public class TransactionDialogTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    TransactionDialog transactionDialog;

    TransactionDialogTestApplication(WalletModel walletModel, KernelMock kernelMock,
            Transaction tx) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        TransactionResult result = new TransactionResult(TransactionResult.Code.SUCCESS);
        transactionDialog = new TransactionDialog(this, tx, gui.getKernel());
        transactionDialog.setVisible(true);
    }
}
