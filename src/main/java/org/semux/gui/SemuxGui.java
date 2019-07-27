/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.cli.ParseException;
import org.semux.Kernel;
import org.semux.Launcher;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.exception.ConfigException;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Fork;
import org.semux.core.Genesis;
import org.semux.core.Transaction;
import org.semux.core.Wallet;
import org.semux.core.event.WalletLoadingEvent;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.event.PubSub;
import org.semux.event.PubSubFactory;
import org.semux.exception.LauncherException;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.dialog.InitializeHdWalletDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.gui.dialog.SelectDialog;
import org.semux.gui.event.MainFrameStartedEvent;
import org.semux.gui.event.WalletSelectionDialogShownEvent;
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

/**
 * Graphic user interface.
 */
public class SemuxGui extends Launcher {

    public static final boolean ENABLE_HD_WALLET_BY_DEFAULT = true;

    private static final Logger logger = LoggerFactory.getLogger(SemuxGui.class);

    private static final PubSub pubSub = PubSubFactory.getDefault();

    private static final int TRANSACTION_LIMIT = 1024; // per account

    private final WalletModel model;
    private Kernel kernel;

    private AddressBookDialog addressBookDialog;

    private boolean isRunning;

    @SuppressWarnings("unused")
    private SplashScreen splashScreen;
    private JFrame main;
    private Thread dataThread;
    private Thread versionThread;
    private String lastVersionNotified;

