/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import java.awt.Dimension;
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
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.model.AddressBook;
import org.semux.gui.model.SemuxAddress;
import org.semux.util.UnreachableException;

public class AddressChangeDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private SemuxAddress address;
    private JTextField txtName;
    private JTextField txtAddress;

    private AddressBookDialog addressBookDialog;

    public AddressChangeDialog(AddressBookDialog parent, SemuxAddress address) {
        super(parent, MessagesUtil.get("ChangeAddressEntry"));
        this.address = address;
        this.addressBookDialog = parent;
        this.setMinimumSize(new Dimension(480, 240));
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(true);

        JLabel lblName = new JLabel(MessagesUtil.get("Name"));
        JLabel lblAddress = new JLabel(MessagesUtil.get("Address"));

        txtName = SwingUtil.textFieldWithCopyPastePopup();
        txtName.setText(address.getName());
        txtAddress = SwingUtil.textFieldWithCopyPastePopup();
        txtAddress.setText(address.getAddress());

        JButton btnOk = SwingUtil.createDefaultButton(MessagesUtil.get("OK"), this, Action.OK);
        btnOk.setSelected(true);

        JButton btnCancel = SwingUtil.createDefaultButton(MessagesUtil.get("Cancel"), this, Action.CANCEL);
        JButton btnDelete = SwingUtil.createDefaultButton(MessagesUtil.get("Delete"), this,
                Action.DELETE_FROM_ADDRESSBOOK);

        JLabel lblMessage = new JLabel(MessagesUtil.get("ChangeAddressEntry"));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(lblMessage))
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblName, 150, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblAddress, 150, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnOk, 150, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(txtName, 100, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtAddress, 100, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnCancel,100, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(btnDelete,100, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                            )
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(lblMessage))
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblName)
                        .addComponent(txtName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(txtAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnOk,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCancel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDelete, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.pack();
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            address.setName(txtName.getText());
            address.setAddress(txtAddress.getText());
            AddressBook.put(address);
            addressBookDialog.refresh();
            this.dispose();
            break;
        case CANCEL:
            this.dispose();
            break;
        case DELETE_FROM_ADDRESSBOOK:
            AddressBook.delete(address.getName());
            addressBookDialog.refresh();
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }

        this.dispose();

    }

}
