/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle.ComponentPlacement;

public class WelcomeFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Semux Wallet";
    private static final String DESCRIPTION = "<html>" + //
            "<h1>Welcome to semux!</h1>" + //
            "<p>Looks like this is your first time running this wallet.<p>" + //
            "<p>Do you want to create a new account, or import accounts from backup files?</p>" + //
            "</html>";
    private static final String OPTION_CREATE = "Create new account";
    private static final String OPTION_IMPORT = "Import acounts from backup file";

    public WelcomeFrame() {
        // setup frame properties
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtil.centerizeFrame(this, 600, 400);

        // create banner
        JLabel banner = new JLabel("");
        banner.setIcon(SwingUtil.loadImage("banner", 125, 200));

        // create description
        JLabel description = new JLabel(DESCRIPTION);

        // create select button group
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(211, 211, 211));
        ButtonGroup buttonGroup = new ButtonGroup();

        JRadioButton btnCreate = new JRadioButton(OPTION_CREATE);
        btnCreate.addActionListener(this);
        buttonGroup.add(btnCreate);
        panel.add(btnCreate);

        JRadioButton btnImport = new JRadioButton(OPTION_IMPORT);
        btnImport.addActionListener(this);
        buttonGroup.add(btnImport);
        panel.add(btnImport);

        // create buttons
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);

        JButton btnNext = new JButton("Next");
        btnNext.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this.getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(21)
                    .addComponent(banner, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, 281, GroupLayout.PREFERRED_SIZE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnNext)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnCancel))
                        .addComponent(description, GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE))
                    .addGap(17))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(52)
                            .addComponent(description)
                            .addGap(18)
                            .addComponent(panel, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(btnNext)
                                .addComponent(btnCancel)))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(36)
                            .addComponent(banner, GroupLayout.PREFERRED_SIZE, 293, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap(49, Short.MAX_VALUE))
        );
        // @formatter:on

        this.getContentPane().setLayout(groupLayout);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