    public static void main(String[] args) {
        try {
            // check jvm version
            if (SystemUtil.is32bitJvm()) {
                JOptionPane.showMessageDialog(null, GuiMessages.get("Jvm32NotSupported"));
                SystemUtil.exit(SystemUtil.Code.JVM_32_NOT_SUPPORTED);
            }

            // check system prerequisite
            checkPrerequisite();

            // setup default look and feel
            setupLookAndFeel();

            // start GUI
            SemuxGui gui = new SemuxGui();
            gui.setupLogger(args);
            gui.setupPubSub();
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
        SystemUtil.setLocale(getConfig().uiLocale());
        SwingUtil.setDefaultFractionDigits(getConfig().uiFractionDigits());
        SwingUtil.setDefaultUnit(getConfig().uiUnit());

        this.model = new WalletModel(getConfig());
    }

    /**
     * Creates a GUI instance with the given model and kernel, for test purpose
     * only.
     *
     * @param model
     * @param kernel
     */
    public SemuxGui(WalletModel model, Kernel kernel) {
        SystemUtil.setLocale(getConfig().uiLocale());

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

        // create/unlock wallet
        Wallet wallet = new Wallet(new File(getDataDir(), "wallet.data"), getConfig().network());
        if (!wallet.exists()) {
            showWelcome(wallet);
        } else {
            checkFilePermissions(wallet);
            unlockWallet(wallet);
        }

        // in case HD wallet is enabled, make sure the seed is properly initialized.
        if (isHdWalletEnabled().orElse(ENABLE_HD_WALLET_BY_DEFAULT)) {
            if (!wallet.isHdWalletInitialized()) {
                initializedHdSeed(wallet);
            }
            if (!wallet.isHdWalletInitialized()) {
                System.exit(SystemUtil.Code.FAILED_TO_INIT_HD_WALLET);
            }
        }

        // add an account is wallet is empty
        if (wallet.size() == 0) {
            Key key;
            if (isHdWalletEnabled().orElse(ENABLE_HD_WALLET_BY_DEFAULT)) {
                key = wallet.addAccountWithNextHdKey();
            } else {
                key = wallet.addAccountRandom();
            }
            wallet.flush();

            logger.info(GuiMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
        }

        // setup splash screen
        setupSplashScreen();

        // setup coinbase & launch kernel
        try {
            int coinbase = setupCoinbase(wallet);
            if (coinbase == -1) {
                SystemUtil.exit(SystemUtil.Code.OK);
            }

            startKernel(getConfig(), wallet, wallet.getAccount(coinbase));
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

    /**
     * Shows the InitializeHdWalletDialog.
     */
    protected void initializedHdSeed(Wallet wallet) {
        InitializeHdWalletDialog dialog = new InitializeHdWalletDialog(wallet, main);
        dialog.setVisible(true);
        dialog.dispose();
    }

    /**
     * Shows the welcome frame.
     */
    protected void showWelcome(Wallet wallet) {
        // start welcome frame
        WelcomeFrame frame = new WelcomeFrame(wallet);
        frame.setVisible(true);

        // wait until done
        frame.join();
        frame.dispose();
    }

    protected void checkFilePermissions(Wallet wallet) throws IOException {
        if (SystemUtil.isPosix()) {
            if (!wallet.isPosixPermissionSecured()) {
                JOptionPane.showMessageDialog(
                        null,
                        GuiMessages.get("WarningWalletPosixPermission"),
                        GuiMessages.get("WarningDialogTitle"),
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    protected void unlockWallet(Wallet wallet) {
        // check empty password
        if (wallet.unlock("")) {
            // do nothing
        } else if (getPassword() != null) {
            if (!wallet.unlock(getPassword())) {
                JOptionPane.showMessageDialog(
                        null,
                        GuiMessages.get("AutomaticUnlockFailed"),
                        GuiMessages.get("ErrorDialogTitle"),
                        JOptionPane.ERROR_MESSAGE);
                SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_UNLOCK_WALLET);
            }
        } else {
            showUnlockDialog(wallet);
        }
    }

    /**
     * Shows the unlock frame, which reads user-entered password and tries to unlock
     * the wallet.
     */
    protected void showUnlockDialog(Wallet wallet) {
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
    }

    /**
     * Select an account as coinbase if the wallet is not empty; or create a new
     * account and use it as coinbase.
     */
    protected int setupCoinbase(Wallet wallet) {
        pubSub.publish(new WalletLoadingEvent());

        // use the coinbase specified in arguments
        if (getCoinbase() != null && getCoinbase() >= 0 && getCoinbase() < wallet.size()) {
            return getCoinbase();
        }

        // use the first account
        if (wallet.size() == 1) {
            return 0;
        }

        String message = GuiMessages.get("AccountSelection");
        List<Object> options = new ArrayList<>();
        List<Key> list = wallet.getAccounts();
        for (Key key : list) {
            Optional<String> name = wallet.getAddressAlias(key.toAddress());
            options.add(Hex.PREF + key.toAddressString() + (name.map(s -> ", " + s).orElse("")));
        }

        // show select dialog
        pubSub.publish(new WalletSelectionDialogShownEvent());
        return showSelectDialog(null, message, options);
    }

    protected synchronized void setupSplashScreen() {
        splashScreen = new SplashScreen();
    }

    /**
     * Starts the kernel and shows main frame.
     */
    protected synchronized void startKernel(Config config, Wallet wallet, Key coinbase) {
        if (isRunning) {
            return;
        }

        // set up model
        model.setCoinbase(coinbase);

        // set up kernel
        kernel = new Kernel(config, Genesis.load(config.network()), wallet, coinbase);
        kernel.start();

        // initialize the model with latest block
        updateModel();

        // start main frame
        EventQueue.invokeLater(() -> {
            main = new MainFrame(this);
            main.setVisible(true);
            pubSub.publish(new MainFrameStartedEvent());

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
                Version v = getCurrentVersions();
                if (v != null && SystemUtil.compareVersion(Constants.CLIENT_VERSION, v.minVersion) < 0) {
                    JOptionPane.showMessageDialog(null, GuiMessages.get("WalletNeedToBeUpgraded"));
                    SystemUtil.exitAsync(SystemUtil.Code.CLIENT_UPGRADE_NEEDED);
                    return;
                }
                // notify user if a new version has been posted (once)
                if (v != null && SystemUtil.compareVersion(Constants.CLIENT_VERSION, v.latestVersion) < 0
                        && !v.latestVersion.equals(lastVersionNotified)) {
                    JOptionPane.showMessageDialog(null, GuiMessages.get("WalletCanBeUpgraded"));
                    lastVersionNotified = v.latestVersion;
                }

                Thread.sleep(8L * 60L * 60L * 1000L); // 8 hours
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
        List<String> validators = chain.getValidators();
        Map<String, Integer> validatorPositionMap = IntStream
                .range(0, validators.size())
                .boxed()
                .collect(Collectors.toMap(validators::get, i -> i));
        for (Delegate d : ds.getDelegates()) {
            wds.add(validatorPositionMap.containsKey(d.getAddressString())
                    ? new WalletDelegate(d, validatorPositionMap.containsKey(d.getAddressString()),
                            validatorPositionMap.get(d.getAddressString()))
                    : new WalletDelegate(d));
        }
        model.setDelegates(wds);

        // update validators
        model.setValidators(validators);
        model.setUniformDistributionEnabled(chain.isForkActivated(Fork.UNIFORM_DISTRIBUTION));

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
    protected Version getCurrentVersions() {
        try {
            URL url = new URL("http://api.semux.org/version");
            URLConnection con = url.openConnection();
            con.addRequestProperty("User-Agent", Constants.DEFAULT_USER_AGENT);
            con.setConnectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT);
            con.setReadTimeout(Constants.DEFAULT_READ_TIMEOUT);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(con.getInputStream());
            String minVersion = node.get("minVersion").asText();
            String latestVersion = node.get("latestVersion").asText();
            return new Version(minVersion, latestVersion);
        } catch (IOException e) {
            logger.info("Failed to fetch version info");
        }
        return null;
    }

    protected int showSelectDialog(JFrame parent, String message, List<Object> options) {
        return new SelectDialog(parent, message, options).showAndGet();
    }

    protected static class Version {
        public final String minVersion;
        public final String latestVersion;

        public Version(String minVersion, String latestVersion) {
            this.minVersion = minVersion;
            this.latestVersion = latestVersion;
        }
    }
}
