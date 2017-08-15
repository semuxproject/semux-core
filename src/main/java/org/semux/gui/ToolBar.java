/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JToolBar;

public class ToolBar extends JToolBar implements ActionListener {

    private static final long serialVersionUID = 1L;

    public ToolBar(boolean floatable) {
        this.setFloatable(floatable);

        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(this);
        btnSend.setIcon(SwingUtil.loadImage("send", 32, 32));
        this.add(btnSend);
        this.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnReceive = new JButton("Receive");
        btnReceive.addActionListener(this);
        btnReceive.setIcon(SwingUtil.loadImage("receive", 32, 32));
        this.add(btnReceive);
        this.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnTransactions = new JButton("Transactions");
        btnTransactions.addActionListener(this);
        btnTransactions.setIcon(SwingUtil.loadImage("receive", 32, 32));
        this.add(btnTransactions);
        this.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnDelegates = new JButton("Delegates");
        btnDelegates.addActionListener(this);
        btnDelegates.setIcon(SwingUtil.loadImage("delegates", 32, 32));
        this.add(btnDelegates);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }

}
