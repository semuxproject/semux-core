/**
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
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.model.WalletModel;
import org.semux.gui.panel.DelegatesPanel;
import org.semux.gui.panel.HomePanel;
import org.semux.gui.panel.ReceivePanel;
import org.semux.gui.panel.SendPanel;
import org.semux.gui.panel.TransactionsPanel;
import org.semux.util.UnreachableException;

public class MainFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private LockGlassPane lockGlassPane;

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
    private JButton btnLock;

    private JPanel activePanel;
    private JButton activeButton;

    public MainFrame(WalletModel model) {
        lockGlassPane = new LockGlassPane();
        lockGlassPane.setOpaque(false);
        this.setGlassPane(lockGlassPane);

        panelHome = new HomePanel(model);
        panelSend = new SendPanel(model);
        panelReceive = new ReceivePanel(model);
        panelTransactions = new TransactionsPanel(model);
        panelDelegates = new DelegatesPanel(model);

        // setup frame properties
        this.setTitle(MessagesUtil.get("SemuxWallet"));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(900, 600));
        SwingUtil.centerizeFrame(this, 900, 600);

        // setup menu bar
        JMenuBar menuBar = new MenuBar(this);
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

        btnHome = createButton(MessagesUtil.get("Home"), "home", Action.SHOW_HOME);
        toolBar.add(btnHome);
        toolBar.add(Box.createRigidArea(gap));

        btnSend = createButton(MessagesUtil.get("Send"), "send", Action.SHOW_SEND);
        toolBar.add(btnSend);
        toolBar.add(Box.createRigidArea(gap));

        btnReceive = createButton(MessagesUtil.get("Receive"), "receive", Action.SHOW_RECEIVE);
        toolBar.add(btnReceive);
        toolBar.add(Box.createRigidArea(gap));

        btnTransactions = createButton(MessagesUtil.get("Transactions"), "transactions", Action.SHOW_TRANSACTIONS);
        toolBar.add(btnTransactions);
        toolBar.add(Box.createRigidArea(gap));

        btnDelegates = createButton(MessagesUtil.get("Delegates"), "delegates", Action.SHOW_DELEGATES);
        toolBar.add(btnDelegates);
        toolBar.add(Box.createRigidArea(gap));

        btnLock = createButton(MessagesUtil.get("Lock"), "lock", Action.LOCK);
        toolBar.add(btnLock);

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
        case LOCK:
            lockGlassPane.setVisible(true);
            break;
        default:
            throw new UnreachableException();
        }
    }

    private static Border BORDER_NORMAL = new CompoundBorder(new LineBorder(new Color(180, 180, 180)),
            new EmptyBorder(0, 5, 0, 10));
    private static Border BORDER_FOCUS = new CompoundBorder(new LineBorder(new Color(51, 153, 255)),
            new EmptyBorder(0, 5, 0, 10));

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
        btn.setContentAreaFilled(false);

        return btn;
    }

    private class LockGlassPane extends JPanel {

        private static final long serialVersionUID = 1L;

        public LockGlassPane() {
            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    InputDialog dialog = new InputDialog(MainFrame.this, MessagesUtil.get("EnterPassword") + ":", true);
                    String pwd = dialog.getInput();

                    if (pwd != null) {
                        Wallet w = Kernel.getInstance().getWallet();
                        if (w.getPassword().equals(pwd)) {
                            lockGlassPane.setVisible(false);
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.this, MessagesUtil.get("IncorrectPassword"));
                        }
                    }
                }
            });
        }

        public void paintComponent(Graphics g) {
            g.setColor(new Color(0, 0, 0, 96));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }
}
