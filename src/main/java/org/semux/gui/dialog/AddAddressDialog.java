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
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.gui.Action;
import org.semux.gui.SwingUtil;
import org.semux.message.GUIMessages;
import org.semux.util.exception.UnreachableException;

public class AddAddressDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private AddressBookDialog addressBookDialog;

    private JTextField name;
    private JTextField address;

    public AddAddressDialog(AddressBookDialog addressBookDialog) {
        super(addressBookDialog, GUIMessages.get("AddAddress"));
        this.addressBookDialog = addressBookDialog;

        JLabel lblName = new JLabel(GUIMessages.get("Name"));
        JLabel lblAddress = new JLabel(GUIMessages.get("Address"));

        name = new JTextField();
        address = new JTextField();

        JButton btnCancel = SwingUtil.createDefaultButton(GUIMessages.get("Cancel"), this, Action.CANCEL);
        JButton btnOk = SwingUtil.createDefaultButton(GUIMessages.get("OK"), this, Action.OK);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(32)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblAddress)
                        .addComponent(lblName))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnCancel)
                            .addGap(18)
                            .addComponent(btnOk))
                        .addComponent(address, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                        .addComponent(name))
                    .addContainerGap(32, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(40)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblName)
                        .addComponent(name, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(address, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnCancel)
                        .addComponent(btnOk))
                    .addContainerGap(40, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(addressBookDialog);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            String n = name.getText().trim();
            String a = address.getText().trim();
            if (!n.isEmpty() && !a.isEmpty()) {
                addressBookDialog.getAddressBook().put(name.getText(), address.getText());
                addressBookDialog.refresh();
                this.dispose();
            }
            break;
        case CANCEL:
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }
}
