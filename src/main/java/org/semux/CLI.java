/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The command line interface.
 *
 */
public class CLI {
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    private static String dataDir = ".";
    private static int coinbase = 0;
    private static String password = null;

    private static Action action = Action.START_KERNEL;

    public static void main(String[] args) {
        parseArguments(args);
        Wallet wallet = new Wallet(new File(dataDir, "wallet.data"));

        switch (action) {
        case START_KERNEL:
            startKernel(wallet);
            break;
        case CREATE_ACCOUNT:
            createAccount(wallet);
            break;
        case LIST_ACCOUNTS:
            listAccounts(wallet);
            break;
        }
    }

    private static void parseArguments(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                case "-h":
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                case "-v":
                case "--version":
                    System.out.println(Config.CLIENT_VERSION);
                    System.exit(0);
                    break;
                case "-d":
                case "--datadir":
                    dataDir = args[++i];
                    break;
                case "-a":
                case "--account":
                    String type = args[++i];
                    switch (type) {
                    case "create":
                        action = Action.CREATE_ACCOUNT;
                        break;
                    case "list":
                        action = Action.LIST_ACCOUNTS;
                        break;
                    default:
                        printUsageAndExit(-1);
                    }
                    break;
                case "-c":
                case "--coinbase":
                    coinbase = Integer.parseInt(args[++i]);
                    break;
                case "-p":
                case "--password":
                    password = args[++i];
                    break;
                default:
                    printUsageAndExit(-1);
                }
            }
        } catch (Exception e) {
            printUsageAndExit(-1);
        }
    }

    private static void printUsage() {
        System.out.println("===============================================================");
        System.out.println(Config.CLIENT_FULL_NAME);
        System.out.println("===============================================================\n");
        System.out.println("Usage:");
        System.out.println("  ./run.sh [options] or run.bat [options]\n");
        System.out.println("Options:");
        System.out.println("  -h, --help              Print help info and exit");
        System.out.println("  -v, --version           Show the version of this client");
        System.out.println("  -d, --datadir   path    Specify the data directory");
        System.out.println("  -a, --account   create  Create an new account and exit");
        System.out.println("                  list    List all accounts and exit");
        System.out.println("  -c, --coinbase  index   Specify which account to be used as coinbase");
        System.out.println("  -p, --password  " + "pwd     Password of the wallet");
        System.out.println();
    }

    private static void printUsageAndExit(int code) {
        printUsage();
        System.exit(code);
    }

    private static void startKernel(Wallet wallet) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
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

    private static void createAccount(Wallet wallet) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        System.out.println(createLine(80));
        System.out.println("Address     : " + key.toString());
        System.out.println("Public key  : " + Hex.encode(key.getPublicKey()));
        System.out.println("Private key : " + Hex.encode(key.getPrivateKey()));
        System.out.println(createLine(80));

        if (wallet.flush()) {
            System.out.println("The created account has been stored in your wallet.");
        }
    }

    private static void listAccounts(Wallet wallet) {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            System.out.println("No account in your wallet!");
        } else {
            String line = String.format("+-%-3s-+-%-45s-+", createLine(3), createLine(45));
            System.out.println(line);
            for (int i = 0; i < accounts.size(); i++) {
                System.out.println(String.format("| %-3d | %-45s |", i, accounts.get(i).toString()));
                System.out.println(line);
            }
        }
    }

    private static String createLine(int width) {
        char[] buf = new char[width];
        Arrays.fill(buf, '-');
        return new String(buf);
    }

    private enum Action {
        START_KERNEL, CREATE_ACCOUNT, LIST_ACCOUNTS
    }
}
