/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.BlockchainListener;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.gui.MainFrame;
import org.semux.gui.Model;
import org.semux.gui.PasswordFrame;
import org.semux.gui.WelcomeFrame;

/**
 * Graphic user interface.
 */
public class GUI implements BlockchainListener {

    private String[] args;
    private Model model;

    public GUI(String[] args) {
        this.args = args;
        this.model = new Model();
    }

    public Model getModel() {
        return model;
    }

    public void setupLookAndFeel() {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // do nothing
        }
    }

    public void showWelcome() {
        // start welcome frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                WelcomeFrame frame = new WelcomeFrame(GUI.this);
                frame.setVisible(true);
            }
        });
    }

    public void showMain() {
        // start kernel
        CLI.main(args);

        // register block listener
        onBlockAdded(CLI.chain.getLatestBlock());
        CLI.chain.addListener(this);

        // start main frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame(GUI.this);
                frame.setVisible(true);
            }
        });
    }

    @Override
    public void onBlockAdded(Block block) {
        AccountState as = CLI.chain.getAccountState();
        DelegateState ds = CLI.chain.getDeleteState();

        // reset the model.
        model.init(Wallet.getInstance().getAccounts());
        model.setLatestBlockNumber(block.getNumber());
        model.setDelegate(ds.getDelegateByAddress(CLI.coinbase.toAddress()) != null);

        for (Model.Account ma : model.getAccounts()) {
            Account a = as.getAccount(ma.getAddress().toAddress());
            ma.setNonce(a.getNonce());
            ma.setBalance(a.getBalance());
            ma.setLocked(a.getLocked());
        }

        model.fireUpdateEvent();
    }

    public static void main(String[] args) {
        GUI gui = new GUI(args);
        gui.setupLookAndFeel();

        Wallet wallet = Wallet.getInstance();
        if (!wallet.exists()) {
            gui.showWelcome();
        } else {
            for (int i = 0;; i++) {
                PasswordFrame frame = new PasswordFrame(i == 0 ? null : "Wrong password, please try again");
                frame.setVisible(true);
                String pw = frame.getPassword();

                if (pw == null) {
                    System.exit(-1);
                } else if (wallet.unlock(pw)) {
                    break;
                }
            }
            gui.showMain();
        }
    }
}