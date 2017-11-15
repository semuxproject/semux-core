/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.core.WalletLockedException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxCLI {

    private static final Logger logger = LoggerFactory.getLogger(SemuxCLI.class);

    private Options options = new Options();

    static final String DEFAULT_DATA_DIR = ".";
    static final String MSG_ADDRESS_NOT_IN_WALLET = "This address doesn't exist in the wallet";
    static final String MSG_ENTER_NEW_PASSWORD = "Please enter the new password: ";
    static final String MSG_FAILED_TO_CHANGE_PASSWORD = "Failed to save the new password";
    static final String MSG_PASSWORD_CHANGED = "Password is successfully changed";

    private String dataDir = DEFAULT_DATA_DIR;
    private int coinbase = 0;
    private String password = null;

    SemuxCLI() {
        // FIXME: the option is redundant in order to avoid ParseException
        Option cli = Option.builder("cli").longOpt("cli").build();
        options.addOption(cli);

        Option help = Option.builder("h").longOpt("help").desc("Print help info and exit").build();
        options.addOption(help);

        Option version = Option.builder("v").longOpt("version").desc("Show the version of this client").build();
        options.addOption(version);

        Option account = Option.builder("a").longOpt("account")
                .desc("action can be one of:" + "\n" + "create - Create an new account and exit" + "\n"
                        + "list - List all accounts and exit")
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("action").type(String.class).build();
        options.addOption(account);

        Option changepassword = Option.builder("cp").longOpt("changepassword").desc("Change password of the wallet")
                .build();
        options.addOption(changepassword);

        Option datadir = Option.builder("d").longOpt("datadir").desc("Specify the data directory").hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("path").type(String.class).build();
        options.addOption(datadir);

        Option coinbase = Option.builder("c").longOpt("coinbase").desc("Specify which account to be used as coinbase")
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("index").type(Number.class).build();
        options.addOption(coinbase);

        Option password = Option.builder("p").longOpt("password").desc("Password of the wallet").hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("password").type(String.class).build();
        options.addOption(password);

        Option dumpprivatekey = Option.builder("dpk").longOpt("dumpprivatekey")
                .desc("Prints the hexadecimal private key of an address").hasArg(true).optionalArg(false)
                .argName("address").type(String.class).build();
        options.addOption(dumpprivatekey);
    }

    public static void main(String[] args) {
        try {
            SemuxCLI cli = new SemuxCLI();
            cli.start(args);
        } catch (ParseException exception) {
            logger.error("Parsing failed. Reason: {}", exception.getMessage());
        }
    }

    public void start(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("datadir")) {
            dataDir = commandLine.getOptionValue("datadir");
        }

        if (commandLine.hasOption("coinbase")) {
            coinbase = ((Number) commandLine.getParsedOptionValue("coinbase")).intValue();
        }

        if (commandLine.hasOption("password")) {
            password = commandLine.getOptionValue("password");
        }

        if (commandLine.hasOption("help")) {
            printHelp();
        } else if (commandLine.hasOption("version")) {
            printVersion();
        } else if (commandLine.hasOption("account")) {
            String accountAction = commandLine.getOptionValue("account").trim();
            if (accountAction.equals("create")) {
                createAccount();
            } else if (accountAction.equals("list")) {
                listAccounts();
            }
        } else if (commandLine.hasOption("changepassword")) {
            changePassword();
        } else if (commandLine.hasOption("dumpprivatekey")) {
            dumpPrivateKey(commandLine.getOptionValue("dumpprivatekey").trim());
        } else {
            startKernel();
        }
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(200);
        formatter.printHelp("./semux.sh --cli [options] or semux.bat [options]", options);
    }

    protected void printVersion() {
        System.out.println(Config.CLIENT_VERSION);
    }

    protected void startKernel() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            EdDSA key = new EdDSA();
            wallet.addAccount(key);
            wallet.flush();
            accounts = wallet.getAccounts();
            logger.info("A new account has been created for you: address = {}", key.toAddressString());
        }

        if (coinbase < 0 || coinbase >= accounts.size()) {
            logger.error("Coinbase does not exist");
            System.exit(-1);
        }

        // start kernel
        Kernel kernel = Kernel.getInstance();
        kernel.init(dataDir, wallet, coinbase);
        kernel.start();
    }

    protected void createAccount() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        if (wallet.flush()) {
            logger.info("A new account has been created and stored in your wallet.");
            logger.info("Address = {}", key.toString());
            logger.info("Public key = {}", Hex.encode(key.getPublicKey()));
            logger.info("Private key = {}", Hex.encode(key.getPrivateKey()));
        }
    }

    protected void listAccounts() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            logger.info("There is no account in your wallet!");
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                logger.info(String.format("Account #{} = {}", i, accounts.get(i).toString()));
            }
        }
    }

    protected void changePassword() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        try {
            String newPassword = SystemUtil.readPassword(MSG_ENTER_NEW_PASSWORD);
            wallet.changePassword(newPassword);
            Boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error(MSG_FAILED_TO_CHANGE_PASSWORD);
            } else {
                logger.info(MSG_PASSWORD_CHANGED);
            }
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    protected void dumpPrivateKey(String address) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        byte[] addressBytes = Hex.parse(address);
        EdDSA account = wallet.getAccount(addressBytes);
        if (account == null) {
            logger.error(MSG_ADDRESS_NOT_IN_WALLET);
            System.exit(1);
        }

        String privateKey = Hex.encode(account.getPrivateKey());
        System.out.println(privateKey);
    }

    protected Wallet loadWallet() {
        return new Wallet(new File(dataDir, "wallet.data"));
    }
}
