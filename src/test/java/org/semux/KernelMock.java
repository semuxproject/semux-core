/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.Wallet;
import org.semux.core.exception.WalletLockedException;
import org.semux.crypto.EdDSA;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.util.ApiClient;

public class KernelMock extends Kernel {

    private static final String password = "password";

    public KernelMock() {
        super(null, null, null);
        try {
            this.config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
            this.wallet = new Wallet(File.createTempFile("wallet", ".data"));
            this.wallet.unlock(password);
            for (int i = 0; i < 10; i++) {
                this.wallet.addAccount(new EdDSA());
            }
            this.coinbase = wallet.getAccount(0);
        } catch (WalletLockedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public KernelMock(Config config, Wallet wallet, EdDSA coinbase) {
        super(config, wallet, coinbase);
    }

    public void setBlockchain(Blockchain chain) {
        this.chain = chain;
    }

    public void setClient(PeerClient client) {
        this.client = client;
    }

    public void setPendingManager(PendingManager pendingMgr) {
        this.pendingMgr = pendingMgr;
    }

    public void setChannelManager(ChannelManager channelMgr) {
        this.channelMgr = channelMgr;
    }

    public void setNodeManager(NodeManager nodeMgr) {
        this.nodeMgr = nodeMgr;
    }

    public void setSyncManager(SemuxSync sync) {
        this.sync = sync;
    }

    public void setConsensus(SemuxBFT cons) {
        this.cons = cons;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setCoinbase(EdDSA coinbase) {
        this.coinbase = coinbase;
    }

    /**
     * Returns an API client instance which connects to the mock kernel.
     *
     * @return an {@link ApiClient} instance
     */
    public ApiClient getApiClient() {
        return new ApiClient(new InetSocketAddress(this.getConfig().apiListenIp(), this.getConfig().apiListenPort()),
                this.getConfig().apiUsername(), this.getConfig().apiPassword());
    }
}
