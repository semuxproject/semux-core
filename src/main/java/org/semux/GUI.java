/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainListener;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.gui.MainFrame;
import org.semux.gui.Model;
import org.semux.gui.WelcomeFrame;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.dialog.SelectDialog;

/**
 * Graphic user interface.
 */
public class GUI {

    private static final int TRANSACTION_LIMIT = 1000; // per account

    private static String dataDir = ".";

    private static Wallet wallet;
    private static Model model;

    public static void main(String[] args) {
        setupLookAndFeel();

        wallet = new Wallet(new File(dataDir, "wallet.data"));
        model = new Model();

        if (!wallet.exists()) {
            showWelcome();
        } else {
            for (int i = 0;; i++) {
                InputDialog frame = new InputDialog(
                        i == 0 ? "Please enter your password:" : "Wrong password, please try again:", true);
                String pwd = frame.getInput();

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
        EventQueue.invokeLater(() -> {
            WelcomeFrame frame = new WelcomeFrame(wallet, model);
            frame.setVisible(true);
        });
    }

    public static void showMain() {
        if (wallet.size() > 0) {
            String message = "Which account would you like to use?";
            List<Object> options = new ArrayList<>();
            List<EdDSA> list = wallet.getAccounts();
            for (int i = 0; i < list.size(); i++) {
                options.add("0x" + list.get(i).toAddressString() + ", #" + i);
            }

            SelectDialog dialog = new SelectDialog(message, options);
            int selected = dialog.getSelectedIndex();
            if (selected == -1) {
                System.exit(0);
            } else {
                model.setCoinbase(selected);
            }
        } else if (wallet.size() == 0) {
            wallet.addAccount(new EdDSA());
            wallet.flush();
        }

        // start kernel
        Kernel kernel = Kernel.getInstance();
        kernel.init(dataDir, wallet, model.getCoinbase());
        kernel.start();

        // register block listener
        BlockchainListener listener = (block) -> {
            onBlockAdded(block);
        };
        listener.onBlockAdded(kernel.getBlockchain().getLatestBlock());
        kernel.getBlockchain().addListener(listener);

        // start main frame
        EventQueue.invokeLater(() -> {
            MainFrame frame = new MainFrame(model);
            frame.setVisible(true);
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
            Account a = as.getAccount(ma.getKey().toAddress());
            ma.setNonce(a.getNonce());
            ma.setBalance(a.getBalance());
            ma.setLocked(a.getLocked());
            ma.setTransactions(chain.getTransactions(ma.getKey().toAddress(), TRANSACTION_LIMIT));
        }

        model.setDelegates(ds.getDelegates());

        model.fireUpdateEvent();
    }
}