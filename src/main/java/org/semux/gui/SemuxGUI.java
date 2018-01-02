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

import javax.swing.JFrame;
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

    protected WalletModel model;
    protected Kernel kernel;

    protected boolean isRunning;
    protected JFrame main;
    protected Thread dataThread;
    protected Thread versionThread;

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
        Option dataDirOption = Option.builder().longOpt(SemuxOption.DATA_DIR.toString())
                .desc(CLIMessages.get("SpecifyDataDir")).hasArg(true).numberOfArgs(1).optionalArg(false).argName("path")
                .type(String.class).build();
        addOption(dataDirOption);

        Option networkOption = Option.builder().longOpt(SemuxOption.NETWORK.toString())
                .desc(CLIMessages.get("SpecifyNetwork")).hasArg(true).numberOfArgs(1).optionalArg(false)
                .argName("network").type(String.class).build();
        addOption(networkOption);
    }

    /**
     * Creates a GUI instance with the given model and kernel, for test purpose
     * only.
     *
     * @param model
     * @param kernel
     */
    public SemuxGUI(WalletModel model, Kernel kernel) {
        this.model = model;
        this.kernel = kernel;
    }

    /**
     * Returns the kernel instance.
     *
     * @return
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Returns the model.
     *
     * @return
     */
    public WalletModel getModel() {
        return model;
    }

    /**
     * Starts GUI with the given command line arguments.
     *
     * @param args
     * @throws ParseException
     */
    public void start(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getOptions(), args);

        if (cmd.hasOption(SemuxOption.DATA_DIR.toString())) {
            setDataDir(cmd.getOptionValue(SemuxOption.DATA_DIR.toString()));
        }

        if (cmd.hasOption(SemuxOption.NETWORK.toString())) {
            setNetwork(cmd.getOptionValue(SemuxOption.NETWORK.toString()));
        }

        // create a wallet instance.
        Wallet wallet = new Wallet(new File(getDataDir(), "wallet.data"));

        if (!wallet.exists()) {
            showWelcome(wallet);
        } else {
            showUnlock(wallet);
        }
    }

    /**
     * Shows the welcome frame.
     */
    public void showWelcome(Wallet wallet) {
        // start welcome frame
        WelcomeFrame frame = new WelcomeFrame(wallet);
        frame.setVisible(true);

        // wait until done
        frame.join();
        frame.dispose();

        setupCoinbase(wallet);
    }

    /**
     * Shows the unlock frame, which reads user-entered password and tries to unlock
     * the wallet.
     */
    public void showUnlock(Wallet wallet) {
        for (int i = 0;; i++) {
            InputDialog dialog = new InputDialog(null, i == 0 ? GUIMessages.get("EnterPassword") + ":"
                    : GUIMessages.get("WrongPasswordPleaseTryAgain") + ":", true);
            String pwd = dialog.showAndGet();

            if (pwd == null) {
                SystemUtil.exitAsync(-1);
            } else if (wallet.unlock(pwd)) {
                break;
            }
        }

        setupCoinbase(wallet);
    }

    /**
     * Select an account as coinbase if the wallet is not empty; or create a new
     * account and use it as coinbase.
     */
    public void setupCoinbase(Wallet wallet) {
        if (wallet.size() > 1) {
            String message = GUIMessages.get("AccountSelection");
            List<Object> options = new ArrayList<>();
            List<EdDSA> list = wallet.getAccounts();
            for (int i = 0; i < list.size(); i++) {
                options.add(Hex.PREF + list.get(i).toAddressString() + ", " + GUIMessages.get("AccountNumShort", i));
            }

            // show select dialog
            int index = showSelectDialog(null, message, options);

            if (index == -1) {
                SystemUtil.exitAsync(0);
            } else {
                // use the selected account as coinbase.
                setCoinbase(index);
            }
        } else if (wallet.size() == 0) {
            wallet.addAccount(new EdDSA());
            wallet.flush();

            // use the first account as coinbase.
            setCoinbase(0);
        }

        startKernelAndMain(wallet);
    }

    /**
     * Starts the kernel and shows main frame.
     */
    public synchronized void startKernelAndMain(Wallet wallet) {
        if (isRunning) {
            return;
        }

        // set up model
        model = new WalletModel();
        model.setAddressBook(new AddressBook(new File(getDataDir(), "addressbook.json")));
        model.setCoinbase(getCoinbase());

        // set up kernel
        kernel = new Kernel(getConfig(), wallet, wallet.getAccount(getCoinbase()));
        kernel.start();

        // initialize the model with latest block
        processBlock(kernel.getBlockchain().getLatestBlock());

        // start main frame
        EventQueue.invokeLater(() -> {
            main = new MainFrame(this);
            main.setVisible(true);
        });

        // start data refresh
        dataThread = new Thread(this::updateModel, "gui-data");
        dataThread.start();

        // start version check
        versionThread = new Thread(this::checkVersion, "gui-version");
        versionThread.start();

        // register shutdown hook
        kernel.reigsterShutdownHook("GUI", this::stop);

        isRunning = true;
    }

    /**
     * Disposes the GUI and release any open resources.
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }

        // stop data refresh thread
        dataThread.interrupt();

        // stop main thread
        versionThread.interrupt();

        isRunning = false;
    }

    /**
     * Starts the version check loop.
     */
    protected void checkVersion() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5L * 60L * 1000L);

                // compare version
                String v = getMinVersion();
                if (v != null && SystemUtil.compareVersion(Constants.CLIENT_VERSION, v) < 0) {
                    JOptionPane.showMessageDialog(null, GUIMessages.get("WalletNeedToBeUpgraded"));
                    SystemUtil.exitAsync(-1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Starts the model update loop.
     */
    protected void updateModel() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(3L * 1000L);

                // process latest block
                processBlock(kernel.getBlockchain().getLatestBlock());
            } catch (InterruptedException e) {
                logger.info("Data refresh interrupted, exiting");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Handles a new block.
     *
     * @param block
     */
    protected void processBlock(Block block) {
        Blockchain chain = kernel.getBlockchain();
        AccountState as = chain.getAccountState();
        DelegateState ds = chain.getDelegateState();

        // update latest block and coinbase delegate status
        model.setSyncProgress(kernel.getSyncManager().getProgress());
        model.setLatestBlock(block);
        model.setDelegate(ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null);

        // refresh accounts
        if (kernel.getWallet().isUnlocked()) {
            List<WalletAccount> accounts = new ArrayList<>();
            for (EdDSA key : kernel.getWallet().getAccounts()) {
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

    /**
     * Set up the Swing look and feel.
     */
    protected static void setupLookAndFeel() {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // do nothing
        }
    }

    /**
     * Returns the min version of semux wallet.
     *
     * @return
     */
    protected String getMinVersion() {
        try {
            URL url = new URL("http://api.semux.org");
            URLConnection con = url.openConnection();
            con.addRequestProperty("User-Agent", Constants.DEFAULT_USER_AGENT);
            con.setConnectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT);
            con.setReadTimeout(Constants.DEFAULT_READ_TIMEOUT);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(con.getInputStream());
            return node.get("minVersion").asText();
        } catch (IOException e) {
            logger.info("Failed to fetch version info");
        }
        return null;
    }

    protected int showSelectDialog(JFrame parent, String message, List<Object> options) {
        return new SelectDialog(parent, message, options).showAndGet();
    }
}
