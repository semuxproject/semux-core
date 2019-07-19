/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import java.io.File;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.semux.Kernel;
import org.semux.Launcher;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.exception.ConfigException;
import org.semux.core.Genesis;
import org.semux.core.Wallet;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.semux.exception.LauncherException;
import org.semux.message.CliMessages;
import org.semux.net.filter.exception.IpFilterJsonParseException;
import org.semux.util.ConsoleUtil;
import org.semux.util.SystemUtil;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Semux command line interface.
 */
public class SemuxCli extends Launcher {

    public static final boolean HD_WALLET_ENABLED = true;

    private static final Logger logger = LoggerFactory.getLogger(SemuxCli.class);

    public static void main(String[] args, SemuxCli cli) {
        try {
            // check jvm version
            if (SystemUtil.is32bitJvm()) {
                logger.error(CliMessages.get("Jvm32NotSupported"));
                SystemUtil.exit(SystemUtil.Code.JVM_32_NOT_SUPPORTED);
            }

            // system system prerequisites
            checkPrerequisite();

            // start CLI
            cli.setupLogger(args);
            cli.start(args);

        } catch (LauncherException | ConfigException | IpFilterJsonParseException | IOException exception) {
            logger.error(exception.getMessage());
        } catch (ParseException exception) {
            logger.error(CliMessages.get("ParsingFailed", exception.getMessage()));
        }
    }

    public static void main(String[] args) {
        main(args, new SemuxCli());
    }

