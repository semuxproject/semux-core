/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semux.config.AbstractConfig;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.config.MainNetConfig;
import org.semux.config.TestNetConfig;

public abstract class OptionStart {

    protected static final String DEVNET = "devnet";

    protected static final String TESTNET = "testnet";

    protected static final String MAINNET = "mainnet";

    private String network = MAINNET;
    private Options options = new Options();

    private int coinbase = 0;
    private String password = null;

    private String dataDir = Constants.DEFAULT_DATA_DIR;

    protected void addOption(Option option) {
        options.addOption(option);
    }

    /**
     * Creates the correct Instance of an AbstractConfig Implementation depending on
     * the CLI --network option given.<br />
     * Defaults to MainNet.
     * 
     * @return AbstractConfigImplementation
     */
    public AbstractConfig getConfig() {
        AbstractConfig config = null;
        switch (getNetwork() != null ? getNetwork() : MAINNET) {
        case TESTNET:
            config = new TestNetConfig(getDataDir());
            break;
        case DEVNET:
            config = new DevNetConfig(getDataDir());
            break;
        case MAINNET:
        default:
            config = new MainNetConfig(getDataDir());
            break;
        }
        return config;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public Options getOptions() {
        return options;
    }

    public int getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(int coinbase) {
        this.coinbase = coinbase;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

}
