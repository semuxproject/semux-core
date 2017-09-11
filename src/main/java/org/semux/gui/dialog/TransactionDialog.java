package org.semux.gui.dialog;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

public class TransactionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public TransactionDialog() {
        JLabel lblHash = new JLabel("Hash:");

        JLabel lblType = new JLabel("Type:");

        JLabel lblFrom = new JLabel("From:");

        JLabel lblTo = new JLabel("To:");

        JLabel lblValue = new JLabel("Value:");

        JLabel lblFee = new JLabel("Fee:");

        JLabel lblNonce = new JLabel("Nonce:");

        JLabel lblTimestamp = new JLabel("Timestamp:");

        JLabel lblData = new JLabel("data:");

        JLabel lblBlockNum = new JLabel("Block #:");

        JLabel blockNum = new JLabel("");

        JLabel hash = new JLabel("");

        JLabel type = new JLabel("");

        JLabel from = new JLabel("");

        JLabel to = new JLabel("");

        JLabel value = new JLabel("");

        JLabel fee = new JLabel("");

        JLabel nonce = new JLabel("");

        JLabel timestamp = new JLabel("");

        JTextArea data = new JTextArea();

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout
                .setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup().addGap(42)
                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addComponent(lblData)
                                        .addComponent(lblTimestamp).addComponent(lblNonce).addComponent(lblFee)
                                        .addComponent(lblValue).addComponent(lblTo).addComponent(lblFrom)
                                        .addComponent(lblType).addComponent(lblHash).addComponent(lblBlockNum))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(blockNum)
                                        .addComponent(hash).addComponent(type).addComponent(from).addComponent(to)
                                        .addComponent(value).addComponent(fee).addComponent(nonce)
                                        .addComponent(timestamp).addComponent(data, GroupLayout.PREFERRED_SIZE, 447,
                                                GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(19, Short.MAX_VALUE)));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
                .createSequentialGroup().addGap(15)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblBlockNum)
                        .addComponent(blockNum))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblHash).addComponent(hash))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblType).addComponent(type))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblFrom).addComponent(from))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblTo).addComponent(to))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(
                        groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblValue).addComponent(value))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblFee).addComponent(fee))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(
                        groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblNonce).addComponent(nonce))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout
                        .createParallelGroup(Alignment.BASELINE).addComponent(lblTimestamp).addComponent(timestamp))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblData)
                        .addComponent(data, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE))
                .addContainerGap(99, Short.MAX_VALUE)));
        getContentPane().setLayout(groupLayout);
        // @formatter:on
    }
}
