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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.semux.Kernel;
import org.semux.Launcher;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.core.Wallet;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.message.CLIMessages;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Semux command line interface.
 */
public class SemuxCLI extends Launcher {

    private static final Logger logger = LoggerFactory.getLogger(SemuxCLI.class);

    public static void main(String[] args) {
        try {
            SemuxCLI cli = new SemuxCLI();
            // set up logger
            cli.setupLogger(args);
            // start
            cli.start(args);
        } catch (ParseException exception) {
            logger.error(CLIMessages.get("ParsingFailed", exception.getMessage()));
        }
    }

    /**
     * Creates a new Semux CLI instance.
     */
    public SemuxCLI() {
        Option helpOption = Option.builder()
                .longOpt(SemuxOption.HELP.toString())
                .desc(CLIMessages.get("PrintHelp"))
                .build();
        addOption(helpOption);

        Option versionOption = Option.builder()
                .longOpt(SemuxOption.VERSION.toString())
                .desc(CLIMessages.get("ShowVersion"))
                .build();
        addOption(versionOption);

        Option accountOption = Option.builder()
                .longOpt(SemuxOption.ACCOUNT.toString())
                .desc(CLIMessages.get("ChooseAction"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("action").type(String.class)
                .build();
        addOption(accountOption);

        Option changePasswordOption = Option.builder()
                .longOpt(SemuxOption.CHANGE_PASSWORD.toString()).desc(CLIMessages.get("ChangeWalletPassword")).build();
        addOption(changePasswordOption);

        Option dataDirOption = Option.builder()
                .longOpt(SemuxOption.DATA_DIR.toString())
                .desc(CLIMessages.get("SpecifyDataDir"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("path").type(String.class)
                .build();
        addOption(dataDirOption);

        Option coinbaseOption = Option.builder()
                .longOpt(SemuxOption.COINBASE.toString()).desc(CLIMessages.get("SpecifyCoinbase"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("index").type(Number.class)
                .build();
        addOption(coinbaseOption);

        Option networkOption = Option.builder()
                .longOpt(SemuxOption.NETWORK.toString()).desc(CLIMessages.get("SpecifyNetwork"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("name").type(String.class)
                .build();
        addOption(networkOption);

        Option passwordOption = Option.builder()
                .longOpt(SemuxOption.PASSWORD.toString()).desc(CLIMessages.get("WalletPassword"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName(SemuxOption.PASSWORD.toString())
                .type(String.class)
                .build();
        addOption(passwordOption);

        Option dumpPrivateKeyOption = Option.builder()
                .longOpt(SemuxOption.DUMP_PRIVATE_KEY.toString())
                .desc(CLIMessages.get("PrintHexKey"))
                .hasArg(true).optionalArg(false).argName("address").type(String.class)
                .build();
        addOption(dumpPrivateKeyOption);

        Option importPrivateKeyOption = Option.builder()
                .longOpt(SemuxOption.IMPORT_PRIVATE_KEY.toString())
                .desc(CLIMessages.get("ImportHexKey"))
                .hasArg(true).optionalArg(false).argName("key").type(String.class)
                .build();
        addOption(importPrivateKeyOption);
    }

    public void start(String[] args) throws ParseException {
        CommandLine cmd = parseOptions(args);

        if (cmd.hasOption(SemuxOption.DATA_DIR.toString())) {
            setDataDir(cmd.getOptionValue(SemuxOption.DATA_DIR.toString()));
        }

        if (cmd.hasOption(SemuxOption.COINBASE.toString())) {
            setCoinbase(((Number) cmd.getParsedOptionValue(SemuxOption.COINBASE.toString())).intValue());
        }

        if (cmd.hasOption(SemuxOption.NETWORK.toString())) {
            setNetwork(cmd.getOptionValue(SemuxOption.NETWORK.toString()));
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
        Wallet wallet = loadAndUnlockWallet();

        List<EdDSA> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            EdDSA key = new EdDSA();
            wallet.addAccount(key);
            wallet.flush();
            accounts = wallet.getAccounts();
            logger.info(CLIMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
        }

        if (getCoinbase() < 0 || getCoinbase() >= accounts.size()) {
            logger.error(CLIMessages.get("CoinbaseDoesNotExist"));
            SystemUtil.exit(-1);
        }

        // start kernel
        startKernel(getConfig(), wallet, wallet.getAccount(getCoinbase()));
    }

    protected Kernel startKernel(Config config, Wallet wallet, EdDSA coinbase) {
        Kernel kernel = new Kernel(config, wallet, coinbase);
        kernel.start();

        return kernel;
    }

    protected void createAccount() {
        Wallet wallet = loadAndUnlockWallet();

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        if (wallet.flush()) {
            logger.info(CLIMessages.get("NewAccountCreatedForAddress", key.toAddressString()));
            logger.info(CLIMessages.get("PublicKey", Hex.encode(key.getPublicKey())));
            logger.info(CLIMessages.get("PrivateKey", Hex.encode(key.getPrivateKey())));
        }
    }

    protected void listAccounts() {
        Wallet wallet = loadAndUnlockWallet();

        List<EdDSA> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            logger.info(CLIMessages.get("AccountMissing"));
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                logger.info(CLIMessages.get("ListAccountItem", i, accounts.get(i).toString()));
            }
        }
    }

    protected void changePassword() {
        Wallet wallet = loadAndUnlockWallet();

        try {
            String newPassword = SystemUtil.readPassword(CLIMessages.get("EnterNewPassword"));
            wallet.changePassword(newPassword);
            Boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error(CLIMessages.get("WalletFileCannotBeUpdated"));
                SystemUtil.exit(1);
            }

            logger.info(CLIMessages.get("PasswordChangedSuccessfully"));
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    protected void dumpPrivateKey(String address) {
        Wallet wallet = loadAndUnlockWallet();

        byte[] addressBytes = Hex.decode0x(address);
        EdDSA account = wallet.getAccount(addressBytes);
        if (account == null) {
            logger.error(CLIMessages.get("AddressNotInWallet"));
            SystemUtil.exit(1);
        } else {
            System.out.println(CLIMessages.get("PrivateKeyIs", Hex.encode(account.getPrivateKey())));
        }
    }

    protected void importPrivateKey(String key) {
        try {
            Wallet wallet = loadAndUnlockWallet();
            byte[] keyBytes = Hex.decode0x(key);
            EdDSA account = new EdDSA(keyBytes);

            boolean accountAdded = wallet.addAccount(account);
            if (!accountAdded) {
                logger.error(CLIMessages.get("PrivateKeyAlreadyInWallet"));
                SystemUtil.exit(1);
            }

            boolean walletFlushed = wallet.flush();
            if (!walletFlushed) {
                logger.error(CLIMessages.get("WalletFileCannotBeUpdated"));
                SystemUtil.exit(2);
            }

            logger.info(CLIMessages.get("PrivateKeyImportedSuccessfully"));
            logger.info(CLIMessages.get("Address", account.toAddressString()));
            logger.info(CLIMessages.get("PublicKey", Hex.encode(account.getPublicKey())));
            logger.info(CLIMessages.get("PrivateKey", Hex.encode(account.getPrivateKey())));
        } catch (InvalidKeySpecException exception) {
            logger.error(CLIMessages.get("PrivateKeyCannotBeDecoded", exception.getMessage()));
            SystemUtil.exit(3);
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
            SystemUtil.exit(-1);
        }
    }

    protected Wallet loadAndUnlockWallet() {
        if (getPassword() == null) {
            setPassword(SystemUtil.readPassword());
        }

        Wallet wallet = loadWallet();
        if (!wallet.unlock(getPassword())) {
            SystemUtil.exit(-1);
        }

        return wallet;
    }

    protected Wallet loadWallet() {
        return new Wallet(new File(getDataDir(), "wallet.data"));
    }

}