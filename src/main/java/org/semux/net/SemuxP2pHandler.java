/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.semux.net.msg.p2p.NodesMessage.MAX_NODES;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
import org.semux.net.NodeManager.Node;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageQueue;
import org.semux.net.msg.MessageWrapper;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BlockHeaderMessage;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.consensus.NewHeightMessage;
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

    private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "p2p-" + cnt.getAndIncrement());
        }
    });

    private Channel channel;

    private Config config;

    private Blockchain chain;
    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;
    private PeerClient client;

    private SyncManager sync;
    private Consensus consensus;

    private MessageQueue msgQueue;
    private boolean isHandshakeDone;

    private ScheduledFuture<?> getNodes = null;
    private ScheduledFuture<?> pingPong = null;

    /**
     * Creates a new P2P handler.
     *
     * @param channel
     */
    public SemuxP2pHandler(Channel channel, Kernel kernel) {
        this.channel = channel;
        this.config = kernel.getConfig();

        this.chain = kernel.getBlockchain();
        this.pendingMgr = kernel.getPendingManager();
        this.channelMgr = kernel.getChannelManager();
        this.nodeMgr = kernel.getNodeManager();
        this.client = kernel.getClient();

        this.sync = kernel.getSyncManager();
        this.consensus = kernel.getConsensus();

        this.msgQueue = channel.getMessageQueue();
        this.isHandshakeDone = false;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler active, cid = {}", channel.getId());

        // activate message queue
        msgQueue.activate(ctx);

        // disconnect if too many connections
        if (channel.isInbound() && channelMgr.size() >= config.netMaxInboundConnections()) {
            msgQueue.disconnect(ReasonCode.TOO_MANY_PEERS);
            return;
        }

        // send a HELLO message to initiate handshake
        if (!channel.isInbound()) {
            Peer peer = new Peer(client.getIp(), client.getPort(), config.networkVersion(), config.getClientId(),
                    client.getPeerId(), chain.getLatestBlockNumber(), config.capabilitySet());
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
        logger.trace("Received message: {}", msg);
        MessageWrapper mr = msgQueue.onMessageReceived(msg);

        switch (msg.getCode()) {
        /* p2p */
        case DISCONNECT: {
            logger.info("Received DISCONNECT message: reason = {}, remoteIP = {}",
                    ((DisconnectMessage) msg).getReason(), channel.getRemoteIp());
            stopTimers();
            ctx.close();
            break;
        }
        case HELLO: {
            HelloMessage helloMsg = (HelloMessage) msg;
            Peer peer = helloMsg.getPeer();

            ReasonCode error = null;
            if (!isSupported(peer)) {
                error = ReasonCode.INCOMPATIBLE_PROTOCOL;
            } else if (client.getPeerId().equals(peer.getPeerId()) || channelMgr.isActivePeer(peer.getPeerId())) {
                error = ReasonCode.DUPLICATED_PEER_ID;

            } else if (chain.getValidators().contains(peer.getPeerId()) // validator
                    && channelMgr.isActiveIP(channel.getRemoteIp()) // connected
                    && config.network() == Network.MAINNET) { // main net
                error = ReasonCode.VALIDATOR_IP_LIMITED;

            } else if (!isValid(helloMsg)) {
                error = ReasonCode.INVALID_HANDSHAKE;
            }

            if (error == null) {
                // notify channel manager
                channelMgr.onChannelActive(channel, peer);

                // reply with a WORLD message
                peer = new Peer(client.getIp(), client.getPort(), config.networkVersion(), config.getClientId(),
                        client.getPeerId(), chain.getLatestBlockNumber(), config.capabilitySet());
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

            if (!isValid(worldMsg)) {
                msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
                break;
            }

            if (!isSupported(worldMsg.getPeer())) {
                msgQueue.disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
                break;
            }

            Peer peer = worldMsg.getPeer();
            channelMgr.onChannelActive(channel, peer);

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
            NodesMessage nodesMsg = new NodesMessage(channelMgr.getActiveAddresses()
                    .stream().limit(MAX_NODES).map(Node::new)
                    .collect(Collectors.toList()));
            msgQueue.sendMessage(nodesMsg);
            break;
        }
        case NODES: {
            // TODO: record how frequent this message is being used, ban if necessary
            NodesMessage nodesMsg = (NodesMessage) msg;
            if (nodesMsg.validate()) {
                nodeMgr.addNodes(nodesMsg.getNodes());
            }
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
                consensus.onMessage(channel, msg);
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
     * Checks if a HELLO message is success.
     *
     * @return
     */
    private boolean isValid(HelloMessage msg) {
        return msg.validate(config)
                && (config.network() == Network.DEVNET || channel.getRemoteIp().equals(msg.getPeer().getIp()));
    }

    /**
     * Checks if a World message is success.
     *
     * @return
     */
    private boolean isValid(WorldMessage msg) {
        return msg.validate(config)
                && (config.network() == Network.DEVNET || channel.getRemoteIp().equals(msg.getPeer().getIp()));
    }

    /**
     * When handshake is done.
     *
     * @param peer
     */
    private void onHandshakeDone(Peer peer) {
        if (!isHandshakeDone) {
            // notify consensus about peer height
            consensus.onMessage(channel, new NewHeightMessage(peer.getLatestBlockNumber() + 1));

            // start peers exchange
            getNodes = exec.scheduleAtFixedRate(() -> msgQueue.sendMessage(new GetNodesMessage()),
                    channel.isInbound() ? 2 : 0, 2, TimeUnit.MINUTES);

            // start ping pong
            pingPong = exec.scheduleAtFixedRate(() -> msgQueue.sendMessage(new PingMessage()),
                    channel.isInbound() ? 1 : 0, 1, TimeUnit.MINUTES);

            // set indicator
            isHandshakeDone = true;
        } else {
            msgQueue.disconnect(ReasonCode.HANDSHAKE_EXISTS);
        }
    }

    /**
     * Returns whether the p2p version is supported.
     *
     * @param peer
     * @return
     */
    private boolean isSupported(Peer peer) {
        if (config.networkVersion() != peer.getNetworkVersion()) {
            return false;
        }

        if (config.network() == Network.MAINNET) {
            return peer.getCapabilities().isSupported(Capability.SEM);
        } else {
            return peer.getCapabilities().isSupported(Capability.SEM_TESTNET);
        }
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

        msgQueue.deactivate();
    }
}
