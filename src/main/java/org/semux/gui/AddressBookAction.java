/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import org.apache.commons.lang3.StringUtils;
import org.semux.gui.dialog.AddressBookSelectDialog;
import org.semux.gui.model.AddressBook;

public class AddressBookAction {

    public static String addToAddressBookAction = "add-to-addressbook-action";
    public static String getFromAddressBookAction = "get-from-addressbook-action";

    public static class AddToAddressBookAction extends TextAction {

        private static final long serialVersionUID = 1L;
        private JComponent invokingComponent;

        /**
         * Create this object with the appropriate identifier.
         *
         * @param comp
         */
        public AddToAddressBookAction(JComponent comp) {
            super(addToAddressBookAction);
            invokingComponent = comp;
        }

        /**
         * Adds an address to the AddressBook
         *
         * @param event
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent target = (JTextComponent) invokingComponent;
            if (invokingComponent != null) {
                String name = JOptionPane.showInputDialog(MessagesUtil.get("AddressBookName"));
                if (StringUtils.isNotEmpty(name)) {
                    AddressBook.put(name, target.getText());
                }
            }
        }
    }

    /**
     * Action to retrieve an address from address book
     */
    public static class GetFromAddressBookAction extends TextAction {

        private static final long serialVersionUID = 1L;
        private JComponent invokingComponent;

        /**
         * Create this object with the appropriate identifier.
         *
         * @param component
         */
        public GetFromAddressBookAction(JComponent component) {
            super(getFromAddressBookAction);
            invokingComponent = component;
        }

        /**
         * Adds an address to the AddressBook
         *
         * @param event
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent target = (JTextComponent) invokingComponent;
            if (target != null) {
                AddressBookSelectDialog select = new AddressBookSelectDialog(null);
                select.setVisible(true);
                target.setText(AddressBook.getAddress(select.getSelected()).getAddress());
            }
        }
    }
}
