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
import org.semux.net.msg.consensus.BlockPartsMessage;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.consensus.GetBlockPartsMessage;
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
import org.semux.net.msg.p2p.handshake.v2.HelloMessage;
import org.semux.net.msg.p2p.handshake.v2.InitMessage;
import org.semux.net.msg.p2p.handshake.v2.WorldMessage;
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
     * @param body
     *            The message body
     * @return The decoded message, or NULL if the message type is not unknown
     * @throws MessageException
     *             when the encoding is illegal
     */
    public Message create(byte code, byte[] body) throws MessageException {

        MessageCode c = MessageCode.of(code);
        if (c == null) {
            logger.debug("Invalid message code: {}", Hex.encode0x(Bytes.of(code)));
            return null;
        }

        try {
            switch (c) {
            case DISCONNECT:
                return new DisconnectMessage(body);
            case HELLO:
                return new org.semux.net.msg.p2p.handshake.v1.HelloMessage(body);
            case WORLD:
                return new org.semux.net.msg.p2p.handshake.v1.WorldMessage(body);
            case PING:
                return new PingMessage(body);
            case PONG:
                return new PongMessage(body);
            case GET_NODES:
                return new GetNodesMessage(body);
            case NODES:
                return new NodesMessage(body);
            case TRANSACTION:
                return new TransactionMessage(body);
            case HANDSHAKE_INIT:
                return new InitMessage(body);
            case HANDSHAKE_HELLO:
                return new HelloMessage(body);
            case HANDSHAKE_WORLD:
                return new WorldMessage(body);

            case GET_BLOCK:
                return new GetBlockMessage(body);
            case BLOCK:
                return new BlockMessage(body);
            case GET_BLOCK_HEADER:
                return new GetBlockHeaderMessage(body);
            case BLOCK_HEADER:
                return new BlockHeaderMessage(body);
            case GET_BLOCK_PARTS:
                return new GetBlockPartsMessage(body);
            case BLOCK_PARTS:
                return new BlockPartsMessage(body);

            case BFT_NEW_HEIGHT:
                return new NewHeightMessage(body);
            case BFT_NEW_VIEW:
                return new NewViewMessage(body);
            case BFT_PROPOSAL:
                return new ProposalMessage(body);
            case BFT_VOTE:
                return new VoteMessage(body);

            default:
                throw new UnreachableException();
            }
        } catch (Exception e) {
            throw new MessageException("Failed to decode message", e);
        }
    }
}
