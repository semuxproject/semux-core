/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.semux.gui.panel.DelegatesPanel;
import org.semux.gui.panel.HomePanel;
import org.semux.gui.panel.ReceivePanel;
import org.semux.gui.panel.SendPanel;
import org.semux.gui.panel.TransactionsPanel;

public class MainFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Semux Wallet";

    private JPanel panelHome = new HomePanel();
    private JPanel panelSend = new SendPanel();
    private JPanel panelReceive = new ReceivePanel();
    private JPanel panelTransactions = new TransactionsPanel();
    private JPanel panelDelegates = new DelegatesPanel();

    private JPanel tabs;

    public MainFrame() {
        // setup frame properties
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtil.centerizeFrame(this, 800, 600);

        // setup menu bar
        JMenuBar menuBar = new MenuBar();
        this.setJMenuBar(menuBar);

        // setup tool bar
        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(new EmptyBorder(15, 15, 15, 15));
        toolBar.setFloatable(false);

        JButton btnHome = new JButton("Home");
        btnHome.setActionCommand(Action.SHOW_HOME.name());
        btnHome.addActionListener(this);
        btnHome.setIcon(SwingUtil.loadImage("send", 32, 32));
        toolBar.add(btnHome);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnSend = new JButton("Send");
        btnSend.setActionCommand(Action.SHOW_SEND.name());
        btnSend.addActionListener(this);
        btnSend.setIcon(SwingUtil.loadImage("send", 32, 32));
        toolBar.add(btnSend);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnReceive = new JButton("Receive");
        btnReceive.setActionCommand(Action.SHOW_RECEIVE.name());
        btnReceive.addActionListener(this);
        btnReceive.setIcon(SwingUtil.loadImage("receive", 32, 32));
        toolBar.add(btnReceive);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnTransactions = new JButton("Transactions");
        btnTransactions.setActionCommand(Action.SHOW_TRANSACTIONS.name());
        btnTransactions.addActionListener(this);
        btnTransactions.setIcon(SwingUtil.loadImage("receive", 32, 32));
        toolBar.add(btnTransactions);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnDelegates = new JButton("Delegates");
        btnDelegates.setActionCommand(Action.SHOW_DELEGATES.name());
        btnDelegates.addActionListener(this);
        btnDelegates.setIcon(SwingUtil.loadImage("delegates", 32, 32));
        toolBar.add(btnDelegates);

        // setup tabs
        tabs = new JPanel();
        tabs.setBorder(new EmptyBorder(0, 15, 15, 15));
        tabs.setLayout(new BorderLayout(0, 0));

        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);

        // show the first tab
        tabs.add(panelHome);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        JPanel tab = null;
        switch (action) {
        case SHOW_HOME:
            tab = panelHome;
            break;
        case SHOW_SEND:
            tab = panelSend;
            break;
        case SHOW_RECEIVE:
            tab = panelReceive;
            break;
        case SHOW_TRANSACTIONS:
            tab = panelTransactions;
            break;
        case SHOW_DELEGATES:
            tab = panelDelegates;
            break;
        default:
            break;
        }

        if (tab != null) {
            tabs.removeAll();
            tabs.add(tab);

            tabs.revalidate();
            tabs.repaint();
        }
    }
}
