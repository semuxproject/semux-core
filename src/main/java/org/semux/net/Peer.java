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

    private short p2pVersion;
    private String clientId;
    private String peerId;
    private long latestBlockNumber;

    /*
     * Variables below are not persisted
     */
    private long latency;

    /**
     * Create a new Peer.
     * 
     * @param ip
     * @param port
     * @param p2pVersion
     * @param clientId
     * @param peerId
     * @param latestBlockNumber
     */
    public Peer(String ip, int port, short p2pVersion, String clientId, String peerId, long latestBlockNumber) {
        super();
        this.ip = ip;
        this.port = port;
        this.peerId = peerId;
        this.latestBlockNumber = latestBlockNumber;
        this.p2pVersion = p2pVersion;
        this.clientId = clientId;
    }

    public boolean validate() {
        return ip != null && ip.length() <= 128 //
                && port >= 0 //
                && p2pVersion >= 0 //
                && clientId != null && clientId.length() < 128 //
                && peerId != null && peerId.length() == 40 //
                && latestBlockNumber >= 0;
    }

    /**
     * Get the listening IP address.
     * 
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * Get the listening port number.
     * 
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the P2P version number.
     * 
     * @return
     */
    public short getP2pVersion() {
        return p2pVersion;
    }

    /**
     * Get the client id.
     * 
     * @return
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get the peerId.
     * 
     * @return
     */
    public String getPeerId() {
        return peerId;
    }

    /**
     * Get the latestBlockNumber.
     * 
     * @return
     */
    public long getLatestBlockNumber() {
        return latestBlockNumber;
    }

    /**
     * Set the latestBlockNumber.
     * 
     * @param number
     */
    public void setLatestBlockNumber(long number) {
        this.latestBlockNumber = number;
    }

    /**
     * Get peer latency.
     * 
     * @return
     */
    public long getLatency() {
        return latency;
    }

    /**
     * Set peer latency.
     * 
     * @param latency
     */
    public void setLatency(long latency) {
        this.latency = latency;
    }

    /**
     * Convert to a byte array.
     * 
     * @return
     */
    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeString(ip);
        enc.writeInt(port);
        enc.writeShort(p2pVersion);
        enc.writeString(clientId);
        enc.writeString(peerId);
        enc.writeLong(latestBlockNumber);

        return enc.toBytes();
    }

    /**
     * Parse from a byte array.
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

    /**
     * Return the string representation
     */
    @Override
    public String toString() {
        return getPeerId() + "@" + ip + ":" + port;
    }
}