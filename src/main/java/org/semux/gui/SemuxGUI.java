/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.core.Wallet;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.dialog.SelectDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletDelegate;
import org.semux.gui.model.WalletModel;
import org.semux.net.Peer;
import org.semux.util.DnsUtil;
import org.semux.message.GUIMessages;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphic user interface.
 */
public class SemuxGUI {

    private static final Logger logger = LoggerFactory.getLogger(SemuxGUI.class);

    private static final int TRANSACTION_LIMIT = 1024; // per account

    private static String dataDir = ".";

    private static Wallet wallet;
    private static WalletModel model;

    private static AtomicBoolean updateFlag = new AtomicBoolean(false);

    public static void fireUpdateEvent() {
        updateFlag.set(true);
    }

    public static void main(String[] args) {
        setupLookAndFeel();

        wallet = new Wallet(new File(dataDir, "wallet.data"));
        model = new WalletModel();

        if (!wallet.exists()) {
            showWelcome();
        } else {
            for (int i = 0;; i++) {
                InputDialog dialog = new InputDialog(null, i == 0 ? GUIMessages.get("EnterPassword") + ":"
                        : GUIMessages.get("WrongPasswordPleaseTryAgain") + ":", true);
                String pwd = dialog.getInput();

                if (pwd == null) {
                    SystemUtil.exitAsync(-1);
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
        WelcomeFrame frame = new WelcomeFrame(wallet);
        frame.setVisible(true);
        frame.join();
        frame.dispose();

        showMain();
    }

    public static void showMain() {
        if (wallet.size() > 1) {
            String message = GUIMessages.get("AccountSelection");
            List<Object> options = new ArrayList<>();
            List<EdDSA> list = wallet.getAccounts();
            for (int i = 0; i < list.size(); i++) {
                options.add(Hex.PREF + list.get(i).toAddressString() + ", " + GUIMessages.get("AccountNumShort", i));
            }

            SelectDialog dialog = new SelectDialog(null, message, options);
            int selected = dialog.getSelectedIndex();
            if (selected == -1) {
                SystemUtil.exitAsync(0);
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
        onBlockAdded(kernel.getBlockchain().getLatestBlock());

        // start main frame
        EventQueue.invokeLater(() -> {
            MainFrame frame = new MainFrame(model);
            frame.setVisible(true);
        });

        // start data refresh
        new Thread(() -> {
            while (true) {
                try {
                    if (kernel.isRunning) {
                        onBlockAdded(kernel.getBlockchain().getLatestBlock());
                    } else {
                        break;
                    }

                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(50);
                        if (updateFlag.compareAndSet(true, false)) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("Data refresh interrupted, exiting");
                    Thread.currentThread().interrupt(); // https://stackoverflow.com/a/4906814/670662
                    break;
                } catch (Exception e) {
                    logger.info("Data refresh exception", e);
                }
            }

            logger.info("Data refresh stopped");
        }, "gui-data").start();

        // start version check
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000L);

                    String hostname = "version.semux.org";
                    List<String> version = DnsUtil.queryTxt(hostname);

                    for (String v : version) {
                        if (SystemUtil.compareVersion(Config.CLIENT_VERSION, v) < 0) {
                            JOptionPane.showMessageDialog(null, GUIMessages.get("WalletNeedToBeUpgraded"));
                            SystemUtil.exitAsync(-1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // https://stackoverflow.com/a/4906814/670662
                    break;
                }
            }
        }, "gui-version").start();
    }

    private static void onBlockAdded(Block block) {
        Kernel kernel = Kernel.getInstance();

        Blockchain chain = kernel.getBlockchain();
        AccountState as = chain.getAccountState();
        DelegateState ds = chain.getDelegateState();

        // update latestBlock and isDelegate
        model.setLatestBlock(block);
        model.setDelegate(ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null);

        // update accounts
        List<WalletAccount> was = new ArrayList<>();
        for (EdDSA key : wallet.getAccounts()) {
            Account a = as.getAccount(key.toAddress());
            WalletAccount wa = new WalletAccount(key, a);
            was.add(wa);

            // most recent transactions of this account
            byte[] address = wa.getKey().toAddress();
            int total = chain.getTransactionCount(address);
            List<Transaction> list = chain.getTransactions(address, Math.max(0, total - TRANSACTION_LIMIT), total);
            Collections.reverse(list);
            wa.setTransactions(list);
        }
        model.setAccounts(was);

        // update delegates
        List<WalletDelegate> wds = new ArrayList<>();
        for (Delegate d : ds.getDelegates()) {
            WalletDelegate wd = new WalletDelegate(d);
            wds.add(wd);
        }
        model.setDelegates(wds);

        // update active peers
        Map<String, Peer> activePeers = new HashMap<>();
        for (Peer peer : kernel.getChannelManager().getActivePeers()) {
            activePeers.put(peer.getPeerId(), peer);
        }
        model.setActivePeers(activePeers);

        model.fireUpdateEvent();
    }
}