/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.semux.Network;

/**
 * Represents a Peer in the semux network, including both static and dynamic
 * info.
 */
public class Peer {

    /**
     * The network id;
     */
    private final Network network;

    /**
     * The network version.
     */
    private final short networkVersion;

    /**
     * The peer id.
     */
    private final String peerId;

    /**
     * The IP address.
     */
    private final String ip;

    /**
     * The listening port.
     */
    private final int port;

    /**
     * The client software id.
     */
    private final String clientId;

    /**
     * The supported capabilities.
     */
    private final String[] capabilities;

    // ===============================
    // Variables below are volatile
    // ===============================

    private long latestBlockNumber;
    private long latency;

    /**
     * Create a new Peer instance.
     *
     * @param network
     * @param networkVersion
     * @param peerId
     * @param ip
     * @param port
     * @param clientId
     * @param capabilities
     * @param latestBlockNumber
     */
    public Peer(Network network, short networkVersion, String peerId, String ip, int port, String clientId,
            String[] capabilities, long latestBlockNumber) {
        this.network = network;
        this.ip = ip;
        this.port = port;
        this.peerId = peerId;
        this.networkVersion = networkVersion;
        this.clientId = clientId;
        this.capabilities = capabilities;
        this.latestBlockNumber = latestBlockNumber;
    }

    /**
     * Returns the listening IP address.
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns the listening port number.
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the network.
     *
     * @return
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Returns the network version.
     *
     * @return
     */
    public short getNetworkVersion() {
        return networkVersion;
    }

    /**
     * Returns the client id.
     *
     * @return
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the peerId.
     *
     * @return
     */
    public String getPeerId() {
        return peerId;
    }

    /**
     * Returns the capabilities.
     */
    public String[] getCapabilities() {
        return capabilities;
    }

    /**
     * Returns the latest block number.
     *
     * @return
     */
    public long getLatestBlockNumber() {
        return latestBlockNumber;
    }

    /**
     * Sets the latest block number.
     *
     * @param number
     */
    public void setLatestBlockNumber(long number) {
        this.latestBlockNumber = number;
    }

    /**
     * Returns peer latency.
     *
     * @return
     */
    public long getLatency() {
        return latency;
    }

    /**
     * Sets peer latency.
     *
     * @param latency
     */
    public void setLatency(long latency) {
        this.latency = latency;
    }

    @Override
    public String toString() {
        return getPeerId() + "@" + ip + ":" + port;
    }
}