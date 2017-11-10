package org.semux.cli;

import org.apache.commons.cli.*;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.core.WalletLockedException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.utils.SystemUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxCLI {

    private static final Logger logger = LoggerFactory.getLogger(SemuxCLI.class);

    private Options options = new Options();

    private Wallet wallet = null;

    /**
     * Values read from options
     */
    private String dataDir = ".";
    private int coinbase = 0;
    private String password = null;

    public SemuxCLI() {
        // FIXME: the option is redundant in order to avoid ParseException thrown from DefaultParser.parse
        Option cli = Option.builder("cli").longOpt("cli").build();
        options.addOption(cli);

        Option help = Option.builder("h")
                .longOpt("help")
                .desc("Print help info and exit")
                .build();
        options.addOption(help);

        Option version = Option.builder("v")
                .longOpt("version")
                .desc("Show the version of this client")
                .build();
        options.addOption(version);

        Option account = Option.builder("a")
                .longOpt("account")
                .desc("create\tCreate an new account and exit" +
                        "\n" +
                        "list\tList all accounts and exit")
                .hasArg(true)
                .numberOfArgs(1)
                .optionalArg(false)
                .argName("action")
                .type(String.class)
                .build();
        options.addOption(account);

        Option changepassword = Option.builder("cp")
                .longOpt("changepassword")
                .desc("Change password of the wallet")
                .build();
        options.addOption(changepassword);

        Option datadir = Option.builder("d")
                .longOpt("datadir")
                .desc("Specify the data directory")
                .hasArg(true)
                .numberOfArgs(1)
                .optionalArg(false)
                .argName("path")
                .type(String.class)
                .build();
        options.addOption(datadir);

        Option coinbase = Option.builder("c")
                .longOpt("coinbase")
                .desc("Specify which account to be used as coinbase")
                .hasArg(true)
                .numberOfArgs(1)
                .optionalArg(false)
                .argName("index")
                .type(Number.class)
                .build();
        options.addOption(coinbase);

        Option password = Option.builder("p")
                .longOpt("password")
                .desc("Password of the wallet")
                .hasArg(true)
                .numberOfArgs(1)
                .optionalArg(false)
                .argName("password")
                .type(String.class)
                .build();
        options.addOption(password);
    }

    public static void main(String[] args) {
        try {
            SemuxCLI cli = new SemuxCLI();
            cli.start(args);
        } catch (ParseException exception) {
            logger.error("Parsing failed.  Reason: {}", exception.getMessage());
        }
    }

    public void start(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("datadir")) {
            dataDir = (String) commandLine.getParsedOptionValue("datadir");
        }

        if (commandLine.hasOption("coinbase")) {
            coinbase = ((Number) commandLine.getParsedOptionValue("coinbase")).intValue();
        }

        if (commandLine.hasOption("password")) {
            password = (String) commandLine.getParsedOptionValue("password");
        }

        wallet = new Wallet(new File(dataDir, "wallet.data"));

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
        } else {
            startKernel();
        }
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./semux.sh --cli [options] or semux.bat [options]", options);
    }

    private void printVersion() {
        System.out.println(Config.CLIENT_VERSION);
    }

    private void startKernel() {
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

    private void createAccount() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        EdDSA key = new EdDSA();
        wallet.addAccount(key);

        logger.info(createLine(80));
        logger.info("Address     : " + key.toString());
        logger.info("Public key  : " + Hex.encode(key.getPublicKey()));
        logger.info("Private key : " + Hex.encode(key.getPrivateKey()));
        logger.info(createLine(80));

        if (wallet.flush()) {
            logger.info("The created account has been stored in your wallet.");
        }
    }

    private void listAccounts() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();

        if (accounts.isEmpty()) {
            logger.info("There is no account in your wallet!");
        } else {
            String line = String.format("+-%-3s-+-%-45s-+", createLine(3), createLine(45));
            logger.info(line);
            for (int i = 0; i < accounts.size(); i++) {
                logger.info(String.format("| %-3d | %-45s |", i, accounts.get(i).toString()));
                logger.info(line);
            }
        }
    }

    private void changePassword() {
        if (password == null) {
            password = SystemUtil.readPassword();
        }
        if (!wallet.unlock(password)) {
            System.exit(-1);
        }

        try {
            String newPassword = SystemUtil.readPassword("Please enter the new password: ");
            wallet.changePassword(newPassword);
            Boolean isFlushed = wallet.flush();
            if (!isFlushed) {
                logger.error("Failed to save the new password");
            } else {
                logger.info("Password is successfully changed");
            }
        } catch (WalletLockedException exception) {
            logger.error(exception.getMessage());
        }
    }

    private static String createLine(int width) {
        char[] buf = new char[width];
        Arrays.fill(buf, '-');
        return new String(buf);
    }
}
