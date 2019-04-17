/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import org.semux.config.Config;
import org.semux.consensus.SemuxBft;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.Genesis;
import org.semux.core.PendingManager;
import org.semux.core.Wallet;
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.util.SimpleApiClient;

/**
 * This kernel mock extends the {@link Kernel} by adding a bunch of setters of
 * the components.
 */
public class KernelMock extends Kernel {

    /**
     * Creates a kernel mock with the given configuration, wallet and coinbase.
     * 
     * @param config
     * @param genesis
     * @param wallet
     * @param coinbase
     */
    public KernelMock(Config config, Genesis genesis, Wallet wallet, Key coinbase) {
        super(config, genesis, wallet, coinbase);
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
     * Sets the bft manager instance.
     * 
     * @param bft
     */
    public void setBftManager(SemuxBft bft) {
        this.bft = bft;
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
     * @return an {@link SimpleApiClient} instance
     */
    public SimpleApiClient getApiClient() {
        Config c = getConfig();
        return new SimpleApiClient(c.apiListenIp(), c.apiListenPort(), c.apiUsername(), c.apiPassword());
    }
}
