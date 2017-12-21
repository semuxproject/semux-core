/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.semux.Kernel;
import org.semux.Launcher;
import org.semux.cli.SemuxOption;
import org.semux.config.Constants;
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
import org.semux.message.CLIMessages;
import org.semux.message.GUIMessages;
import org.semux.net.Peer;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Graphic user interface.
 */
public class SemuxGUI extends Launcher {

    private static final Logger logger = LoggerFactory.getLogger(SemuxGUI.class);

    private static final int TRANSACTION_LIMIT = 1024; // per account

    private Wallet wallet;

    private WalletModel model;

    private Kernel kernel;

    public static void main(String[] args) {
        try {
            setupLookAndFeel();

            SemuxGUI gui = new SemuxGUI();
            // set up logger
            gui.setupLogger(args);
            // start
            gui.start(args);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(null, "Filed to parse the parameters: " + e.getMessage());
        }
    }

    /**
     * Creates a new Semux GUI instance.
     */
    public SemuxGUI() {
        Option dataDirOption = Option.builder()
                .longOpt(SemuxOption.DATA_DIR.toString())
                .desc(CLIMessages.get("SpecifyDataDir")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("path").type(String.class)
                .build();
        addOption(dataDirOption);

        Option networkOption = Option.builder()
                .longOpt(SemuxOption.NETWORK.toString()).desc(CLIMessages.get("SpecifyNetwork")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("network").type(String.class)
                .build();
        addOption(networkOption);
    }

    public SemuxGUI(WalletModel model, Kernel kernel) {
        this.model = model;
        this.kernel = kernel;
    }

    public Kernel getKernel() {
        return kernel;
    }

    public WalletModel getModel() {
        return model;
    }

    public void start(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getOptions(), args);

        if (cmd.hasOption(SemuxOption.DATA_DIR.toString())) {
            setDataDir(cmd.getOptionValue(SemuxOption.DATA_DIR.toString()));
        }

        if (cmd.hasOption(SemuxOption.NETWORK.toString())) {
            setNetwork(cmd.getOptionValue(SemuxOption.NETWORK.toString()));
        }

        start();
    }

    protected void start() {
        wallet = new Wallet(new File(getDataDir(), "wallet.data"));
        model = new WalletModel(new AddressBook(new File(getDataDir(), "addressbook.json")));

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

    public void showWelcome() {
        // start welcome frame
        WelcomeFrame frame = new WelcomeFrame(wallet);
        frame.setVisible(true);
        frame.join();
        frame.dispose();

        showMain();
    }

    public void showMain() {
        if (wallet.size() > 1) {
            String message = GUIMessages.get("AccountSelection");
            List<Object> options = new ArrayList<>();
            List<EdDSA> list = wallet.getAccounts();
            for (int i = 0; i < list.size(); i++) {
                options.add(Hex.PREF + list.get(i).toAddressString() + ", " + GUIMessages.get("AccountNumShort", i));
            }

            SelectDialog dialog = new SelectDialog(null, message, options);
            setCoinbase(dialog.getSelectedIndex());
            if (getCoinbase() == -1) {
                SystemUtil.exitAsync(0);
            } else {
                model.setCoinbase(getCoinbase());
            }
        } else if (wallet.size() == 0) {
            wallet.addAccount(new EdDSA());
            wallet.flush();
        }

        // start kernel
        kernel = new Kernel(getConfig(), wallet, wallet.getAccount(getCoinbase()));
        kernel.start();
        onBlockAdded(kernel.getBlockchain().getLatestBlock());

        // start main frame
        EventQueue.invokeLater(() -> {
            MainFrame frame = new MainFrame(this);
            frame.setVisible(true);
        });

        // start data refresh
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    logger.info("Data refresh interrupted, exiting");
                    Thread.currentThread().interrupt();
                    break;
                }

                // stops if kernel stops
                if (!kernel.isRunning()) {
                    break;
                }

                // necessary because when kernel exists, the GUI component is not closed.
                ReadLock lock = kernel.getStateLock().readLock();
                lock.lock();
                onBlockAdded(kernel.getBlockchain().getLatestBlock());
                lock.unlock();
            }

            logger.info("Data refresh stopped");
        }, "gui-data").start();

        // start version check
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5L * 60L * 1000L);

                    URL url = new URL("http://api.semux.org");
                    URLConnection con = url.openConnection();
                    con.addRequestProperty("User-Agent", Constants.DEFAULT_USER_AGENT);
                    con.setConnectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT);
                    con.setReadTimeout(Constants.DEFAULT_READ_TIMEOUT);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(con.getInputStream());
                    String v = node.get("minVersion").asText();

                    if (SystemUtil.compareVersion(Constants.CLIENT_VERSION, v) < 0) {
                        JOptionPane.showMessageDialog(null, GUIMessages.get("WalletNeedToBeUpgraded"));
                        SystemUtil.exitAsync(-1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    logger.info("Failed to retrieve latest version");
                }
            }
        }, "gui-version").start();
    }

    private void onBlockAdded(Block block) {
        Blockchain chain = kernel.getBlockchain();
        AccountState as = chain.getAccountState();
        DelegateState ds = chain.getDelegateState();

        // update latest block and coinbase delegate status
        model.setSyncProgress(kernel.getSyncManager().isRunning() ? kernel.getSyncManager().getProgress() : null);
        model.setLatestBlock(block);
        model.setDelegate(ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null);

        // refresh accounts
        if (wallet.isUnlocked()) {
            List<WalletAccount> accounts = new ArrayList<>();
            for (EdDSA key : wallet.getAccounts()) {
                Account a = as.getAccount(key.toAddress());
                WalletAccount wa = new WalletAccount(key, a);
                accounts.add(wa);
            }
            model.setAccounts(accounts);
        }

        // update transactions
        for (WalletAccount a : model.getAccounts()) {
            // most recent transactions of this account
            byte[] address = a.getKey().toAddress();
            int total = chain.getTransactionCount(address);
            List<Transaction> list = chain.getTransactions(address, Math.max(0, total - TRANSACTION_LIMIT), total);
            Collections.reverse(list);
            a.setTransactions(list);
        }

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

        // fire an update event
        model.fireUpdateEvent();
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
}