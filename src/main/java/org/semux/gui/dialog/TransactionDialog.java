/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.core.Transaction;
import org.semux.crypto.Hex;
import org.semux.gui.SwingUtil;
import org.semux.message.GUIMessages;

public class TransactionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public TransactionDialog(JFrame parent, Transaction tx) {

        JLabel lblHash = new JLabel(GUIMessages.get("Hash") + ":");
        JLabel lblType = new JLabel(GUIMessages.get("Type") + ":");
        JLabel lblFrom = new JLabel(GUIMessages.get("From") + ":");
        JLabel lblTo = new JLabel(GUIMessages.get("To") + ":");
        JLabel lblValue = new JLabel(GUIMessages.get("Value") + ":");
        JLabel lblFee = new JLabel(GUIMessages.get("Fee") + ":");
        JLabel lblNonce = new JLabel(GUIMessages.get("Nonce") + ":");
        JLabel lblTimestamp = new JLabel(GUIMessages.get("Timestamp") + ":");
        JLabel lblData = new JLabel(GUIMessages.get("Data") + ":");

        JTextArea hash = SwingUtil.textAreaWithCopyPastePopup(Hex.encode0x(tx.getHash()));
        JLabel type = new JLabel(tx.getType().name());
        JTextArea from = SwingUtil.textAreaWithCopyPastePopup(Hex.encode0x(tx.getFrom()));
        JTextArea to = SwingUtil.textAreaWithCopyPastePopup(Arrays.stream(tx.getRecipients())
                .map(Hex::encode0x)
                .collect(Collectors.joining(", ")));
        JLabel value = new JLabel(SwingUtil.formatValue((tx.getValue())));
        JLabel fee = new JLabel(SwingUtil.formatValue((tx.getFee())));
        JLabel nonce = new JLabel(SwingUtil.formatNumber(tx.getNonce()));
        JLabel timestamp = new JLabel(SwingUtil.formatTimestamp(tx.getTimestamp()));
        JTextArea data = new JTextArea(Hex.encode0x(tx.getData()));
        data.setEditable(false);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(42)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblData)
                        .addComponent(lblTimestamp)
                        .addComponent(lblNonce)
                        .addComponent(lblFee)
                        .addComponent(lblValue)
                        .addComponent(lblTo)
                        .addComponent(lblFrom)
                        .addComponent(lblType)
                        .addComponent(lblHash))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(hash)
                        .addComponent(type)
                        .addComponent(from)
                        .addComponent(to)
                        .addComponent(value)
                        .addComponent(fee)
                        .addComponent(nonce)
                        .addComponent(timestamp)
                        .addComponent(data, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(19, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblHash)
                        .addComponent(hash))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblType)
                        .addComponent(type))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFrom)
                        .addComponent(from))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(to))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblValue)
                        .addComponent(value))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(fee))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNonce)
                        .addComponent(nonce))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTimestamp)
                        .addComponent(timestamp))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(data, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setTitle(GUIMessages.get("Transaction"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
