/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
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
public class GUI {

    private static String dataDir = ".";
    private static int coinbase = 0;

    private static Wallet wallet;
    private static Model model;

    public static void main(String[] args) {
        // TODO: parse parameters for GUI

        setupLookAndFeel();

        wallet = new Wallet(new File(dataDir, "wallet.data"));
        model = new Model();

        if (!wallet.exists()) {
            showWelcome();
        } else {
            for (int i = 0;; i++) {
                PasswordFrame frame = new PasswordFrame(i == 0 ? null : "Wrong password, please try again");
                frame.setVisible(true);
                String pwd = frame.getPassword();

                if (pwd == null) {
                    System.exit(-1);
                } else if (wallet.unlock(pwd)) {
                    break;
                }
            }
            showMain();
        }
    }

    public static void setupLookAndFeel() {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // do nothing
        }
    }

    public static void showWelcome() {
        // start welcome frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                WelcomeFrame frame = new WelcomeFrame(model);
                frame.setVisible(true);
            }
        });
    }

    public static void showMain() {
        // start kernel
        Kernel kernel = Kernel.getInstance();
        kernel.init(dataDir, wallet, coinbase);

        // register block listener
        BlockchainListener listener = (block) -> {
            onBlockAdded(block);
        };
        listener.onBlockAdded(kernel.getBlockchain().getLatestBlock());
        kernel.getBlockchain().addListener(listener);

        // start main frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame(model);
                frame.setVisible(true);
            }
        });
    }

    private static void onBlockAdded(Block block) {
        Kernel kernel = Kernel.getInstance();

        Blockchain chain = kernel.getBlockchain();
        AccountState as = chain.getAccountState();
        DelegateState ds = chain.getDeleteState();

        // reset the model.
        model.init(wallet.getAccounts());
        model.setLatestBlockNumber(block.getNumber());
        model.setDelegate(ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null);

        for (Model.Account ma : model.getAccounts()) {
            Account a = as.getAccount(ma.getAddress().toAddress());
            ma.setNonce(a.getNonce());
            ma.setBalance(a.getBalance());
            ma.setLocked(a.getLocked());
            ma.setTransactions(chain.getTransactions(ma.getAddress().toAddress()));
        }

        model.setDelegates(ds.getDelegates());

        model.fireUpdateEvent();
    }
}