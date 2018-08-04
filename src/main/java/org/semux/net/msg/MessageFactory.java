/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import org.semux.crypto.Hex;
import org.semux.net.msg.consensus.BlockHeaderMessage;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.consensus.NewHeightMessage;
import org.semux.net.msg.consensus.NewViewMessage;
import org.semux.net.msg.consensus.ProposalMessage;
import org.semux.net.msg.consensus.VoteMessage;
import org.semux.net.msg.p2p.DisconnectMessage;
import org.semux.net.msg.p2p.GetNodesMessage;
import org.semux.net.msg.p2p.NodesMessage;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.util.Bytes;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageFactory {

    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    /**
     * Decode a raw message.
     *
     * @param code
     *            The message code
     * @param encoded
     *            The message encoded
     * @return The decoded message, or NULL if the message type is not unknown
     * @throws MessageException
     *             when the encoding is illegal
     */
    public Message create(byte code, byte[] encoded) throws MessageException {

        MessageCode c = MessageCode.of(code);
        if (c == null) {
            logger.debug("Invalid message code: {}", Hex.encode0x(Bytes.of(code)));
            return null;
        }

        try {
            switch (c) {
            case DISCONNECT:
                return new DisconnectMessage(encoded);
            case HELLO:
                return new org.semux.net.msg.p2p.handshake.v1.HelloMessage(encoded);
            case WORLD:
                return new org.semux.net.msg.p2p.handshake.v1.WorldMessage(encoded);
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
            case HANDSHAKE_INIT:
                return new org.semux.net.msg.p2p.handshake.v2.InitMessage(encoded);
            case HANDSHAKE_HELLO:
                return new org.semux.net.msg.p2p.handshake.v2.HelloMessage(encoded);
            case HANDSHAKE_WORLD:
                return new org.semux.net.msg.p2p.handshake.v2.WorldMessage(encoded);

            case GET_BLOCK:
                return new GetBlockMessage(encoded);
            case BLOCK:
                return new BlockMessage(encoded);
            case GET_BLOCK_HEADER:
                return new GetBlockHeaderMessage(encoded);
            case BLOCK_HEADER:
                return new BlockHeaderMessage(encoded);

            case BFT_NEW_HEIGHT:
                return new NewHeightMessage(encoded);
            case BFT_NEW_VIEW:
                return new NewViewMessage(encoded);
            case BFT_PROPOSAL:
                return new ProposalMessage(encoded);
            case BFT_VOTE:
                return new VoteMessage(encoded);

            default:
                throw new UnreachableException();
            }
        } catch (Exception e) {
            throw new MessageException("Failed to decode message", e);
        }
    }
}
