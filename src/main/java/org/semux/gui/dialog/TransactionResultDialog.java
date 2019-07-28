/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.util.stream.Collectors;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.ethereum.vm.LogInfo;
import org.ethereum.vm.util.HashUtil;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Hex;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;
import org.semux.vm.client.SemuxInternalTransaction;

public class TransactionResultDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public TransactionResultDialog(JFrame parent, Transaction tx, TransactionResult result) {
        super(null, GuiMessages.get("TransactionResult"), ModalityType.MODELESS);
        setName("TransactionResultDialog");

        String notAvailable = GuiMessages.get("NotAvailable");

        JLabel lblBlockNumber = new JLabel(GuiMessages.get("BlockNumber"));
        JLabel lblCode = new JLabel(GuiMessages.get("Code"));
        JLabel lblGas = new JLabel(GuiMessages.get("Gas"));
        JLabel lblGasUsed = new JLabel(GuiMessages.get("GasUsed"));
        JLabel lblGasPrice = new JLabel(GuiMessages.get("GasPrice"));
        JLabel lblReturnData = new JLabel(GuiMessages.get("ReturnData"));
        JLabel lblLogs = new JLabel(GuiMessages.get("Logs"));
        JLabel lblInternalTransactions = new JLabel(GuiMessages.get("InternalTransactions"));
        JLabel lblContractAddress = new JLabel(GuiMessages.get("ContractAddress"));

        JLabel blockNumber = new JLabel(SwingUtil.formatNumber(result.getBlockNumber()));
        blockNumber.setName("blockNumber");

        JLabel code = new JLabel(result.getCode().name());
        code.setName("code");

        JLabel gas = new JLabel(SwingUtil.formatNumber(result.getGas()));
        gas.setName("gas");
        JLabel gasUsed = new JLabel(SwingUtil.formatNumber(result.getGasUsed()));
        gasUsed.setName("gasUsed");
        JLabel gasPrice = new JLabel(SwingUtil.formatAmount(result.getGasPrice()));
        gasPrice.setName("gasPrice");

        JTextArea returnData = SwingUtil.textAreaWithCopyPopup(Hex.encode0x(result.getReturnData()));
        returnData.setName("returnData");
        returnData.setLineWrap(true);
        JScrollPane returnDataPane = new JScrollPane(returnData);

        // TODO: replace with JSON for logs and internal transactions
        JTextArea logs = SwingUtil.textAreaWithCopyPopup(
                result.getLogs().stream().map(LogInfo::toString).collect(Collectors.joining("\n")));
        logs.setName("logs");
        logs.setLineWrap(true);
        JScrollPane logsPane = new JScrollPane(logs);

        JTextArea internalTransactions = SwingUtil.textAreaWithCopyPopup(
                result.getInternalTransactions().stream().map(SemuxInternalTransaction::toString)
                        .collect(Collectors.joining("\n")));
        internalTransactions.setName("internalTransactions");
        internalTransactions.setLineWrap(true);
        JScrollPane internalTransactionsPane = new JScrollPane(internalTransactions);

        JTextArea contractAddress = SwingUtil.textAreaWithCopyPopup(notAvailable);
        contractAddress.setName("contractAddress");
        if (tx.getType() == TransactionType.CREATE && result.getCode().isSuccess()) {
            contractAddress.setText(Hex.encode0x(HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce())));
        }

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(42)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblContractAddress)
                        .addComponent(lblInternalTransactions)
                        .addComponent(lblLogs)
                        .addComponent(lblReturnData)
                        .addComponent(lblGasPrice)
                        .addComponent(lblGasUsed)
                        .addComponent(lblGas)
                        .addComponent(lblCode)
                        .addComponent(lblBlockNumber))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(blockNumber)
                        .addComponent(code)
                        .addComponent(gas)
                        .addComponent(gasUsed)
                        .addComponent(gasPrice)
                        .addComponent(returnDataPane, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                        .addComponent(logsPane, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                        .addComponent(internalTransactionsPane, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                        .addComponent(contractAddress))
                    .addContainerGap(19, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblBlockNumber)
                        .addComponent(blockNumber))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblCode)
                        .addComponent(code))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblGas)
                        .addComponent(gas))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblGasUsed)
                        .addComponent(gasUsed))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblGasPrice)
                        .addComponent(gasPrice))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblReturnData)
                        .addComponent(returnDataPane, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblLogs)
                        .addComponent(logsPane, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblInternalTransactions)
                        .addComponent(internalTransactionsPane, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblContractAddress)
                        .addComponent(contractAddress))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setTitle(GuiMessages.get("TransactionResult"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
