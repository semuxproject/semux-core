/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;
import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.MainFrame;
import org.semux.gui.MessagesUtil;
import org.semux.gui.Model;
import org.semux.gui.WelcomeFrame;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.dialog.SelectDialog;
import org.semux.net.Peer;
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Graphic user interface.
 */
public class GUI {

    private static Logger logger = LoggerFactory.getLogger(GUI.class);

    private static final int TRANSACTION_LIMIT = 1024; // per account

    private static String dataDir = ".";

    private static Wallet wallet;
    private static Model model;

    private static AtomicBoolean updateFlag = new AtomicBoolean(false);

    public static void fireUpdateEvent() {
        updateFlag.set(true);
    }

    public static void main(String[] args) {
        setupLookAndFeel();

        wallet = new Wallet(new File(dataDir, "wallet.data"));
        model = new Model();

        if (!wallet.exists()) {
            showWelcome();
        } else {
            for (int i = 0;; i++) {
                InputDialog dialog = new InputDialog(null, i == 0 ? MessagesUtil.get("EnterPassword") + ":"
                        : MessagesUtil.get("WrongPasswordPleaseTryAgain") + ":", true);
                String pwd = dialog.getInput();

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
        WelcomeFrame frame = new WelcomeFrame(wallet, model);
        frame.setVisible(true);
        frame.join();
        frame.dispose();

        showMain();
    }

    public static void showMain() {
        if (wallet.size() > 1) {
            String message = MessagesUtil.get("AccountSelection");
            List<Object> options = new ArrayList<>();
            List<EdDSA> list = wallet.getAccounts();
            for (int i = 0; i < list.size(); i++) {
                options.add(Hex.PREF + list.get(i).toAddressString() + ", " + MessagesUtil.get("AccountNumShort") + i);
            }

            SelectDialog dialog = new SelectDialog(null, message, options);
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
                    ReadLock r = Config.STATE_LOCK.readLock();

                    r.lock();
                    if (kernel.isRunning) {
                        onBlockAdded(kernel.getBlockchain().getLatestBlock());
                    } else {
                        break;
                    }
                    r.unlock();

                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(50);
                        if (updateFlag.get()) {
                            updateFlag.set(false);
                            break;
                        }
                    }

                } catch (InterruptedException e) {
                    logger.info("Data refresh interrupted, exiting");
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
                    Thread.sleep(5 * 60 * 1000);

                    String domain = "version.semux.org";
                    Lookup lookup = new Lookup(domain, Type.TXT);
                    lookup.setResolver(new SimpleResolver());
                    lookup.setCache(null);
                    Record[] records = lookup.run();

                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        for (Record record : records) {
                            TXTRecord txt = (TXTRecord) record;
                            for (Object str : txt.getStrings()) {
                                String version = str.toString();

                                if (SystemUtil.compareVersion(Config.CLIENT_VERSION, version) < 0) {
                                    JOptionPane.showMessageDialog(null, MessagesUtil.get("WalletNeedToBeUpgraded"));
                                    System.exit(-1);
                                }
                            }
                        }
                    }
                } catch (TextParseException | UnknownHostException e) {
                    logger.debug("Failed to get min client version");
                } catch (InterruptedException e) {
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

        // reset the model.
        model.init(wallet.getAccounts());
        model.setLatestBlock(block);
        model.setDelegate(ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null);
        for (Model.Account ma : model.getAccounts()) {
            Account a = as.getAccount(ma.getKey().toAddress());
            ma.setNonce(a.getNonce());
            ma.setBalance(a.getBalance());
            ma.setLocked(a.getLocked());

            // most recent transactions of this account
            byte[] address = ma.getKey().toAddress();
            int total = chain.getTotalTransactions(address);
            List<Transaction> list = chain.getTransactions(address, Math.max(0, total - TRANSACTION_LIMIT), total);
            Collections.reverse(list);
            ma.setTransactions(list);
        }
        model.setDelegates(ds.getDelegates());
        Map<String, Peer> activePeers = new HashMap<>();
        for (Peer peer : kernel.getChannelManager().getActivePeers()) {
            activePeers.put(peer.getPeerId(), peer);
        }
        model.setActivePeers(activePeers);

        model.fireUpdateEvent();
    }
}