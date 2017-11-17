/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.gui.Action;
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.AddressBook;
import org.semux.gui.model.SemuxAddress;
import org.semux.util.UnreachableException;

public class AddressBookSelectDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private String selected = "";

    private JComboBox<String> comboBox;

    public AddressBookSelectDialog(JFrame parent) {
        super(parent, MessagesUtil.get("Select"));
        this.setMinimumSize(new Dimension(400, 240));
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JLabel labelLogo = new JLabel("");
        labelLogo.setIcon(SwingUtil.loadImage("logo", 96, 96));

        JButton btnOk = SwingUtil.createDefaultButton(MessagesUtil.get("OK"), this, Action.OK);
        JButton btnCancel = SwingUtil.createDefaultButton(MessagesUtil.get("Canel"), this, Action.CANCEL);

        JLabel lblMessage = new JLabel(MessagesUtil.get("FromAddressbook"));
        comboBox = new JComboBox<>();
        comboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        for (SemuxAddress element : AddressBook.getAllAddresses()) {
            comboBox.addItem(element.getName());
        }
        comboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    actionPerformed(new ActionEvent(AddressBookSelectDialog.this, 0, Action.OK.name()));
                }
            }
        });

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
                        .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                            .addGap(101)
                            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                        .addComponent(comboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                            .addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(16)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(btnOk)
                                .addComponent(btnCancel))))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(parent);
    }

    public String getSelected() {
        return selected;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            selected = (String) comboBox.getSelectedItem();
            break;
        case CANCEL:
            selected = "";
            break;
        default:
            throw new UnreachableException();
        }

        this.dispose();
    }
}
