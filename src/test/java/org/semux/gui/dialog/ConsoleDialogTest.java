/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsoleDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testBasicUse() throws InterruptedException {

        ConsoleDialogTestApplication application = GuiActionRunner
                .execute(() -> new ConsoleDialogTestApplication(walletModel, kernelRule1.getKernel()));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();
        DialogFixture console = window.dialog("Console").requireVisible();
        JTextComponentFixture consoleText = console.textBox("txtConsole");

        // check txtInput
        console.textBox("txtInput").click().requireFocused().requireEditable().requireVisible();

        // help
        console.textBox("txtInput").enterText("help\n");
        await().until(() -> consoleText.text().contains("transfer"));

        // listAccounts
        console.textBox("txtInput").enterText("listAccounts\n");
        kernelRule1.getKernel().getWallet().getAccount(0).toAddressString();

        // getBlockByNumber
        Blockchain blockChain = mock(Blockchain.class);
        Block block = getBlock();
        when(blockChain.getBlock(anyLong())).thenReturn(block);
        kernelRule1.getKernel().setBlockchain(blockChain);

        console.textBox("txtInput").enterText("getBlockByNumber 1\n");

        await().until(() -> consoleText.text().contains("transactionsRoot"));
    }

    private Block getBlock() {

        long number = 1;
        byte[] coinbase = Bytes.random(20);
        byte[] prevHash = Bytes.random(20);
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] transactionsRoot = Bytes.random(32);
        byte[] resultsRoot = Bytes.random(32);
        byte[] stateRoot = Bytes.random(32);
        byte[] data = {};
        List<Transaction> transactions = Collections.emptyList();
        List<TransactionResult> results = Arrays.asList(new TransactionResult(), new TransactionResult());
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header, transactions, results);
    }

    @Override
    protected void onSetUp() {

    }
}
