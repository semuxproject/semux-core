/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v1;

import org.semux.Network;
import org.semux.net.Peer;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class PeerCodec {

    public static String[] mandatoryCapabilities(Network network) {
        switch (network) {
        case MAINNET:
            return new String[] { "SEM" };
        case TESTNET:
        case DEVNET:
        default:
            return new String[] { "SEM_TESTNET" };
        }
    }

    public static boolean validate(Peer peer) {
        return peer.getIp() != null && peer.getIp().length() <= 128
                && peer.getPort() >= 0
                && peer.getNetworkVersion() >= 0
                && peer.getClientId() != null && peer.getClientId().length() < 128
                && peer.getPeerId() != null && peer.getPeerId().length() == 40
                && peer.getLatestBlockNumber() >= 0
                && peer.getCapabilities() != null && peer.getCapabilities().length <= 128;
    }

    public static byte[] toBytes(Peer peer) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeString(peer.getIp());
        enc.writeInt(peer.getPort());
        enc.writeShort(peer.getNetworkVersion());
        enc.writeString(peer.getClientId());
        enc.writeString(peer.getPeerId());
        enc.writeLong(peer.getLatestBlockNumber());

        // encode capabilities
        enc.writeInt(peer.getCapabilities().length);
        for (String capability : peer.getCapabilities()) {
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
        final int numberOfCapabilities = Math.min(dec.readInt(), 128);
        String[] capabilityList = new String[numberOfCapabilities];
        for (int i = 0; i < numberOfCapabilities; i++) {
            capabilityList[i] = dec.readString();
        }

        return new Peer(null, p2pVersion, peerId, ip, port, clientId, capabilityList, latestBlockNumber);
    }
}
