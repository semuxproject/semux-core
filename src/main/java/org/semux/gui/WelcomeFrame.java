/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.Dimension;
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

import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.gui.model.WalletModel;
import org.semux.util.SystemUtil;
import org.semux.util.UnreachableException;

public class WelcomeFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JPasswordField passwordField;
    private JPasswordField repeatField;
    private JLabel lblRepeat;
    private JRadioButton btnCreate;
    private JRadioButton btnImport;

    private Wallet wallet;

    private File backupFile = null;

    private Object done = new Object();

    public WelcomeFrame(Wallet wallet, WalletModel model) {
        this.wallet = wallet;

        // setup frame properties
        this.setTitle(MessagesUtil.get("SemuxWallet"));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(600, 400));
        SwingUtil.centerizeFrame(this, 600, 400);

        // create banner
        JLabel banner = new JLabel("");
        banner.setIcon(SwingUtil.loadImage("banner", 125, 200));

        // create description
        JLabel description = new JLabel(MessagesUtil.get("WelcomeDescriptionHtml"));

        // create select button group
        Color color = new Color(220, 220, 220);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 3, 8, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(color);
        ButtonGroup buttonGroup = new ButtonGroup();

        btnCreate = new JRadioButton(MessagesUtil.get("CreateNewAccount"));
        btnCreate.setSelected(true);
        btnCreate.setBackground(color);
        btnCreate.setActionCommand(Action.CREATE_ACCOUNT.name());
        btnCreate.addActionListener(this);
        buttonGroup.add(btnCreate);
        panel.add(btnCreate);

        btnImport = new JRadioButton(MessagesUtil.get("ImportAccountsFromBackupFile"));
        btnImport.setBackground(color);
        btnImport.setActionCommand(Action.IMPORT_ACCOUNTS.name());
        btnImport.addActionListener(this);
        buttonGroup.add(btnImport);
        panel.add(btnImport);

        // create buttons
        JButton btnCancel = SwingUtil.createDefaultButton(MessagesUtil.get("Cancel"), this, Action.CANCEL);

        JButton btnNext = SwingUtil.createDefaultButton(MessagesUtil.get("Next"), this, Action.OK);
        btnNext.setSelected(true);

        JLabel lblPassword = new JLabel(MessagesUtil.get("Password") + ":");
        passwordField = new JPasswordField();
        passwordField.setActionCommand(Action.OK.name());
        passwordField.addActionListener(this);

        lblRepeat = new JLabel(MessagesUtil.get("RepeatPassword") + ":");
        repeatField = new JPasswordField();
        repeatField.setActionCommand(Action.OK.name());
        repeatField.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this.getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addComponent(btnNext, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(21)
                            .addComponent(banner, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblRepeat)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                                        .addComponent(lblPassword)
                                        .addComponent(description, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))
                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                                        .addComponent(repeatField, Alignment.LEADING)
                                        .addComponent(passwordField, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE))))))
                    .addGap(32))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(10)
                            .addComponent(description)
                            .addGap(10)
                            .addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(20)
                            .addComponent(lblPassword)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(lblRepeat)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(repeatField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(10)
                            .addComponent(banner, GroupLayout.PREFERRED_SIZE, 293, GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(ComponentPlacement.RELATED, 84, Short.MAX_VALUE)
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
            repeatField.setVisible(true);
            lblRepeat.setVisible(true);
            break;
        case IMPORT_ACCOUNTS:
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter(MessagesUtil.get("WalletBinaryFormat"), "data"));
            int ret = chooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                backupFile = chooser.getSelectedFile();
                repeatField.setVisible(false);
                lblRepeat.setVisible(false);
            } else {
                btnCreate.setSelected(true);
            }
            break;
        case OK:
            String password = new String(passwordField.getPassword());
            String repeat = new String(repeatField.getPassword());
            if (repeatField.isVisible()) {
                if (!password.equals(repeat)) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("RepeatPasswordError"));
                    break;
                }
            }
            if (!wallet.unlock(password)) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("UnlockFailed"));
                break;
            }

            if (backupFile == null) {
                EdDSA key = new EdDSA();
                wallet.addAccount(key);
                wallet.flush();

                done();
            } else {
                Wallet w = new Wallet(backupFile);

                if (!w.unlock(password)) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("BackupUnlockFailed"));
                } else if (w.size() == 0) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("NoAccountFound"));
                } else {
                    wallet.addAccounts(w.getAccounts());
                    wallet.flush();

                    done();
                }
            }
            break;
        case CANCEL:
            SystemUtil.exitAsync(0);
            break;
        default:
            throw new UnreachableException();
        }
    }

    public void join() {
        synchronized (done) {
            try {
                done.wait();
            } catch (InterruptedException e) {
                SystemUtil.exitAsync(0);
            }
        }
    }

    public void done() {
        synchronized (done) {
            done.notifyAll();
        }
    }
}
