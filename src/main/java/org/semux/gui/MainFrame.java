/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Semux Wallet";

    private String blockNumber = "1";
    private String status = "Normal";
    private String balance = "0 SEM";
    private String locked = "0 SEM";

    public MainFrame() {
        // setup frame properties
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtil.centerizeFrame(this, 800, 600);

        // add menu bar
        JMenuBar menuBar = new MenuBar();
        this.setJMenuBar(menuBar);

        // add tool bar
        JToolBar toolBar = new ToolBar(false);

        // add overview panel
        JPanel panelOverview = new JPanel();
        panelOverview.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Overview", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        panelOverview.setLayout(new GridLayout(4, 2, 0, 0));

        JLabel labelBlockNum = new JLabel("Block #:");
        panelOverview.add(labelBlockNum);

        JLabel valueBlockNum = new JLabel(blockNumber);
        panelOverview.add(valueBlockNum);

        JLabel labelStatus = new JLabel("Status:");
        panelOverview.add(labelStatus);

        JLabel valueStatus = new JLabel(status);
        panelOverview.add(valueStatus);

        JLabel labelBalance = new JLabel("Balance:");
        panelOverview.add(labelBalance);

        JLabel valueBalance = new JLabel(balance);
        panelOverview.add(valueBalance);

        JLabel labelLocked = new JLabel("Locked:");
        panelOverview.add(labelLocked);

        JLabel valueLocked = new JLabel(locked);
        panelOverview.add(valueLocked);

        // add transactions panel
        JPanel panelTransactions = new JPanel();
        panelTransactions.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Transactions", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this.getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 866, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                                    .addGap(4)
                                    .addComponent(panelOverview, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                    .addGap(15)
                                    .addComponent(panelTransactions, GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE))
                                .addComponent(toolBar, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 858, Short.MAX_VALUE))
                            .addGap(14))))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(12)
                    .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, 193, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(panelTransactions, GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                        .addComponent(panelOverview, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE))
                    .addGap(18))
        );
        
        JPanel panel = new JPanel();
        tabbedPane.addTab("New tab", null, panel, null);
        
        JPanel panel_1 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_1, null);
        
        JPanel panel_2 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_2, null);
        
        JPanel panel_3 = new JPanel();
        tabbedPane.addTab("New tab", null, panel_3, null);
        // @formatter:on

        this.getContentPane().setLayout(groupLayout);
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
