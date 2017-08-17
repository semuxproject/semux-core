package org.semux.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class HomePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private String blockNumber = "1";
    private String status = "Normal";
    private String balance = "0 SEM";
    private String locked = "0 SEM";

    public HomePanel() {
        // setup overview panel
        JPanel overview = new JPanel();
        overview.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Overview", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        overview.setLayout(new GridLayout(4, 2, 0, 0));

        JLabel labelBlockNum = new JLabel("Block #:");
        overview.add(labelBlockNum);

        JLabel valueBlockNum = new JLabel(blockNumber);
        overview.add(valueBlockNum);

        JLabel labelStatus = new JLabel("Status:");
        overview.add(labelStatus);

        JLabel valueStatus = new JLabel(status);
        overview.add(valueStatus);

        JLabel labelBalance = new JLabel("Balance:");
        overview.add(labelBalance);

        JLabel valueBalance = new JLabel(balance);
        overview.add(valueBalance);

        JLabel labelLocked = new JLabel("Locked:");
        overview.add(labelLocked);

        JLabel valueLocked = new JLabel(locked);
        overview.add(valueLocked);

        // setup transactions panel
        JPanel transactions = new JPanel();
        transactions.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Transactions", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(overview, GroupLayout.PREFERRED_SIZE, 252, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(overview, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE)
                        .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                    .addGap(0))
        );
        // @formatter:on
        setLayout(groupLayout);
    }

    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getLocked() {
        return locked;
    }

    public void setLocked(String locked) {
        this.locked = locked;
    }
}
