/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.handler;

import org.semux.consensus.SemuxBFT;
import org.semux.core.Consensus;
import org.semux.net.Channel;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * The network handler that processes consensus messages.
 * 
 */
public class ConsensusHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger(ConsensusHandler.class);

    private final static int[] MESSAGE_RANGE = { 0x30, 0x6F };

    private Channel channel;
    private MessageQueue msgQueue;

    private Consensus consensus;

    /**
     * Create a consensus handler.
     * 
     * @param channel
     */
    public ConsensusHandler(Channel channel) {
        this.channel = channel;
        this.msgQueue = channel.getMessageQueue();

        this.consensus = SemuxBFT.getInstance();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Consensus handler active, cid = {}", channel.getId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Consensus handler inactive, cid = {}", channel.getId());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("Exception in consensus handler, cid = {}", channel.getId(), cause);

        // close channel
        if (ctx.channel().isOpen()) {
            ctx.channel().close();
        }
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        if (super.acceptInboundMessage(msg)) {
            int code = ((Message) msg).getCode().getCode();
            if (code >= MESSAGE_RANGE[0] && code <= MESSAGE_RANGE[1]) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Message msg) throws InterruptedException {
        logger.trace("Received message: " + msg);
        msgQueue.receivedMessage(msg);

        consensus.onMessage(channel, msg);
    }
}