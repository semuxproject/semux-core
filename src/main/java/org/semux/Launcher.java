/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.semux.cli.SemuxOption;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.config.MainnetConfig;
import org.semux.config.TestnetConfig;
import org.semux.log.LoggerConfigurator;
import org.semux.message.CliMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    protected static final String DEVNET = "devnet";
    protected static final String TESTNET = "testnet";
    protected static final String MAINNET = "mainnet";

    /**
     * Here we make sure that all shutdown hooks will be executed in the order of
     * registration. This is necessary to be manually maintained because
     * ${@link Runtime#addShutdownHook(Thread)} starts shutdown hooks concurrently
     * in unspecified order.
     */
    private static List<Pair<String, Runnable>> shutdownHooks = Collections.synchronizedList(new ArrayList<>());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Launcher::shutdownHook, "shutdown-hook"));
    }

    private Options options = new Options();

    private String dataDir = Constants.DEFAULT_DATA_DIR;
    private String network = MAINNET;

    private int coinbase = 0;
    private String password = null;

    public Launcher() {
        Option dataDirOption = Option.builder()
                .longOpt(SemuxOption.DATA_DIR.toString())
                .desc(CliMessages.get("SpecifyDataDir"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("path").type(String.class)
                .build();
        addOption(dataDirOption);

        Option networkOption = Option.builder()
                .longOpt(SemuxOption.NETWORK.toString()).desc(CliMessages.get("SpecifyNetwork"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("name").type(String.class)
                .build();
        addOption(networkOption);
    }

    /**
     * Creates an instance of {@link Config} based on the given `--network` option.
     * <p>
     * Defaults to MainNet.
     *
     * @return the configuration
     */
    public Config getConfig() {
        switch (getNetwork()) {
        case TESTNET:
            return new TestnetConfig(getDataDir());
        case DEVNET:
            return new DevnetConfig(getDataDir());
        default:
            return new MainnetConfig(getDataDir());
        }
    }

    /**
     * Returns the network.
     *
     * @return
     */
    public String getNetwork() {
        return network;
    }

    /**
     * Returns the data directory.
     *
     * @return
     */
    public String getDataDir() {
        return dataDir;
    }

    /**
     * Returns the coinbase.
     *
     * @return
     */
    public int getCoinbase() {
        return coinbase;
    }

    /**
     * Returns the provided password if any.
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Parses options from the given arguments.
     *
     * @param args
     * @return
     * @throws ParseException
     */
    protected CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getOptions(), args);

        if (cmd.hasOption(SemuxOption.DATA_DIR.toString())) {
            setDataDir(cmd.getOptionValue(SemuxOption.DATA_DIR.toString()));
        }

        if (cmd.hasOption(SemuxOption.NETWORK.toString())) {
            setNetwork(cmd.getOptionValue(SemuxOption.NETWORK.toString()));
        }

        return cmd;
    }

    /**
     * Set up customized logger configuration.
     *
     * @param args
     * @throws ParseException
     */
    protected void setupLogger(String[] args) throws ParseException {
        // parse options
        parseOptions(args);

        LoggerConfigurator.configure(new File(dataDir));
    }

    /**
     * Returns all supported options.
     *
     * @return
     */
    protected Options getOptions() {
        return options;
    }

    /**
     * Adds a supported option.
     *
     * @param option
     */
    protected void addOption(Option option) {
        options.addOption(option);
    }

    /**
     * Sets the network.
     *
     * @param network
     */
    protected void setNetwork(String network) {
        this.network = network;
    }

    /**
     * Sets the data directory.
     *
     * @param dataDir
     */
    protected void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Sets the coinbase.
     *
     * @param coinbase
     */
    protected void setCoinbase(int coinbase) {
        this.coinbase = coinbase;
    }

    /**
     * Sets the password.
     *
     * @param password
     */
    protected void setPassword(String password) {
        this.password = password;
    }

    /**
     * Registers a shutdown hook which will be executed in the order of
     * registration.
     *
     * @param name
     * @param runnable
     */
    public static synchronized void registerShutdownHook(String name, Runnable runnable) {
        shutdownHooks.add(Pair.of(name, runnable));
    }

    /**
     * Call registered shutdown hooks in the order of registration.
     *
     */
    private static synchronized void shutdownHook() {
        // shutdown hooks
        for (Pair<String, Runnable> r : shutdownHooks) {
            try {
                logger.info("Shutting down {}", r.getLeft());
                r.getRight().run();
            } catch (Exception e) {
                logger.info("Failed to shutdown {}", r.getLeft(), e);
            }
        }

        // flush log4j async loggers
        LogManager.shutdown();
    }
}
