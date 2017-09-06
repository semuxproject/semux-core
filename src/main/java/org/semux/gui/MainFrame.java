/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.semux.gui.panel.DelegatesPanel;
import org.semux.gui.panel.HomePanel;
import org.semux.gui.panel.ReceivePanel;
import org.semux.gui.panel.SendPanel;
import org.semux.gui.panel.TransactionsPanel;

public class MainFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Semux Wallet";

    private HomePanel panelHome;
    private SendPanel panelSend;
    private ReceivePanel panelReceive;
    private TransactionsPanel panelTransactions;
    private DelegatesPanel panelDelegates;

    private JButton btnHome;
    private JButton btnSend;
    private JButton btnReceive;
    private JButton btnTransactions;
    private JButton btnDelegates;

    private JPanel activePanel;
    private JButton activeButton;

    public MainFrame(Model model) {
        panelHome = new HomePanel(model);
        panelSend = new SendPanel(model);
        panelReceive = new ReceivePanel(model);
        panelTransactions = new TransactionsPanel(model);
        panelDelegates = new DelegatesPanel(model);

        // setup frame properties
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtil.centerizeFrame(this, 800, 600);

        // setup menu bar
        JMenuBar menuBar = new MenuBar();
        this.setJMenuBar(menuBar);

        // setup tool bar
        JPanel toolBar = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setVgap(0);
        layout.setHgap(0);
        layout.setAlignment(FlowLayout.LEFT);
        toolBar.setLayout(layout);
        toolBar.setBorder(new EmptyBorder(15, 15, 15, 15));

        Dimension gap = new Dimension(15, 0);

        btnHome = createButton("Home", "home", Action.SHOW_HOME);
        toolBar.add(btnHome);
        toolBar.add(Box.createRigidArea(gap));

        btnSend = createButton("Send", "send", Action.SHOW_SEND);
        toolBar.add(btnSend);
        toolBar.add(Box.createRigidArea(gap));

        btnReceive = createButton("Receive", "receive", Action.SHOW_RECEIVE);
        toolBar.add(btnReceive);
        toolBar.add(Box.createRigidArea(gap));

        btnTransactions = createButton("Transactions", "transactions", Action.SHOW_TRANSACTIONS);
        toolBar.add(btnTransactions);
        toolBar.add(Box.createRigidArea(gap));

        btnDelegates = createButton("Delegates", "delegates", Action.SHOW_DELEGATES);
        toolBar.add(btnDelegates);

        // setup tabs
        activePanel = new JPanel();
        activePanel.setBorder(new EmptyBorder(0, 15, 15, 15));
        activePanel.setLayout(new BorderLayout(0, 0));

        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(activePanel, BorderLayout.CENTER);

        // show the first tab
        activePanel.add(panelHome);
        select(panelHome, btnHome);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case SHOW_HOME:
            select(panelHome, btnHome);
            break;
        case SHOW_SEND:
            select(panelSend, btnSend);
            break;
        case SHOW_RECEIVE:
            select(panelReceive, btnReceive);
            break;
        case SHOW_TRANSACTIONS:
            select(panelTransactions, btnTransactions);
            break;
        case SHOW_DELEGATES:
            select(panelDelegates, btnDelegates);
            break;
        default:
            break;
        }
    }

    private static Border BORDER_NORMAL = new EmptyBorder(1, 5, 1, 10);
    private static Border BORDER_FOCUS = new CompoundBorder(new LineBorder(new Color(51, 153, 255)),
            new EmptyBorder(0, 4, 0, 9));

    private void select(JPanel panel, JButton button) {
        if (activeButton != null) {
            activeButton.setBorder(BORDER_NORMAL);
        }
        activeButton = button;
        activeButton.setBorder(BORDER_FOCUS);

        activePanel.removeAll();
        activePanel.add(panel);

        activePanel.revalidate();
        activePanel.repaint();
    }

    private JButton createButton(String name, String icon, Action action) {
        JButton btn = new JButton(name);
        btn.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
        btn.setActionCommand(action.name());
        btn.addActionListener(this);
        btn.setIcon(SwingUtil.loadImage(icon, 36, 36));
        btn.setFocusPainted(false);
        btn.setBorder(BORDER_NORMAL);

        return btn;
    }
}
