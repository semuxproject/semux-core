/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import org.semux.core.Wallet;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;

public class ChangePasswordDialog extends JDialog implements ActionListener {

    private transient SemuxGui gui;

    public ChangePasswordDialog(SemuxGui gui, JFrame parent) {
        super(parent, GuiMessages.get("ChangePassword"));
        this.gui = gui;

        JLabel lblOldPassword = new JLabel(GuiMessages.get("OldPassword") + ":");
        JLabel lblPassword = new JLabel(GuiMessages.get("Password") + ":");
        JLabel lblRepeat = new JLabel(GuiMessages.get("RepeatPassword") + ":");

        oldPasswordField = new JPasswordField();
        passwordField = new JPasswordField();
        repeatField = new JPasswordField();

        JButton btnOk = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

        JButton btnCancel = SwingUtil.createDefaultButton(GuiMessages.get("Cancel"), this, Action.CANCEL);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblPassword)
                        .addComponent(lblOldPassword)
                        .addComponent(lblRepeat))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnCancel)
                            .addGap(18)
                            .addComponent(btnOk))
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(repeatField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(passwordField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(oldPasswordField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)))
                    .addGap(23))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(32)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblOldPassword)
                        .addComponent(oldPasswordField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblPassword)
                        .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRepeat)
                        .addComponent(repeatField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnCancel)
                        .addComponent(btnOk))
                    .addContainerGap(77, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(400, 240));
        this.setLocationRelativeTo(parent);
    }

    private static final long serialVersionUID = 1L;
    private JPasswordField oldPasswordField;
    private JPasswordField passwordField;
    private JPasswordField repeatField;

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK: {
            String oldPassword = new String(oldPasswordField.getPassword());
            String newPassword = new String(passwordField.getPassword());
            String newPasswordRepeat = new String(repeatField.getPassword());

            Wallet wallet = gui.getKernel().getWallet();

            if (!newPassword.equals(newPasswordRepeat)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("RepeatPasswordError"));
            } else if (!wallet.unlock(oldPassword)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("IncorrectPassword"));
            } else {
                wallet.changePassword(newPassword);
                wallet.flush();
                JOptionPane.showMessageDialog(this, GuiMessages.get("PasswordChanged"));
                this.dispose();
            }
            break;
        }
        case CANCEL: {
            this.dispose();
            break;
        }
        default:
            break;
        }
    }
}
