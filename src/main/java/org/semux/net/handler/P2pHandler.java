/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.handler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.Peer;
import org.semux.net.PeerClient;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageQueue;
import org.semux.net.msg.MessageRoundtrip;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.p2p.GetNodesMessage;
import org.semux.net.msg.p2p.HelloMessage;
import org.semux.net.msg.p2p.NodesMessage;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;
import org.semux.net.msg.p2p.TransactionMessage;
import org.semux.net.msg.p2p.WorldMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Process P2P messages
 * 
 */
public class P2pHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger(P2pHandler.class);

    private final static short[] SUPPORTED_VERSIONS = { 1 };
    private final static int[] MESSAGE_RANGE = { 0, 0x2F };

    private Channel channel;
    private PeerClient client;

    private Blockchain chain;
    private MessageQueue msgQueue;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    private PendingManager pendingMgr;

    private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "p2p-handler-" + cnt.getAndIncrement());
        }
    });

    private ScheduledFuture<?> getPeers = null;
    private ScheduledFuture<?> pingPong = null;

    public P2pHandler(Channel channel, PeerClient client) {
        this.channel = channel;
        this.client = client;

        this.chain = channel.getBlockchain();
        this.msgQueue = channel.getMessageQueue();
        this.channelMgr = channel.getChannelManager();
        this.nodeMgr = channel.getNodeManager();

        this.pendingMgr = PendingManager.getInstance();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler active, cid = {}", channel.getId());

        msgQueue.activate(ctx);

        // disconnect if too many connections
        if (channel.isInbound() && channelMgr.size() >= Config.NET_MAX_CONNECTIONS) {
            msgQueue.disconnect(ReasonCode.TOO_MANY_PEERS);
            return;
        }

        // only the connection initiator sends the HELLO message
        if (!channel.isInbound()) {
            Peer peer = new Peer(client.getIp(), client.getPort(), Config.P2P_VERSION, Config.getClientId(),
                    client.getPeerId(), chain.getLatestBlockNumber());
            HelloMessage msg = new HelloMessage(peer, client.getCoinbase());
            msgQueue.sendMessage(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler inactive, cid = {}", channel.getId());

        stopTimers();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("Exception in P2P handler, cid = {}", channel.getId(), cause);

        stopTimers();

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
        MessageRoundtrip mr = msgQueue.receivedMessage(msg);

        switch (msg.getCode()) {
        case DISCONNECT: {
            msgQueue.disconnect();
            break;
        }
        case HELLO: {
            HelloMessage helloMsg = (HelloMessage) msg;
            Peer peer = helloMsg.getPeer();

            ReasonCode error = null;
            if (!isSupported(peer.getP2pVersion())) {
                error = ReasonCode.BAD_PROTOCOL;
            } else if (client.getPeerId().equals(peer.getPeerId()) || channelMgr.isConnected(peer.getPeerId())) {
                error = ReasonCode.DUPLICATE_PEER_ID;
            } else if (!helloMsg.isValid()) {
                error = ReasonCode.BAD_PEER_ID;
            }

            if (error == null) {
                // update peer state
                channel.setRemotePeer(peer);

                // reply with a WORLD message
                peer = new Peer(client.getIp(), client.getPort(), Config.P2P_VERSION, Config.getClientId(),
                        client.getPeerId(), chain.getLatestBlockNumber());
                WorldMessage worldMsg = new WorldMessage(peer, client.getCoinbase());

                msgQueue.sendMessage(worldMsg);

                // add consensus handler
                addConsensusHandler(ctx);
            } else {
                msgQueue.disconnect(error);
            }
            break;
        }
        case WORLD: {
            // update peer state
            WorldMessage worldMsg = (WorldMessage) msg;
            Peer peer = worldMsg.getPeer();
            channel.setRemotePeer(peer);

            // register handler
            addConsensusHandler(ctx);

            // start peers exchange
            getPeers = exec.scheduleAtFixedRate(() -> {
                msgQueue.sendMessage(new GetNodesMessage(channelMgr.getActiveAddresses()));
            }, 1, 5, TimeUnit.MINUTES);

            // start ping pong
            pingPong = exec.scheduleAtFixedRate(() -> {
                msgQueue.sendMessage(new PingMessage());
            }, 1, 1, TimeUnit.MINUTES);
            break;
        }
        case PING: {
            PongMessage pong = new PongMessage();
            msgQueue.sendMessage(pong);
            break;
        }
        case PONG: {
            if (mr != null) {
                long latency = System.currentTimeMillis() - mr.getLastTimestamp();
                channel.getRemotePeer().setLatency(latency);
            }
            break;
        }
        case GET_NODES: {
            GetNodesMessage getNodesMsg = (GetNodesMessage) msg;
            nodeMgr.addNodes(getNodesMsg.getNodes());

            NodesMessage nodesMsg = new NodesMessage(channelMgr.getActiveAddresses());
            msgQueue.sendMessage(nodesMsg);
            break;
        }
        case NODES: {
            NodesMessage nodesMsg = (NodesMessage) msg;
            nodeMgr.addNodes(nodesMsg.getNodes());
            break;
        }
        case TRANSACTION: {
            TransactionMessage transactionMsg = (TransactionMessage) msg;
            pendingMgr.addTransaction(transactionMsg.getTransaction());
            break;
        }
        default: {
            ctx.fireChannelRead(msg);
            break;
        }
        }
    }

    /**
     * Add consensus handler after successful handshake.
     * 
     * @param ctx
     */
    private void addConsensusHandler(ChannelHandlerContext ctx) {
        ConsensusHandler ch = new ConsensusHandler(channel);
        ctx.pipeline().addLast("consensusHandler", ch);
    }

    /**
     * Check if the p2p version is supported.
     * 
     * @param version
     * @return
     */
    private boolean isSupported(short version) {
        return ArrayUtils.contains(SUPPORTED_VERSIONS, version);
    }

    /**
     * Stop all scheduled timers, including the message queue.
     */
    private void stopTimers() {
        if (getPeers != null) {
            getPeers.cancel(false);
            getPeers = null;
        }

        if (pingPong != null) {
            pingPong.cancel(false);
            pingPong = null;
        }

        msgQueue.close();
    }
}