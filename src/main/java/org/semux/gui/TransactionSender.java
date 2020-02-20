/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.core.Amount;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.gui.model.WalletAccount;
import org.semux.util.TimeUtil;

public class TransactionSender {

    public static PendingManager.ProcessingResult send(Kernel kernel, WalletAccount account, TransactionType type,
            byte[] to, Amount value, Amount fee, byte[] data) {
        return send(kernel, account, type, to, value, fee, data, 0, Amount.ZERO);
    }

    public static PendingManager.ProcessingResult send(Kernel kernel, WalletAccount account, TransactionType type,
            byte[] to, Amount value, Amount fee, byte[] data, long gas, Amount gasPrice) {
        PendingManager pendingMgr = kernel.getPendingManager();

        Network network = kernel.getConfig().network();
        byte[] from = account.getKey().toAddress();
        long nonce = pendingMgr.getNonce(from);
        long timestamp = TimeUtil.currentTimeMillis();
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data, gas, gasPrice);
        tx.sign(account.getKey());

        return pendingMgr.addTransactionSync(tx);
    }
}
