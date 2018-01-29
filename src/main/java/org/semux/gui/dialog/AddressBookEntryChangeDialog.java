/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.semux.core.Wallet;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.Action;
import org.semux.gui.AddressBookEntry;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.util.exception.UnreachableException;

public class AddressBookEntryChangeDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextField textFieldName;
    private JTextField textFieldAddress;
    private Wallet wallet;
    private WalletModel model;
    private AddressBookEntry entry;

    public AddressBookEntryChangeDialog(JFrame parent, AddressBookEntry entry, Wallet wallet, WalletModel model) {
        super(parent, GuiMessages.get("ChangeAddressBookEntry"));
        this.wallet = wallet;
        this.model = model;
        this.entry = entry;

        JLabel lblName = new JLabel("Name");

        JLabel lblAddress = new JLabel("Address");

        textFieldName = new JTextField(entry.getName());
        textFieldName.setColumns(10);

        textFieldAddress = new JTextField(entry.getAddress());
        textFieldAddress.setColumns(10);

        JButton btnUpdate = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

        JButton btnCancel = SwingUtil.createDefaultButton(GuiMessages.get("Cancel"), this, Action.CANCEL);

        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGap(38)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(lblAddress)
                                        .addComponent(lblName))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(btnUpdate)
                                                .addGap(18)
                                                .addComponent(btnCancel))
                                        .addComponent(textFieldName, GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
                                        .addComponent(textFieldAddress, GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE))
                                .addContainerGap()));
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGap(37)
                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblName)
                                        .addComponent(textFieldName, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblAddress)
                                        .addComponent(textFieldAddress, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(btnUpdate)
                                        .addComponent(btnCancel))
                                .addContainerGap(125, Short.MAX_VALUE)));
        getContentPane().setLayout(groupLayout);
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(400, 240));
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            String address = textFieldAddress.getText();
            String name = textFieldName.getText().trim();
            if (StringUtils.isEmpty(name)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidName"));
            } else if (address != null) {
                try {
                    byte[] addr = Hex.decode0x(address.trim());
                    if (addr.length != Key.ADDRESS_LEN) {
                        JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidAddress"));
                    } else {
                        byte[] oldAddr = Hex.decode0x(entry.getAddress());
                        wallet.removeAddressAlias(oldAddr);
                        wallet.setAddressAlias(addr, name.trim());
                        wallet.flush();
                        model.fireUpdateEvent();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidAddress"));
                }
                this.dispose();
                textFieldName.setText("");
                textFieldAddress.setText("");
            }
            break;
        case CANCEL:
            textFieldName.setText("");
            textFieldAddress.setText("");
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }
}
