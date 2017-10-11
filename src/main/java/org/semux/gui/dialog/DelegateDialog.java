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

import org.semux.Kernel;
import org.semux.core.Blockchain;
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
        Blockchain chain = Kernel.getInstance().getBlockchain();
        long forged = chain.getNumberOfBlocksForged(d.getAddress());
        long missed = chain.getNumberOfBlocksMissed(d.getAddress());
        long total = forged + missed;

        JLabel lblName = new JLabel("Name:");
        JLabel lblAddress = new JLabel("Address:");
        JLabel lblRegisteredAt = new JLabel("Registered At:");
        JLabel lblVotes = new JLabel("Votes:");
        JLabel lblVotesFromMe = new JLabel("Votes from Me:");
        JLabel lblNumOfBlocksForged = new JLabel("# of Blocks Forged:");
        JLabel lblNumOfBlocksMissed = new JLabel("# of Blocks Missed:");
        JLabel lblRate = new JLabel("Rate:");

        JTextArea name = selectableText(d.getNameString());
        JTextArea address = selectableText(Hex.PREF + Hex.encode(d.getAddress()));
        JLabel registeredAt = new JLabel(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(d.getRegisteredAt())));
        JLabel votes = new JLabel(Long.toString(d.getVotes() / Unit.SEM));
        JLabel votesFromMe = new JLabel(Long.toString(d.getVotesFromMe() / Unit.SEM));
        JLabel numOfBlocksForged = new JLabel(Long.toString(forged));
        JLabel numOfBlocksMissed = new JLabel(Long.toString(missed));
        JLabel rate = new JLabel(total == 0 ? "N/A" : String.format("%.2f%%", forged * 100.0 / total));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblRate)
                        .addComponent(lblNumOfBlocksMissed)
                        .addComponent(lblNumOfBlocksForged)
                        .addComponent(lblVotesFromMe)
                        .addComponent(lblVotes)
                        .addComponent(lblRegisteredAt)
                        .addComponent(lblAddress)
                        .addComponent(lblName))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(name)
                        .addComponent(address)
                        .addComponent(votes)
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
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRegisteredAt)
                        .addComponent(registeredAt))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblVotes)
                        .addComponent(votes))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblVotesFromMe)
                        .addComponent(votesFromMe))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
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
