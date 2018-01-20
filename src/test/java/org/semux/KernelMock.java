/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.net.InetSocketAddress;

import org.semux.config.Config;
import org.semux.consensus.SemuxBft;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.Wallet;
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.util.ApiClient;

/**
 * This kernel mock extends the {@link Kernel} by adding a bunch of setters of
 * the components.
 */
public class KernelMock extends Kernel {

    /**
     * Creates a kernel mock with the given configuration, wallet and coinbase.
     * 
     * @param config
     * @param wallet
     * @param coinbase
     */
    public KernelMock(Config config, Wallet wallet, Key coinbase) {
        super(config, wallet, coinbase);
    }

    /**
     * Sets the blockchain instance.
     * 
     * @param chain
     */
    public void setBlockchain(Blockchain chain) {
        this.chain = chain;
    }

    /**
     * Sets the peer client instance.
     * 
     * @param client
     */
    public void setClient(PeerClient client) {
        this.client = client;
    }

    /**
     * Sets the pending manager instance.
     * 
     * @param pendingMgr
     */
    public void setPendingManager(PendingManager pendingMgr) {
        this.pendingMgr = pendingMgr;
    }

    /**
     * Sets the channel manager instance.
     * 
     * @param channelMgr
     */
    public void setChannelManager(ChannelManager channelMgr) {
        this.channelMgr = channelMgr;
    }

    /**
     * Sets the node manager instance.
     * 
     * @param nodeMgr
     */
    public void setNodeManager(NodeManager nodeMgr) {
        this.nodeMgr = nodeMgr;
    }

    /**
     * Sets the sync manager instance.
     * 
     * @param sync
     */
    public void setSyncManager(SemuxSync sync) {
        this.sync = sync;
    }

    /**
     * Sets the consensus instance.
     * 
     * @param cons
     */
    public void setConsensus(SemuxBft cons) {
        this.cons = cons;
    }

    /**
     * Sets the configuration instance.
     * 
     * @param config
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Sets the coinbase.
     * 
     * @param coinbase
     */
    public void setCoinbase(Key coinbase) {
        this.coinbase = coinbase;
    }

    /**
     * Returns an API client instance which connects to the mock kernel.
     *
     * @return an {@link ApiClient} instance
     */
    public ApiClient getApiClient() {
        Config config = getConfig();
        return new ApiClient(new InetSocketAddress(config.apiListenIp(), config.apiListenPort()),
                config.apiUsername(), config.apiPassword());
    }
}
