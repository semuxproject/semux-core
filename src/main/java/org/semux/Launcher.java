/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.config.MainNetConfig;
import org.semux.config.TestNetConfig;

public abstract class Launcher {

    protected static final String DEVNET = "devnet";
    protected static final String TESTNET = "testnet";
    protected static final String MAINNET = "mainnet";

    private Options options = new Options();

    private String network = MAINNET;
    private int coinbase = 0;
    private String password = null;
    private String dataDir = Constants.DEFAULT_DATA_DIR;

    /**
     * Creates an instance of {@link Config} based on the given `--network` option.
     * <p>
     * Defaults to MainNet.
     * 
     * @return the configuration
     */
    public Config getConfig() {
        switch (network) {
        case TESTNET:
            return new TestNetConfig(getDataDir());
        case DEVNET:
            return new DevNetConfig(getDataDir());
        default:
            return new MainNetConfig(getDataDir());
        }
    }

    protected void addOption(Option option) {
        options.addOption(option);
    }

    protected Options getOptions() {
        return options;
    }

    public String getNetwork() {
        return network;
    }

    protected void setNetwork(String network) {
        this.network = network;
    }

    public int getCoinbase() {
        return coinbase;
    }

    protected void setCoinbase(int coinbase) {
        this.coinbase = coinbase;
    }

    public String getPassword() {
        return password;
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    public String getDataDir() {
        return dataDir;
    }

    protected void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
