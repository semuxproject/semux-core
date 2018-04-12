/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.cli.ParseException;
import org.semux.Kernel;
import org.semux.Launcher;
import org.semux.config.Constants;
import org.semux.config.exception.ConfigException;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Transaction;
import org.semux.core.Wallet;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.exception.LauncherException;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.dialog.SelectDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletDelegate;
import org.semux.gui.model.WalletModel;
import org.semux.gui.model.WalletModel.Status;
import org.semux.message.GuiMessages;
import org.semux.net.Peer;
import org.semux.net.filter.exception.IpFilterJsonParseException;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

/**
 * Graphic user interface.
 */
public class SemuxGui extends Launcher {

    private static final Logger logger = LoggerFactory.getLogger(SemuxGui.class);

    private static final int TRANSACTION_LIMIT = 1024; // per account

    private WalletModel model;
    private Kernel kernel;

    private AddressBookDialog addressBookDialog;

    private boolean isRunning;

    @SuppressWarnings("unused")
    private SplashScreen splashScreen;
    private JFrame main;
    private Thread dataThread;
    private Thread versionThread;

    public static void main(String[] args) {
        try {
            setupLookAndFeel();

            checkPrerequisite();

            SemuxGui gui = new SemuxGui();
            // set up logger
            gui.setupLogger(args);
            // start
            gui.start(args);

        } catch (LauncherException | ConfigException | IpFilterJsonParseException | IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    GuiMessages.get("ErrorDialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Filed to parse the parameters: " + e.getMessage(),
                    GuiMessages.get("ErrorDialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates a new Semux GUI instance.
     */
    public SemuxGui() {
        SystemUtil.setLocale(getConfig().locale());
        SwingUtil.setDefaultFractionDigits(getConfig().uiFractionDigits());
        SwingUtil.setDefaultUnit(getConfig().uiUnit());

        this.model = new WalletModel();
    }

    /**
     * Creates a GUI instance with the given model and kernel, for test purpose
     * only.
     *
     * @param model
     * @param kernel
     */
    public SemuxGui(WalletModel model, Kernel kernel) {
        SystemUtil.setLocale(getConfig().locale());

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
     * Returns the address book dialog.
     * 
     * @return
     */
    public AddressBookDialog getAddressBookDialog() {
        return addressBookDialog;
    }

    /**
     * Starts GUI with the given command line arguments.
     *
     * @param args
     * @throws ParseException
     */
    public void start(String[] args) throws ParseException, IOException {
        // parse options
        parseOptions(args);

        // create a wallet instance.
        Wallet wallet = new Wallet(new File(getDataDir(), "wallet.data"));

        if (!wallet.exists()) {
            showWelcome(wallet);
        } else {
            checkWalletPermissions(wallet);
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

        showSplashScreen();
        setupCoinbase(wallet);
    }

    public void checkWalletPermissions(Wallet wallet) throws IOException {
        if (SystemUtil.isPosix() && !wallet.checkPosixPermission()) {
            JOptionPane.showMessageDialog(
                    null,
                    GuiMessages.get("WarningWalletPosixPermission"),
                    GuiMessages.get("WarningDialogTitle"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Shows the unlock frame, which reads user-entered password and tries to unlock
     * the wallet.
     */
    public void showUnlock(Wallet wallet) {
        for (int i = 0;; i++) {
            InputDialog dialog = new InputDialog(null, i == 0 ? GuiMessages.get("EnterPassword") + ":"
                    : GuiMessages.get("WrongPasswordPleaseTryAgain") + ":", true);
            String pwd = dialog.showAndGet();

            if (pwd == null) {
                SystemUtil.exitAsync(SystemUtil.Code.OK);
            } else if (wallet.unlock(pwd)) {
                break;
            }
        }

        showSplashScreen();
        setupCoinbase(wallet);
    }

    /**
     * Select an account as coinbase if the wallet is not empty; or create a new
     * account and use it as coinbase.
     */
    public void setupCoinbase(Wallet wallet) {
        model.fireSemuxEvent(SemuxEvent.WALLET_LOADING);
        if (wallet.size() > 1) {
            String message = GuiMessages.get("AccountSelection");
            List<Object> options = new ArrayList<>();
            List<Key> list = wallet.getAccounts();
            for (Key key : list) {
                Optional<String> name = wallet.getAddressAlias(key.toAddress());
                options.add(Hex.PREF + key.toAddressString() + (name.map(s -> ", " + s).orElse("")));
            }

            // show select dialog
            model.fireSemuxEvent(SemuxEvent.GUI_WALLET_SELECTION_DIALOG_SHOWN);
            int index = showSelectDialog(null, message, options);

            if (index == -1) {
                SystemUtil.exitAsync(SystemUtil.Code.OK);
            } else {
                // use the selected account as coinbase.
                setCoinbase(index);
            }
        } else if (wallet.size() == 0) {
            wallet.addAccount(new Key());
            wallet.flush();

            // use the first account as coinbase.
            setCoinbase(0);
        }

        try {
            startKernelAndMain(wallet);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    GuiMessages.get("ErrorDialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Uncaught exception during kernel startup.", e);
            SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_LAUNCH_KERNEL);
        }
    }

    private synchronized void showSplashScreen() {
        splashScreen = new SplashScreen(model);
    }

    /**
     * Starts the kernel and shows main frame.
     */
    public synchronized void startKernelAndMain(Wallet wallet) {
        if (isRunning) {
            return;
        }

        // set up model
        model.setCoinbase(wallet.getAccount(getCoinbase()));

        // set up kernel
        model.fireSemuxEvent(SemuxEvent.KERNEL_STARTING);
        kernel = new Kernel(getConfig(), wallet, wallet.getAccount(getCoinbase()));
        kernel.start();

        // initialize the model with latest block
        updateModel();

        // start main frame
        EventQueue.invokeLater(() -> {
            main = new MainFrame(this);
            main.setVisible(true);
            model.fireSemuxEvent(SemuxEvent.GUI_MAINFRAME_STARTED);

            addressBookDialog = new AddressBookDialog(main, kernel.getWallet(), this);
            model.addListener(ev -> addressBookDialog.refresh());
        });

        // start data refresh
        dataThread = new Thread(this::updateModelLoop, "gui-data");
        dataThread.start();

        // start version check
        versionThread = new Thread(this::checkVersionLoop, "gui-version");
        versionThread.start();

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

        // wait until all threads are stopped
        while (dataThread.isAlive() || versionThread.isAlive()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Failed to stop data/version threads", e);
            }
        }

        isRunning = false;
    }

    /**
     * Starts the version check loop.
     */
    protected void checkVersionLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5L * 60L * 1000L);

                // compare version
                String v = getMinVersion();
                if (v != null && SystemUtil.compareVersion(Constants.CLIENT_VERSION, v) < 0) {
                    JOptionPane.showMessageDialog(null, GuiMessages.get("WalletNeedToBeUpgraded"));
                    SystemUtil.exitAsync(SystemUtil.Code.CLIENT_UPGRADE_NEEDED);
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
    protected void updateModelLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5L * 1000L);

                // process latest block
                updateModel();
            } catch (InterruptedException e) {
                logger.info("Data refresh interrupted, exiting");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Update the model.
     */
    public void updateModel() {
        Blockchain chain = kernel.getBlockchain();
        AccountState as = chain.getAccountState();
        DelegateState ds = chain.getDelegateState();
        Block block = kernel.getBlockchain().getLatestBlock();

        // update latest block and coinbase delegate status
        model.setSyncProgress(kernel.getSyncManager().getProgress());
        model.setLatestBlock(block);

        // update coinbase
        boolean isDelegate = ds.getDelegateByAddress(kernel.getCoinbase().toAddress()) != null;
        boolean isValidator = chain.getValidators().contains(kernel.getCoinbase().toAddressString());
        model.setCoinbase(kernel.getCoinbase());
        model.setStatus(isValidator ? Status.VALIDATOR : (isDelegate ? Status.DELEGATE : Status.NORMAL));

        // refresh accounts
        if (kernel.getWallet().isUnlocked()) {
            List<WalletAccount> accounts = new ArrayList<>();
            for (Key key : kernel.getWallet().getAccounts()) {
                Account a = as.getAccount(key.toAddress());
                Optional<String> name = kernel.getWallet().getAddressAlias(key.toAddress());
                WalletAccount wa = new WalletAccount(key, a, name.orElse(null));
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
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
    }

    /**
     * Returns the min version of semux wallet.
     *
     * @return the min version, or null if failed to retrieve
     */
    protected String getMinVersion() {
        try {
            URL url = new URL("http://api.semux.org/version");
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
