package org.semux.gui.dialog;

import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.core.Delegate;
import org.semux.core.Unit;
import org.semux.crypto.Hex;

public class DelegateDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JTextArea selectableText(String txt) {
        JTextArea c = new JTextArea(txt);
        c.setBackground(null);
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        c.setEditable(false);

        return c;
    }

    public DelegateDialog(JComponent parent, Delegate d) {

        JLabel lblName = new JLabel("Name:");
        JLabel lblAddress = new JLabel("Address:");
        JLabel lblTotalVotes = new JLabel("Total Votes:");
        JLabel lblVotesFromMe = new JLabel("Votes from Me:");
        JLabel lblRegisteredAt = new JLabel("Registered At:");
        JLabel lblNumOfBlocksForged = new JLabel("# of Blocks Forged:");
        JLabel lblNumOfBlocksMissed = new JLabel("# of Blocks Missed:");
        JLabel lblRate = new JLabel("Rate:");

        JTextArea name = selectableText(d.getNameString());
        JTextArea address = selectableText(Hex.PREF + Hex.encode(d.getAddress()));
        JLabel totalVotes = new JLabel(Long.toString(d.getVotes() / Unit.SEM));
        JLabel votesFromMe = new JLabel("0");
        JLabel registeredAt = new JLabel("Unavailable");
        JLabel numOfBlocksForged = new JLabel("Unavailable");
        JLabel numOfBlocksMissed = new JLabel("Unavailable");
        JLabel rate = new JLabel("Unavailable");

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblRate)
                        .addComponent(lblAddress)
                        .addComponent(lblName)
                        .addComponent(lblTotalVotes)
                        .addComponent(lblRegisteredAt)
                        .addComponent(lblVotesFromMe)
                        .addComponent(lblNumOfBlocksMissed)
                        .addComponent(lblNumOfBlocksForged))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(name)
                        .addComponent(address)
                        .addComponent(totalVotes)
                        .addComponent(votesFromMe)
                        .addComponent(registeredAt)
                        .addComponent(numOfBlocksForged)
                        .addComponent(numOfBlocksMissed)
                        .addComponent(rate))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblName)
                        .addComponent(name, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(address, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(12)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTotalVotes)
                        .addComponent(totalVotes))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblVotesFromMe)
                        .addComponent(votesFromMe))
                    .addGap(12)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRegisteredAt)
                        .addComponent(registeredAt))
                    .addGap(12)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNumOfBlocksForged)
                        .addComponent(numOfBlocksForged))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNumOfBlocksMissed)
                        .addComponent(numOfBlocksMissed))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRate)
                        .addComponent(rate))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setTitle("Delegate");
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
