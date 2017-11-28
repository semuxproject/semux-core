/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import java.io.File;
import java.security.spec.InvalidKeySpecException;
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
import org.semux.gui.MessagesUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxCLI {

    private static final Logger logger = LoggerFactory.getLogger(SemuxCLI.class);

    private Options options = new Options();

    static final String DEFAULT_DATA_DIR = ".";
    static final String MSG_ADDRESS_NOT_IN_WALLET = MessagesUtil.get("AddressNotInWallet");
    static final String MSG_ENTER_NEW_PASSWORD = MessagesUtil.get("EnterNewPassword") + ": ";
    static final String MSG_PASSWORD_CHANGED = MessagesUtil.get("PasswordChangedSuccessfully");
    static final String MSG_FAILED_TO_FLUSH_WALLET_FILE = MessagesUtil.get("WalletFileCannotBeUpdated");
    static final String MSG_PRIVATE_KEY_EXISTED = MessagesUtil.get("PrivateKeyAlreadyInWallet");
    static final String MSG_PRIVATE_KEY_IMPORTED = MessagesUtil.get("PrivateKeyImportedSuccessfully");
    static final String MSG_PRIVATE_KEY_SEPC_EXCETION = MessagesUtil.get("PrivateKeyCannotBeDecoded") + " {}";
    static final String MSG_ADDRESS = MessagesUtil.get("Address") + " = {}";
    static final String MSG_PUBLIC_KEY = MessagesUtil.get("PublicKey") + " = {}";
    static final String MSG_PRIVATE_KEY = MessagesUtil.get("PrivateKey") + " = {}";
    static final String MSG_ACCOUNT = MessagesUtil.get("AccountNum") + " = {}";
    static final String MSG_ACCOUNT_EMPTY = MessagesUtil.get("AccountMissing");
    static final String MSG_PARSING_FAILED = MessagesUtil.get("ParsingFailed") + " {}";
    static final String MSG_NEW_ACCOUNT_CREATED = MessagesUtil.get("NewAccountCreated");
    static final String MSG_START_KERNEL_NEW_ACCOUNT_CREATED = MessagesUtil.get("NewAccoutnCreatedForAddress")
            + " = {}";
    static final String MSG_COINBASE_NOT_EXISTED = MessagesUtil.get("CoinbaseDoesNotExist");

    private String dataDir = DEFAULT_DATA_DIR;
    private int coinbase = 0;
    private String password = null;

    SemuxCLI() {
        Option help = Option.builder().longOpt("help").desc(MessagesUtil.get("PrintHelp")).build();
        options.addOption(help);

        Option version = Option.builder().longOpt("version").desc(MessagesUtil.get("ShowVersion")).build();
        options.addOption(version);

        Option account = Option.builder().longOpt("account").desc(MessagesUtil.get("ChooseAction")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("action").type(String.class).build();
        options.addOption(account);

        Option changepassword = Option.builder().longOpt("changepassword")
                .desc(MessagesUtil.get("ChangeWalletPassword")).build();
        options.addOption(changepassword);

        Option datadir = Option.builder().longOpt("datadir").desc(MessagesUtil.get("SpecifyDataDir")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("path").type(String.class).build();
        options.addOption(datadir);

        Option coinbase = Option.builder().longOpt("coinbase").desc(MessagesUtil.get("SpecifyCoinbase")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("index").type(Number.class).build();
        options.addOption(coinbase);

        Option password = Option.builder().longOpt("password").desc(MessagesUtil.get("WalletPassword")).hasArg(true)
                .numberOfArgs(1).optionalArg(false).argName("password").type(String.class).build();
        options.addOption(password);

        Option dumpprivatekey = Option.builder().longOpt("dumpprivatekey").desc(MessagesUtil.get("PrintHexKey"))
                .hasArg(true).optionalArg(false).argName("address").type(String.class).build();
        options.addOption(dumpprivatekey);

        Option importprivatekey = Option.builder().longOpt("importprivatekey").desc(MessagesUtil.get("ImportHexKey"))
                .hasArg(true).optionalArg(false).argName("key").type(String.class).build();
        options.addOption(importprivatekey);
    }

    public static void main(String[] args) {
        try {
            SemuxCLI cli = new SemuxCLI();
            cli.start(args);
        } catch (ParseException exception) {
            logger.error(MSG_PARSING_FAILED, exception.getMessage());
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
        } else if (commandLine.hasOption("importprivatekey")) {
            importPrivateKey(commandLine.getOptionValue("importprivatekey").trim());
        } else {
            startKernel();
        }
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(200);
        formatter.printHelp("./semux-cli.sh [options]", options);
    }

    protected void printVersion() {
        System.out.println(Config.CLIENT_VERSION);
    }

    protected void startKernel() {
        Wallet wallet = loadAndUnlockWallet();

        List<EdDSA> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            EdDSA key = new EdDSA();
            wallet.addAccount(key);
            wallet.flush();
            accounts = wallet.getAccounts();
            logger.info(MSG_START_KERNEL_NEW_ACCOUNT_CREATED, key.toAddressString());
        }

        if (coinbase < 0 || coinbase >= accounts.size()) {
            logger.error(MSG_COINBASE_NOT_EXISTED);
            SystemUtil.exit(-1);
        }

        // start kernel
        Kernel kernel = Kernel.getInstance();
        kernel.init(dataDir, wallet, coinbase);
        kernel.start();
    }

    protected void createAccount() {
        Wallet wallet = loadAndUnlockWallet();

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        if (wallet.flush()) {
            logger.info(MSG_NEW_ACCOUNT_CREATED);
            logger.info(MSG_ADDRESS, key.toString());
            logger.info(MSG_PUBLIC_KEY, Hex.encode(key.getPublicKey()));
            logger.info(MSG_PRIVATE_KEY, Hex.encode(key.getPrivateKey()));
        }
    }

    protected void listAccounts() {
        Wallet wallet = loadAndUnlockWallet();

        List<EdDSA> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            logger.info(MSG_ACCOUNT_EMPTY);
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                logger.info(MSG_ACCOUNT, i, accounts.get(i).toString());
            }
        }
    }

    protected void changePassword() {
        Wallet wallet = loadAndUnlockWallet();

        try {
            String newPassword = SystemUtil.readPassword(MSG_ENTER_NEW_PASSWORD);
            wallet.changePassword(newPassword);
            Boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error(MSG_FAILED_TO_FLUSH_WALLET_FILE);
                SystemUtil.exit(1);
            }

            logger.info(MSG_PASSWORD_CHANGED);
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    protected void dumpPrivateKey(String address) {
        Wallet wallet = loadAndUnlockWallet();

        byte[] addressBytes = Hex.parse(address);
        EdDSA account = wallet.getAccount(addressBytes);
        if (account == null) {
            logger.error(MSG_ADDRESS_NOT_IN_WALLET);
            SystemUtil.exit(1);
        } else {
            String privateKey = Hex.encode(account.getPrivateKey());
            System.out.println(privateKey);
        }
    }

    protected void importPrivateKey(String key) {
        try {
            Wallet wallet = loadAndUnlockWallet();
            byte[] keyBytes = Hex.parse(key);
            EdDSA account = new EdDSA(keyBytes);

            boolean accountAdded = wallet.addAccount(account);
            if (!accountAdded) {
                logger.error(MSG_PRIVATE_KEY_EXISTED);
                SystemUtil.exit(1);
            }

            boolean walletFlushed = wallet.flush();
            if (!walletFlushed) {
                logger.error(MSG_FAILED_TO_FLUSH_WALLET_FILE);
                SystemUtil.exit(2);
            }

            logger.info(MSG_PRIVATE_KEY_IMPORTED);
            logger.info(MSG_ADDRESS, account.toAddressString());
            logger.info(MSG_PUBLIC_KEY, Hex.encode(account.getPublicKey()));
            logger.info(MSG_PRIVATE_KEY, Hex.encode(account.getPrivateKey()));
        } catch (InvalidKeySpecException exception) {
            logger.error(MSG_PRIVATE_KEY_SEPC_EXCETION, exception.getMessage());
            SystemUtil.exit(3);
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
            SystemUtil.exit(-1);
        }
    }

    protected Wallet loadAndUnlockWallet() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(password)) {
            SystemUtil.exit(-1);
        }

        return wallet;
    }

    protected Wallet loadWallet() {
        return new Wallet(new File(dataDir, "wallet.data"));
    }
}