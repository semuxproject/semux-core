/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * Represents a Peer in the Semux network.
 */
public class Peer {
    /*
     * Below are the listening IP address and port number, not necessarily the real
     * address that we're connecting to.
     */
    private String ip;
    private int port;

    private short networkVersion;
    private String clientId;
    private String peerId;
    private long latestBlockNumber;

    /*
     * Variables below are not persisted
     */
    private long latency;

    /**
     * Create a new Peer instance.
     * 
     * @param ip
     * @param port
     * @param networkVersion
     * @param clientId
     * @param peerId
     * @param latestBlockNumber
     */
    public Peer(String ip, int port, short networkVersion, String clientId, String peerId, long latestBlockNumber) {
        super();
        this.ip = ip;
        this.port = port;
        this.peerId = peerId;
        this.latestBlockNumber = latestBlockNumber;
        this.networkVersion = networkVersion;
        this.clientId = clientId;
    }

    public boolean validate() {
        return ip != null && ip.length() <= 128
                && port >= 0
                && networkVersion >= 0
                && clientId != null && clientId.length() < 128
                && peerId != null && peerId.length() == 40
                && latestBlockNumber >= 0;
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
     * Returns the latestBlockNumber.
     * 
     * @return
     */
    public long getLatestBlockNumber() {
        return latestBlockNumber;
    }

    /**
     * Sets the latestBlockNumber.
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

    /**
     * Converts into a byte array.
     * 
     * @return
     */
    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeString(ip);
        enc.writeInt(port);
        enc.writeShort(networkVersion);
        enc.writeString(clientId);
        enc.writeString(peerId);
        enc.writeLong(latestBlockNumber);

        return enc.toBytes();
    }

    /**
     * Parses from a byte array.
     * 
     * @param bytes
     * @return
     */
    public static Peer fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        String ip = dec.readString();
        int port = dec.readInt();
        short p2pVersion = dec.readShort();
        String clientId = dec.readString();
        String peerId = dec.readString();
        long latestBlockNumber = dec.readLong();

        return new Peer(ip, port, p2pVersion, clientId, peerId, latestBlockNumber);
    }

    @Override
    public String toString() {
        return getPeerId() + "@" + ip + ":" + port;
    }
}