    /**
     * Creates a new Semux CLI instance.
     */
    public SemuxCli() {
        SystemUtil.setLocale(getConfig().uiLocale());

        Option helpOption = Option.builder()
                .longOpt(SemuxOption.HELP.toString())
                .desc(CliMessages.get("PrintHelp"))
                .build();
        addOption(helpOption);

        Option versionOption = Option.builder()
                .longOpt(SemuxOption.VERSION.toString())
                .desc(CliMessages.get("ShowVersion"))
                .build();
        addOption(versionOption);

        Option accountOption = Option.builder()
                .longOpt(SemuxOption.ACCOUNT.toString())
                .desc(CliMessages.get("ChooseAction"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("action").type(String.class)
                .build();
        addOption(accountOption);

        Option changePasswordOption = Option.builder()
                .longOpt(SemuxOption.CHANGE_PASSWORD.toString()).desc(CliMessages.get("ChangeWalletPassword")).build();
        addOption(changePasswordOption);

        Option dumpPrivateKeyOption = Option.builder()
                .longOpt(SemuxOption.DUMP_PRIVATE_KEY.toString())
                .desc(CliMessages.get("PrintHexKey"))
                .hasArg(true).optionalArg(false).argName("address").type(String.class)
                .build();
        addOption(dumpPrivateKeyOption);

        Option importPrivateKeyOption = Option.builder()
                .longOpt(SemuxOption.IMPORT_PRIVATE_KEY.toString())
                .desc(CliMessages.get("ImportHexKey"))
                .hasArg(true).optionalArg(false).argName("key").type(String.class)
                .build();
        addOption(importPrivateKeyOption);
    }

    public void start(String[] args) throws ParseException, IOException {
        // parse common options
        CommandLine cmd = parseOptions(args);

        // parse remaining options
        if (cmd.hasOption(SemuxOption.HELP.toString())) {
            printHelp();

        } else if (cmd.hasOption(SemuxOption.VERSION.toString())) {
            printVersion();

        } else if (cmd.hasOption(SemuxOption.ACCOUNT.toString())) {
            String action = cmd.getOptionValue(SemuxOption.ACCOUNT.toString()).trim();
            if ("create".equals(action)) {
                createAccount();
            } else if ("list".equals(action)) {
                listAccounts();
            }

        } else if (cmd.hasOption(SemuxOption.CHANGE_PASSWORD.toString())) {
            changePassword();

        } else if (cmd.hasOption(SemuxOption.DUMP_PRIVATE_KEY.toString())) {
            dumpPrivateKey(cmd.getOptionValue(SemuxOption.DUMP_PRIVATE_KEY.toString()).trim());

        } else if (cmd.hasOption(SemuxOption.IMPORT_PRIVATE_KEY.toString())) {
            importPrivateKey(cmd.getOptionValue(SemuxOption.IMPORT_PRIVATE_KEY.toString()).trim());

        } else {
            start();
        }
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(200);
        formatter.printHelp("./semux-cli.sh [options]", getOptions());
    }

    protected void printVersion() {
        System.out.println(Constants.CLIENT_VERSION);
    }

    protected void start() throws IOException {
        // create/unlock wallet
        Wallet wallet = loadWallet().exists() ? loadAndUnlockWallet() : createNewWallet();
        if (wallet == null) {
            return;
        }

        // check file permissions
        if (SystemUtil.isPosix()) {
            if (!wallet.isPosixPermissionSecured()) {
                logger.warn(CliMessages.get("WarningWalletPosixPermission"));
            }
        }

        // check time drift
        long timeDrift = TimeUtil.getTimeOffsetFromNtp();
        if (Math.abs(timeDrift) > 5000L) {
            logger.warn(CliMessages.get("SystemTimeDrift"));
        }

        // in case HD wallet is enabled, make sure the seed is properly initialized.
        if (HD_WALLET_ENABLED) {
            if (!wallet.isHdWalletInitialized()) {
                initializedHdSeed(wallet);
            }
        }

        // create a new account if the wallet is empty
        List<Key> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            Key key;
            if (HD_WALLET_ENABLED) {
                key = wallet.addAccountWithNextHdKey();
            } else {
                key = wallet.addAccountRandom();
            }
            wallet.flush();

            accounts = wallet.getAccounts();
            logger.info(CliMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
        }

        // check coinbase if the user specifies one
        int coinbase = getCoinbase() == null ? 0 : getCoinbase();
        if (coinbase < 0 || coinbase >= accounts.size()) {
            logger.error(CliMessages.get("CoinbaseDoesNotExist"));
            exit(SystemUtil.Code.ACCOUNT_NOT_EXIST);
            return;
        }

        // start kernel
        try {
            startKernel(getConfig(), wallet, wallet.getAccount(coinbase));
        } catch (Exception e) {
            logger.error("Uncaught exception during kernel startup.", e);
            exit(SystemUtil.Code.FAILED_TO_LAUNCH_KERNEL);
        }
    }

    /**
     * Starts the kernel.
     */
    protected Kernel startKernel(Config config, Wallet wallet, Key coinbase) {
        Kernel kernel = new Kernel(config, Genesis.load(config.network()), wallet, coinbase);
        kernel.start();

        return kernel;
    }

    protected void createAccount() {
        Wallet wallet = loadAndUnlockWallet();

        Key key;
        if (HD_WALLET_ENABLED) {
            key = wallet.addAccountWithNextHdKey();
        } else {
            key = wallet.addAccountRandom();
        }

        if (wallet.flush()) {
            logger.info(CliMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
            logger.info(CliMessages.get("PublicKey", Hex.encode(key.getPublicKey())));
        }
    }

    protected void listAccounts() {
        Wallet wallet = loadAndUnlockWallet();

        List<Key> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            logger.info(CliMessages.get("AccountMissing"));
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                logger.info(CliMessages.get("ListAccountItem", i, accounts.get(i).toString()));
            }
        }
    }

    protected void changePassword() {
        Wallet wallet = loadAndUnlockWallet();

        try {
            String newPassword = readNewPassword("EnterNewPassword", "ReEnterNewPassword");
            if (newPassword == null) {
                return;
            }

            wallet.changePassword(newPassword);
            boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error(CliMessages.get("WalletFileCannotBeUpdated"));
                exit(SystemUtil.Code.FAILED_TO_WRITE_WALLET_FILE);
                return;
            }

            logger.info(CliMessages.get("PasswordChangedSuccessfully"));
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    protected void exit(int code) {
        SystemUtil.exit(code);
    }

    protected String readPassword() {
        return ConsoleUtil.readPassword();
    }

    protected String readPassword(String prompt) {
        return ConsoleUtil.readPassword(prompt);
    }

    /**
     * Read a new password from input and require confirmation
     *
     * @return new password, or null if the confirmation failed
     */
    protected String readNewPassword(String newPasswordMessageKey, String reEnterNewPasswordMessageKey) {
        String newPassword = readPassword(CliMessages.get(newPasswordMessageKey));
        String newPasswordRe = readPassword(CliMessages.get(reEnterNewPasswordMessageKey));

        if (!newPassword.equals(newPasswordRe)) {
            logger.error(CliMessages.get("ReEnterNewPasswordIncorrect"));
            exit(SystemUtil.Code.PASSWORD_REPEAT_NOT_MATCH);
            return null;
        }

        return newPassword;
    }

    protected void dumpPrivateKey(String address) {
        Wallet wallet = loadAndUnlockWallet();

        byte[] addressBytes = Hex.decode0x(address);
        Key account = wallet.getAccount(addressBytes);
        if (account == null) {
            logger.error(CliMessages.get("AddressNotInWallet"));
            exit(SystemUtil.Code.ACCOUNT_NOT_EXIST);
        } else {
            System.out.println(CliMessages.get("PrivateKeyIs", Hex.encode(account.getPrivateKey())));
        }
    }

    protected void importPrivateKey(String key) {
        try {
            Wallet wallet = loadAndUnlockWallet();
            byte[] keyBytes = Hex.decode0x(key);
            Key account = new Key(keyBytes);

            boolean accountAdded = wallet.addAccount(account);
            if (!accountAdded) {
                logger.error(CliMessages.get("PrivateKeyAlreadyInWallet"));
                exit(SystemUtil.Code.ACCOUNT_ALREADY_EXISTS);
                return;
            }

            boolean walletFlushed = wallet.flush();
            if (!walletFlushed) {
                logger.error(CliMessages.get("WalletFileCannotBeUpdated"));
                exit(SystemUtil.Code.FAILED_TO_WRITE_WALLET_FILE);
                return;
            }

            logger.info(CliMessages.get("PrivateKeyImportedSuccessfully"));
            logger.info(CliMessages.get("Address", account.toAddressString()));
            logger.info(CliMessages.get("PublicKey", Hex.encode(account.getPublicKey())));
        } catch (InvalidKeySpecException exception) {
            logger.error(CliMessages.get("PrivateKeyCannotBeDecoded", exception.getMessage()));
            exit(SystemUtil.Code.INVALID_PRIVATE_KEY);
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
            exit(SystemUtil.Code.WALLET_LOCKED);
        }
    }

    protected Wallet loadAndUnlockWallet() {

        Wallet wallet = loadWallet();
        if (getPassword() == null) {
            if (wallet.unlock("")) {
                setPassword("");
            } else {
                setPassword(readPassword());
            }
        }

        if (!wallet.unlock(getPassword())) {
            logger.error("Invalid password");
            exit(SystemUtil.Code.FAILED_TO_UNLOCK_WALLET);
        }

        return wallet;
    }

    /**
     * Create a new wallet with a new password from input and save the wallet file
     * to disk
     *
     * @return created new wallet, or null if it failed to create the wallet
     */
    protected Wallet createNewWallet() {
        String newPassword = readNewPassword("EnterNewPassword", "ReEnterNewPassword");
        if (newPassword == null) {
            return null;
        }

        setPassword(newPassword);
        Wallet wallet = loadWallet();
        if (!wallet.unlock(newPassword) || !wallet.flush()) {
            logger.error("CreateNewWalletError");
            exit(SystemUtil.Code.FAILED_TO_WRITE_WALLET_FILE);
            return null;
        }

        return wallet;
    }

    protected Wallet loadWallet() {
        return new Wallet(new File(getDataDir(), "wallet.data"), getConfig().network());
    }

    protected void initializedHdSeed(Wallet wallet) {
        if (wallet.isUnlocked() && !wallet.isHdWalletInitialized()) {
            // HD Mnemonic
            MnemonicGenerator generator = new MnemonicGenerator();
            String phrase = generator.getWordlist(Wallet.MNEMONIC_ENTROPY_LENGTH, Wallet.MNEMONIC_LANGUAGE);
            System.out.println(CliMessages.get("HdWalletInstructions", phrase));

            String repeat = ConsoleUtil.readLine(CliMessages.get("HdWalletMnemonicRepeat"));
            if (!repeat.equals(phrase)) {
                logger.info(CliMessages.get("HdWalletInitializationFailure"));
                SystemUtil.exit(SystemUtil.Code.FAILED_TO_INIT_HD_WALLET);
                return;
            }

            wallet.initializeHdWallet(phrase);
            wallet.flush();
            logger.error(CliMessages.get("HdWalletInitializationSuccess"));
        }
    }
}