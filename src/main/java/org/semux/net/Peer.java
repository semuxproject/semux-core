/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.semux.net.Capability.MAX_NUMBER_OF_CAPABILITIES;

import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * Represents a Peer in the semux network.
 */
public class Peer {
    /**
     * The IP address.
     */
    private final String ip;

    /**
     * The listening port.
     */
    private final int port;

    /**
     * The network version.
     */
    private final short networkVersion;

    /**
     * The client software id.
     */
    private final String clientId;

    /**
     * The peer id.
     */
    private final String peerId;

    /**
     * The supported capabilities.
     */
    private final CapabilitySet capabilities;

    // ===============================
    // Variables below are volatile
    // ===============================

    private long latestBlockNumber;
    private long latency;

    /**
     * Create a new Peer instance.
     *
     * @param ip
     * @param port
     * @param networkVersion
     * @param clientId
     * @param peerId
     * @param capabilities
     * @param latestBlockNumber
     */
    public Peer(String ip, int port, short networkVersion, String clientId, String peerId, CapabilitySet capabilities,
            long latestBlockNumber) {
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
     * Getter for property 'capabilities'.
     *
     * @return Value for property 'capabilities'.
     */
    public CapabilitySet getCapabilities() {
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

    /*
     * The following methods should be removed once we decide to fully deprecate the
     * old handshake.
     */

    public boolean validate() {
        return ip != null && ip.length() <= 128
                && port >= 0
                && networkVersion >= 0
                && clientId != null && clientId.length() < 128
                && peerId != null && peerId.length() == 40
                && latestBlockNumber >= 0
                && capabilities != null && capabilities.size() <= MAX_NUMBER_OF_CAPABILITIES;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeString(ip);
        enc.writeInt(port);
        enc.writeShort(networkVersion);
        enc.writeString(clientId);
        enc.writeString(peerId);
        enc.writeLong(latestBlockNumber);

        // encode capabilities
        enc.writeInt(capabilities.size());
        for (String capability : capabilities.toList()) {
            enc.writeString(capability);
        }

        return enc.toBytes();
    }

    public static Peer fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        String ip = dec.readString();
        int port = dec.readInt();
        short p2pVersion = dec.readShort();
        String clientId = dec.readString();
        String peerId = dec.readString();
        long latestBlockNumber = dec.readLong();

        // decode capabilities
        final int numberOfCapabilities = Math.min(dec.readInt(), MAX_NUMBER_OF_CAPABILITIES);
        String[] capabilityList = new String[numberOfCapabilities];
        for (int i = 0; i < numberOfCapabilities; i++) {
            capabilityList[i] = dec.readString();
        }

        return new Peer(ip, port, p2pVersion, clientId, peerId, CapabilitySet.of(capabilityList), latestBlockNumber);
    }
}