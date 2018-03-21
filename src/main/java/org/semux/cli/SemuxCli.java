/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import java.io.File;
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
import org.semux.core.Wallet;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.exception.LauncherException;
import org.semux.message.CliMessages;
import org.semux.net.filter.exception.IpFilterJsonParseException;
import org.semux.util.ConsoleUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Semux command line interface.
 */
public class SemuxCli extends Launcher {

    private static final Logger logger = LoggerFactory.getLogger(SemuxCli.class);

    public static void main(String[] args) {
        try {
            checkPrerequisite();

            SemuxCli cli = new SemuxCli();
            // set up logger
            cli.setupLogger(args);
            // start
            cli.start(args);
        } catch (LauncherException | ConfigException | IpFilterJsonParseException exception) {
            logger.error(exception.getMessage());
        } catch (ParseException exception) {
            logger.error(CliMessages.get("ParsingFailed", exception.getMessage()));
        }
    }

    /**
     * Creates a new Semux CLI instance.
     */
    public SemuxCli() {
        SystemUtil.setLocale(getConfig().locale());

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

        Option coinbaseOption = Option.builder()
                .longOpt(SemuxOption.COINBASE.toString()).desc(CliMessages.get("SpecifyCoinbase"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("index").type(Number.class)
                .build();
        addOption(coinbaseOption);

        Option passwordOption = Option.builder()
                .longOpt(SemuxOption.PASSWORD.toString()).desc(CliMessages.get("WalletPassword"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName(SemuxOption.PASSWORD.toString())
                .type(String.class)
                .build();
        addOption(passwordOption);

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

    public void start(String[] args) throws ParseException {
        // parse options
        CommandLine cmd = parseOptions(args);

        if (cmd.hasOption(SemuxOption.COINBASE.toString())) {
            setCoinbase(((Number) cmd.getParsedOptionValue(SemuxOption.COINBASE.toString())).intValue());
        }

        if (cmd.hasOption(SemuxOption.PASSWORD.toString())) {
            setPassword(cmd.getOptionValue(SemuxOption.PASSWORD.toString()));
        }

        if (cmd.hasOption(SemuxOption.HELP.toString())) {
            printHelp();
        } else if (cmd.hasOption(SemuxOption.VERSION.toString())) {
            printVersion();
        } else if (cmd.hasOption(SemuxOption.ACCOUNT.toString())) {
            String accountAction = cmd.getOptionValue(SemuxOption.ACCOUNT.toString()).trim();
            if (accountAction.equals("create")) {
                createAccount();
            } else if (accountAction.equals("list")) {
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

    protected void start() {
        // load wallet file
        Wallet wallet = loadWallet().exists() ? loadAndUnlockWallet() : createNewWallet();
        if (wallet == null) {
            return;
        }

        // create a new account if the wallet is empty
        List<Key> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            Key key = new Key();
            wallet.addAccount(key);
            wallet.flush();
            accounts = wallet.getAccounts();
            logger.info(CliMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
        }

        if (getCoinbase() < 0 || getCoinbase() >= accounts.size()) {
            logger.error(CliMessages.get("CoinbaseDoesNotExist"));
            SystemUtil.exit(-1);
            return;
        }

        // start kernel
        try {
            startKernel(getConfig(), wallet, wallet.getAccount(getCoinbase()));
        } catch (Exception e) {
            logger.error("Uncaught exception during kernel startup.", e);
            SystemUtil.exitAsync(-1);
        }
    }

    protected Kernel startKernel(Config config, Wallet wallet, Key coinbase) {
        Kernel kernel = new Kernel(config, wallet, coinbase);
        kernel.start();

        return kernel;
    }

    protected void createAccount() {
        Wallet wallet = loadAndUnlockWallet();

        Key key = new Key();
        wallet.addAccount(key);

        if (wallet.flush()) {
            logger.info(CliMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
            logger.info(CliMessages.get("PublicKey", Hex.encode(key.getPublicKey())));
            logger.info(CliMessages.get("PrivateKey", Hex.encode(key.getPrivateKey())));
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
            String newPassword = readNewPassword();
            if (newPassword == null) {
                return;
            }

            wallet.changePassword(newPassword);
            Boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error(CliMessages.get("WalletFileCannotBeUpdated"));
                SystemUtil.exit(1);
                return;
            }

            logger.info(CliMessages.get("PasswordChangedSuccessfully"));
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    /**
     * Read a new password from input and require confirmation
     * 
     * @return new password, or null if the confirmation failed
     */
    private String readNewPassword() {
        String newPassword = ConsoleUtil.readPassword(CliMessages.get("EnterNewPassword"));
        String newPasswordRe = ConsoleUtil.readPassword(CliMessages.get("ReEnterNewPassword"));

        if (!newPassword.equals(newPasswordRe)) {
            logger.error(CliMessages.get("ReEnterNewPasswordIncorrect"));
            SystemUtil.exit(1);
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
            SystemUtil.exit(1);
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
                SystemUtil.exit(1);
                return;
            }

            boolean walletFlushed = wallet.flush();
            if (!walletFlushed) {
                logger.error(CliMessages.get("WalletFileCannotBeUpdated"));
                SystemUtil.exit(2);
                return;
            }

            logger.info(CliMessages.get("PrivateKeyImportedSuccessfully"));
            logger.info(CliMessages.get("Address", account.toAddressString()));
            logger.info(CliMessages.get("PublicKey", Hex.encode(account.getPublicKey())));
            logger.info(CliMessages.get("PrivateKey", Hex.encode(account.getPrivateKey())));
        } catch (InvalidKeySpecException exception) {
            logger.error(CliMessages.get("PrivateKeyCannotBeDecoded", exception.getMessage()));
            SystemUtil.exit(3);
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
            SystemUtil.exit(-1);
        }
    }

    protected Wallet loadAndUnlockWallet() {
        if (getPassword() == null) {
            setPassword(ConsoleUtil.readPassword());
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(getPassword())) {
            SystemUtil.exit(-1);
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
        String newPassword = readNewPassword();
        if (newPassword == null) {
            return null;
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(newPassword) || !wallet.flush()) {
            logger.error("CreateNewWalletError");
            SystemUtil.exit(-1);
            return null;
        }

        return wallet;
    }

    protected Wallet loadWallet() {
        return new Wallet(new File(getDataDir(), "wallet.data"));
    }

}