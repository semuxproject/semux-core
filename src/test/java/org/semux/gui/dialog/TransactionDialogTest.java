/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.time.Instant;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.DialogFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class TransactionDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testDisplayTransferTransaction() {
        kernelRule1.getKernel().start();

        final EdDSA from = new EdDSA();
        final EdDSA to = new EdDSA();
        final long value = 1000 * Unit.SEM;
        final long fee = (long) (0.05 * Unit.SEM);
        final long now = Instant.now().toEpochMilli();
        final byte[] data = new String("some data").getBytes();
        Transaction tx = new Transaction(
                TransactionType.TRANSFER,
                to.toAddress(),
                value,
                fee,
                0,
                now,
                data).sign(from);

        TransactionDialogTestApplication application = GuiActionRunner
                .execute(() -> new TransactionDialogTestApplication(walletModel, tx, kernelRule1.getKernel()));

        FrameFixture window = new FrameFixture(robot(), application);
        DialogFixture dialog = window.show().requireVisible().moveToFront()
                .dialog("TransactionDialog").requireVisible();

        dialog.textBox("hashText").requireVisible().requireText(Hex.encode0x(tx.getHash()));
        dialog.textBox("fromText").requireVisible().requireText(Hex.encode0x(from.toAddress()));
        dialog.textBox("toText").requireVisible().requireText(Hex.encode0x(to.toAddress()));
        dialog.label("valueText").requireVisible().requireText(SwingUtil.formatValue(value));
        dialog.label("feeText").requireVisible().requireText(SwingUtil.formatValue(fee));
        dialog.label("nonceText").requireVisible().requireText("0");
        dialog.label("timestampText").requireVisible().requireText(SwingUtil.formatTimestamp(now));
        dialog.textBox("dataText").requireVisible().requireText(Hex.encode0x(data));
    }

    @Override
    protected void onSetUp() {

    }
}
