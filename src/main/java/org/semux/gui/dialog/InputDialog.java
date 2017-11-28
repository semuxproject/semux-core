/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.gui.Action;
import org.semux.message.GUIMessages;
import org.semux.gui.SwingUtil;
import org.semux.util.UnreachableException;

public class InputDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextField textField;
    private String text;

    public InputDialog(JFrame parent, String message, boolean isPassword) {
        super(parent, GUIMessages.get("Input"));

        JLabel labelLogo = new JLabel("");
        labelLogo.setIcon(SwingUtil.loadImage("logo", 96, 96));

        JLabel lblMessage = new JLabel(message);
        textField = isPassword ? new JPasswordField() : SwingUtil.textFieldWithCopyPastePopup();
        textField.setActionCommand(Action.OK.name());
        textField.addActionListener(this);

        JButton btnOk = SwingUtil.createDefaultButton(GUIMessages.get("OK"), this, Action.OK);
        btnOk.setSelected(true);

        JButton btnCancel = SwingUtil.createDefaultButton(GUIMessages.get("Cancel"), this, Action.CANCEL);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(labelLogo)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblMessage)
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(textField, GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))))
                    .addGap(17))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(labelLogo))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(21)
                            .addComponent(lblMessage)
                            .addGap(18)
                            .addComponent(textField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(btnOk)
                                .addComponent(btnCancel))))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            text = textField.getText();
            this.dispose();
            break;
        case CANCEL:
            text = null;
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }

    public String getInput() {
        this.setVisible(true);
        return text;
    }
}
