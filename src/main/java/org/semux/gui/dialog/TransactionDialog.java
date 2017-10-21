package org.semux.gui.dialog;

import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.core.Transaction;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.gui.MessagesUtil;

public class TransactionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JTextArea selectableText(String txt) {
        JTextArea c = new JTextArea(txt);
        c.setBackground(null);
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        c.setEditable(false);

        return c;
    }

    public TransactionDialog(JComponent parent, Transaction tx) {
        JLabel lblHash = new JLabel(MessagesUtil.get("Hash") + ":");
        JLabel lblType = new JLabel(MessagesUtil.get("Type") + ":");
        JLabel lblFrom = new JLabel(MessagesUtil.get("From") + ":");
        JLabel lblTo = new JLabel(MessagesUtil.get("To") + ":");
        JLabel lblValue = new JLabel(MessagesUtil.get("Value") + ":");
        JLabel lblFee = new JLabel(MessagesUtil.get("Fee") + ":");
        JLabel lblNonce = new JLabel(MessagesUtil.get("Nonce") + ":");
        JLabel lblTimestamp = new JLabel(MessagesUtil.get("Timestamp") + ":");
        JLabel lblData = new JLabel(MessagesUtil.get("Data") + ":");

        JTextArea hash = selectableText(Hex.PREF + Hex.encode(tx.getHash()));
        JLabel type = new JLabel(tx.getType().name());
        JTextArea from = selectableText(Hex.PREF + Hex.encode(tx.getFrom()));
        JTextArea to = selectableText(Hex.PREF + Hex.encode(tx.getTo()));
        JLabel value = new JLabel(tx.getValue() / Unit.SEM + " SEM");
        JLabel fee = new JLabel(tx.getFee() / Unit.SEM + " SEM");
        JLabel nonce = new JLabel(Long.toString(tx.getNonce()));
        JLabel timestamp = new JLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(tx.getTimestamp())));
        JTextArea data = new JTextArea(Hex.PREF + Hex.encode(tx.getData()));
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

        this.setTitle(MessagesUtil.get("Transaction"));
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
