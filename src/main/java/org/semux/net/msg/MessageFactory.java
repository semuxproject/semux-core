/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import org.semux.net.msg.consensus.BFTNewHeightMessage;
import org.semux.net.msg.consensus.BFTNewViewMessage;
import org.semux.net.msg.consensus.BFTProposalMessage;
import org.semux.net.msg.consensus.BFTVoteMessage;
import org.semux.net.msg.consensus.BlockHeaderMessage;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.p2p.DisconnectMessage;
import org.semux.net.msg.p2p.GetNodesMessage;
import org.semux.net.msg.p2p.HelloMessage;
import org.semux.net.msg.p2p.NodesMessage;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.net.msg.p2p.WorldMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    /**
     * Decode a raw message.
     * 
     * @param code
     * @param encoded
     * @return
     */
    public Message create(byte code, byte[] encoded) {
        try {
            MessageCode c = MessageCode.of(code);

            switch (c) {
            case DISCONNECT:
                return new DisconnectMessage(encoded);
            case HELLO:
                return new HelloMessage(encoded);
            case WORLD:
                return new WorldMessage(encoded);
            case PING:
                return new PingMessage(encoded);
            case PONG:
                return new PongMessage(encoded);
            case GET_NODES:
                return new GetNodesMessage(encoded);
            case NODES:
                return new NodesMessage(encoded);
            case TRANSACTION:
                return new TransactionMessage(encoded);

            case GET_BLOCK:
                return new GetBlockMessage(encoded);
            case BLOCK:
                return new BlockMessage(encoded);
            case GET_BLOCK_HEADER:
                return new GetBlockHeaderMessage(encoded);
            case BLOCK_HEADER:
                return new BlockHeaderMessage(encoded);

            case BFT_NEW_HEIGHT:
                return new BFTNewHeightMessage(encoded);
            case BFT_NEW_VIEW:
                return new BFTNewViewMessage(encoded);
            case BFT_PROPOSAL:
                return new BFTProposalMessage(encoded);
            case BFT_VOTE:
                return new BFTVoteMessage(encoded);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse encoded message data", e);
        }

        return null;
    }
}
