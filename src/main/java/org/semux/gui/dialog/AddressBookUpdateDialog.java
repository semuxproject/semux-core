/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

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

public class AddressBookUpdateDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private transient Wallet wallet;
    private transient WalletModel model;

    private JTextField nameText;
    private JTextField addressText;

    public AddressBookUpdateDialog(Window parent, AddressBookEntry entry, Wallet wallet, WalletModel model) {
        super(parent,
                entry != null ? GuiMessages.get("EditAddressBookEntry") : GuiMessages.get("AddAddressBookEntry"));

        this.wallet = wallet;
        this.model = model;

        JLabel lblName = new JLabel("Name");
        JLabel lblAddress = new JLabel("Address");

        nameText = new JTextField(entry != null ? entry.getName() : "");
        addressText = new JTextField(entry != null ? entry.getAddress() : "");

        JButton btnCancel = SwingUtil.createDefaultButton(GuiMessages.get("Cancel"), this, Action.CANCEL);
        JButton btnOk = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

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
                        .addComponent(addressText, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                        .addComponent(nameText))
                    .addContainerGap(32, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(40)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblName)
                        .addComponent(nameText, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(addressText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
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
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            String name = nameText.getText().trim();
            String address = addressText.getText().trim();

            if (StringUtils.isEmpty(name)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidName"));
                return;
            }
            if (StringUtils.isEmpty(address) || !address.matches("0x[a-z0-9]{" + Key.ADDRESS_LEN * 2 + "}")) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidAddress"));
                break;
            }

            wallet.setAddressAlias(Hex.decode0x(address), name);
            wallet.flush();

            model.fireUpdateEvent();

            this.dispose();
            break;
        case CANCEL:
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }
}
