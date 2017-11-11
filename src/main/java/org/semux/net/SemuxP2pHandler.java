/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.semux.Config;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.Sync;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageQueue;
import org.semux.net.msg.MessageRoundtrip;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BFTNewHeightMessage;
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Semux P2P message handler
 */
public class SemuxP2pHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger(SemuxP2pHandler.class);

    private final static short[] SUPPORTED_VERSIONS = { Config.P2P_VERSION };

    private Channel channel;

    private Blockchain chain;
    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    private MessageQueue msgQueue;
    private PeerClient client;

    private boolean isHandshakeDone;
    private Sync sync;
    private Consensus consenus;

    private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "p2p-" + cnt.getAndIncrement());
        }
    });

    private ScheduledFuture<?> getNodes = null;
    private ScheduledFuture<?> pingPong = null;

    /**
     * Creates a new P2P handler.
     * 
     * @param channel
     */
    public SemuxP2pHandler(Channel channel) {
        this.channel = channel;

        this.chain = channel.getBlockchain();
        this.pendingMgr = channel.getPendingManager();
        this.channelMgr = channel.getChannelManager();
        this.nodeMgr = channel.getNodeManager();

        this.msgQueue = channel.getMessageQueue();
        this.client = channel.getClient();

        this.isHandshakeDone = false;
        this.sync = SemuxSync.getInstance();
        this.consenus = SemuxBFT.getInstance();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler active, cid = {}", channel.getId());

        // activate message queue
        msgQueue.activate(ctx);

        // disconnect if too many connections
        if (channel.isInbound() && channelMgr.size() >= Config.NET_MAX_CONNECTIONS) {
            msgQueue.disconnect(ReasonCode.TOO_MANY_PEERS);
            return;
        }

        // send a HELLO message to initiate handshake
        if (!channel.isInbound()) {
            Peer peer = new Peer(client.getIp(), client.getPort(), Config.P2P_VERSION, Config.getClientId(false),
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
        logger.debug("Exception in P2P handler, cid = {}", channel.getId(), cause);

        stopTimers();

        ctx.close();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Message msg) throws InterruptedException {
        logger.trace("Received message: " + msg);
        MessageRoundtrip mr = msgQueue.receivedMessage(msg);

        switch (msg.getCode()) {
        /* p2p */
        case DISCONNECT: {
            logger.info("Received DISCONNECT msg, reason = {}", ((DisconnectMessage) msg).getReason());
            stopTimers();
            ctx.close();
            break;
        }
        case HELLO: {
            HelloMessage helloMsg = (HelloMessage) msg;
            Peer peer = helloMsg.getPeer();

            ReasonCode error = null;
            if (!isSupported(peer.getP2pVersion())) {
                error = ReasonCode.BAD_PROTOCOL;
            } else if (client.getPeerId().equals(peer.getPeerId()) || channelMgr.isActivePeer(peer.getPeerId())) {
                error = ReasonCode.DUPLICATE_PEER_ID;
            } else if (channelMgr.isActiveIP(channel.getRemoteIp()) // connected
                    && Config.isMainNet()) { // main net
                error = ReasonCode.BAD_PEER;
            } else if (!isValid(helloMsg)) {
                error = ReasonCode.INVALID_HANDSHAKE;
            }

            if (error == null) {
                // update peer state
                channel.onActive(peer);

                // reply with a WORLD message
                peer = new Peer(client.getIp(), client.getPort(), Config.P2P_VERSION, Config.getClientId(false),
                        client.getPeerId(), chain.getLatestBlockNumber());
                WorldMessage worldMsg = new WorldMessage(peer, client.getCoinbase());
                msgQueue.sendMessage(worldMsg);

                // handshake done
                onHandshakeDone(peer);
            } else {
                msgQueue.disconnect(error);
            }
            break;
        }
        case WORLD: {
            // update peer state
            WorldMessage worldMsg = (WorldMessage) msg;
            Peer peer = worldMsg.getPeer();
            channel.onActive(peer);

            // handshake done
            onHandshakeDone(peer);
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

        /* sync */
        case GET_BLOCK: {
            if (isHandshakeDone) {
                GetBlockMessage m = (GetBlockMessage) msg;
                Block block = chain.getBlock(m.getNumber());
                channel.getMessageQueue().sendMessage(new BlockMessage(block));
            }
            break;
        }
        case BLOCK: {
            if (isHandshakeDone) {
                sync.onMessage(channel, msg);
            }
            break;
        }
        case GET_BLOCK_HEADER: {
            if (isHandshakeDone) {
                GetBlockHeaderMessage m = (GetBlockHeaderMessage) msg;
                BlockHeader header = chain.getBlockHeader(m.getNumber());
                channel.getMessageQueue().sendMessage(new BlockHeaderMessage(header));
            }
            break;
        }
        case BLOCK_HEADER: {
            if (isHandshakeDone) {
                sync.onMessage(channel, msg);
            }
            break;
        }

        /* consensus */
        case BFT_NEW_HEIGHT:
        case BFT_NEW_VIEW:
        case BFT_PROPOSAL:
        case BFT_VOTE: {
            if (isHandshakeDone) {
                consenus.onMessage(channel, msg);
            }
            break;
        }

        default: {
            ctx.fireChannelRead(msg);
            break;
        }
        }
    }

    /**
     * Checks if a HELLO message is valid.
     * 
     * @return
     */
    private boolean isValid(HelloMessage msg) {
        long now = System.currentTimeMillis();

        return msg.validate() //
                && Math.abs(now - msg.getTimestamp()) <= Config.NET_HANDSHAKE_EXPIRE //
                && (Config.isDevNet() || channel.getRemoteIp().equals(msg.getPeer().getIp()));
    }

    /**
     * When handshake is done.
     * 
     * @param peer
     */
    private void onHandshakeDone(Peer peer) {
        if (!isHandshakeDone) {
            // notify consensus about peer height
            consenus.onMessage(channel, new BFTNewHeightMessage(peer.getLatestBlockNumber() + 1));

            // start peers exchange
            getNodes = exec.scheduleAtFixedRate(() -> {
                msgQueue.sendMessage(new GetNodesMessage());
            }, channel.isInbound() ? 2 : 0, 2, TimeUnit.MINUTES);

            // start ping pong
            pingPong = exec.scheduleAtFixedRate(() -> {
                msgQueue.sendMessage(new PingMessage());
            }, channel.isInbound() ? 1 : 0, 1, TimeUnit.MINUTES);

            // set indicator
            isHandshakeDone = true;
        }
    }

    /**
     * Returns whether the p2p version is supported.
     * 
     * @param version
     * @return
     */
    private boolean isSupported(short version) {
        return ArrayUtils.contains(SUPPORTED_VERSIONS, version);
    }

    /**
     * Stops all scheduled timers and the message queue.
     */
    private void stopTimers() {
        if (getNodes != null) {
            getNodes.cancel(false);
            getNodes = null;
        }

        if (pingPong != null) {
            pingPong.cancel(false);
            pingPong = null;
        }

        msgQueue.close();
    }
}