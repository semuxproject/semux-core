/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.semux.core.TransactionType.CREATE;
import static org.semux.core.Unit.NANO_SEM;
import static org.semux.core.Unit.SEM;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.ethereum.vm.LogInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionResultDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testTransactionResult() {
        kernelRule1.getKernel().start();

        Key from = new Key();
        Key to = new Key();
        Amount value = SEM.of(1000);
        Amount fee = SEM.fromDecimal(new BigDecimal("0.05"));
        long nonce = 0L;
        long now = Instant.now().toEpochMilli();
        byte[] data = "some data".getBytes();
        long gas = 10_000;
        Amount gasPrice = NANO_SEM.of(10);
        Transaction tx = new Transaction(kernelRule1.getKernel().getConfig().network(), CREATE, to.toAddress(), value,
                fee, nonce, now, data, gas, gasPrice).sign(from);

        TransactionResult result = new TransactionResult();
        result.setCode(TransactionResult.Code.FAILURE);
        result.setReturnData("Test".getBytes());
        result.setGas(gas, gasPrice, 5_000);
        LogInfo log = new LogInfo(Bytes.random(20), Collections.emptyList(), "log".getBytes());
        result.setLogs(Collections.singletonList(log));
        result.setInternalTransactions(Collections.emptyList());

        TransactionResultDialogTestApplication application = GuiActionRunner
                .execute(() -> new TransactionResultDialogTestApplication(walletModel, kernelRule1.getKernel(), tx,
                        result));

        FrameFixture window = new FrameFixture(robot(), application);
        DialogFixture dialog = window.show().requireVisible().moveToFront()
                .dialog("TransactionResultDialog").requireVisible();

        dialog.label("blockNumber").requireVisible().requireText(String.valueOf(result.getBlockNumber()));
    }

    @Override
    protected void onSetUp() {

    }
}
