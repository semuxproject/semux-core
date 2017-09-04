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
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.semux.GUI;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;

public class WelcomeFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Semux Wallet";
    private static final String DESCRIPTION = "<html>" + //
            "<h1>Welcome to semux!</h1>" + //
            "<p>Do you want to create a new account, or import accounts from backup files?</p>" + //
            "</html>";
    private static final String LABEL_CREATE = "Create new account";
    private static final String LABEL_IMPORT = "Import acounts from backup file";

    private JPasswordField passwordField;
    private JRadioButton btnCreate;
    private JRadioButton btnImport;

    private Wallet wallet;

    private File backupFile = null;

    public WelcomeFrame(Wallet wallet, Model model) {
        this.wallet = wallet;

        // setup frame properties
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        SwingUtil.centerizeFrame(this, 600, 400);

        // create banner
        JLabel banner = new JLabel("");
        banner.setIcon(SwingUtil.loadImage("banner", 125, 200));

        // create description
        JLabel description = new JLabel(DESCRIPTION);

        // create select button group
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 3, 8, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(211, 211, 211));
        ButtonGroup buttonGroup = new ButtonGroup();

        btnCreate = new JRadioButton(LABEL_CREATE);
        btnCreate.setSelected(true);
        btnCreate.setActionCommand(Action.CREATE_ACCOUNT.name());
        btnCreate.addActionListener(this);
        buttonGroup.add(btnCreate);
        panel.add(btnCreate);

        btnImport = new JRadioButton(LABEL_IMPORT);
        btnImport.setActionCommand(Action.IMPORT_ACCOUNTS.name());
        btnImport.addActionListener(this);
        buttonGroup.add(btnImport);
        panel.add(btnImport);

        // create buttons
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        btnCancel.setActionCommand(Action.CANCEL.name());

        JButton btnNext = new JButton("Next");
        btnNext.setSelected(true);
        btnNext.addActionListener(this);
        btnNext.setActionCommand(Action.OK.name());

        passwordField = new JPasswordField();
        passwordField.setActionCommand(Action.OK.name());
        passwordField.addActionListener(this);

        JLabel lblPassword = new JLabel("Password:");

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this.getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnNext, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(21)
                            .addComponent(banner, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 269, GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblPassword)
                                .addComponent(description, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))))
                    .addGap(32))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(52)
                            .addComponent(description)
                            .addGap(18)
                            .addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addComponent(lblPassword)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(36)
                            .addComponent(banner, GroupLayout.PREFERRED_SIZE, 293, GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(ComponentPlacement.RELATED, 54, Short.MAX_VALUE)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnNext)
                        .addComponent(btnCancel))
                    .addGap(21))
        );
        // @formatter:on

        this.getContentPane().setLayout(groupLayout);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case CREATE_ACCOUNT:
            backupFile = null;
            break;
        case IMPORT_ACCOUNTS:
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Wallet backup file", "data");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(this);

            if (ret == JFileChooser.APPROVE_OPTION) {
                backupFile = chooser.getSelectedFile();
            } else {
                btnCreate.setSelected(true);
            }
            break;
        case OK:
            String password = new String(passwordField.getPassword());

            if (!wallet.unlock(password)) {
                JOptionPane.showMessageDialog(this, "Failed to unlock the wallet, wrong password?");
                break;
            }

            if (backupFile == null) {
                EdDSA key = new EdDSA();
                wallet.addAccount(key);
                wallet.flush();

                goMainFrame();
            } else {
                Wallet w = new Wallet(backupFile);

                if (!w.unlock(password)) {
                    JOptionPane.showMessageDialog(this, "Failed to unlock the backup file.");
                } else if (w.size() == 0) {
                    JOptionPane.showMessageDialog(this, "No account found!");
                } else {
                    wallet.addAccounts(w.getAccounts());
                    wallet.flush();
                    goMainFrame();
                }
            }
            break;
        case CANCEL:
            System.exit(0);
            break;
        default:
            break;
        }
    }

    private void goMainFrame() {
        dispose();

        GUI.showMain();
    }
}
