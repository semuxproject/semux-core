/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.semux.net.msg.p2p.NodesMessage.MAX_NODES;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.core.BftManager;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.BlockPart;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
import org.semux.net.NodeManager.Node;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageQueue;
import org.semux.net.msg.MessageWrapper;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BlockHeaderMessage;
import org.semux.net.msg.consensus.BlockMessage;
import org.semux.net.msg.consensus.BlockPartsMessage;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;
import org.semux.net.msg.consensus.GetBlockMessage;
import org.semux.net.msg.consensus.GetBlockPartsMessage;
import org.semux.net.msg.consensus.NewHeightMessage;
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
import org.semux.util.SystemUtil;
import org.semux.util.TimeUtil;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Semux P2P message handler
 */
public class SemuxP2pHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger(SemuxP2pHandler.class);

    private static final ScheduledExecutorService exec = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactory() {
                private final AtomicInteger cnt = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "p2p-" + cnt.getAndIncrement());
                }
            });

    private final Channel channel;
    private final Config config;
    private final Blockchain chain;
    private final PendingManager pendingMgr;
    private final ChannelManager channelMgr;
    private final NodeManager nodeMgr;
    private final PeerClient client;
    private final SyncManager sync;
    private final BftManager bft;
    private final MessageQueue msgQueue;

    private AtomicBoolean isHandshakeDone = new AtomicBoolean(false);

    private ScheduledFuture<?> getNodes = null;
    private ScheduledFuture<?> pingPong = null;

    private byte[] secret = Bytes.random(InitMessage.SECRET_LENGTH);
    private long timestamp = TimeUtil.currentTimeMillis();

    // whether to use new handshake for this channel
    private boolean useNewHandShake;

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
        this.bft = kernel.getBftManager();

        this.msgQueue = channel.getMessageQueue();

        this.useNewHandShake = isNewHandShakeEnabled(config.network());
    }

    /**
     * Client prior to v1.3.0 does not accept unknown message type. To make sure
     * they are able to connect to the network; we're partially applying the new
     * handshake protocol.
     *
     * Especially, after receiving an inbound connection, do not send the INIT
     * message immediately; otherwise the connection will be killed by the peer.
     *
     * @return
     */
    protected boolean isNewHandShakeEnabled(Network network) {

        if (SystemUtil.isJUnitTest() || network != Network.MAINNET) {
            return true;
        }

        // To developer: set 0 to test old handshake, or set 1 to test new handshake.

        return Math.random() < 0.5;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler active, remoteIp = {}", channel.getRemoteIp());

        // activate message queue
        msgQueue.activate(ctx);

        // disconnect if too many connections
        if (channel.isInbound() && channelMgr.size() >= config.netMaxInboundConnections()) {
            msgQueue.disconnect(ReasonCode.TOO_MANY_PEERS);
            return;
        }

        if (useNewHandShake) {
            if (channel.isInbound()) {
                msgQueue.sendMessage(new InitMessage(secret, timestamp));
            } else {
                // in this case, the connection will never be established if the peer
                // doesn't enable new handshake protocol.
            }
        } else {
            if (channel.isOutbound()) {
                Message helloMessage = new org.semux.net.msg.p2p.handshake.v1.HelloMessage(
                        config.network(), config.networkVersion(), client.getPeerId(),
                        client.getIp(), client.getPort(),
                        config.getClientId(),
                        chain.getLatestBlockNumber(),
                        client.getCoinbase());
                msgQueue.sendMessage(helloMessage);
            }
        }

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P handler inactive, remoteIp = {}", channel.getRemoteIp());

        // deactivate the message queue
        msgQueue.deactivate();

        // stop scheduled workers
        if (getNodes != null) {
            getNodes.cancel(false);
            getNodes = null;
        }

        if (pingPong != null) {
            pingPong.cancel(false);
            pingPong = null;
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("Exception in P2P handler, remoteIp = {}", channel.getRemoteIp(), cause);

        // close connection on exception
        ctx.close();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Message msg) throws InterruptedException {
        logger.trace("Received message: {}", msg);
        MessageWrapper request = msgQueue.onMessageReceived(msg);

        switch (msg.getCode()) {
        /* p2p */
        case DISCONNECT:
            onDisconnect(ctx, (DisconnectMessage) msg);
            break;
        case HELLO:
            if (!useNewHandShake)
                onHello((org.semux.net.msg.p2p.handshake.v1.HelloMessage) msg);
            break;
        case WORLD:
            if (!useNewHandShake)
                onWorld((org.semux.net.msg.p2p.handshake.v1.WorldMessage) msg);
            break;
        case PING:
            onPing();
            break;
        case PONG:
            onPong(request);
            break;
        case GET_NODES:
            onGetNodes();
            break;
        case NODES:
            onNodes((NodesMessage) msg);
            break;
        case TRANSACTION:
            onTransaction((TransactionMessage) msg);
            break;
        case HANDSHAKE_INIT:
            if (useNewHandShake)
                onHandshakeInit((InitMessage) msg);
            break;
        case HANDSHAKE_HELLO:
            if (useNewHandShake)
                onHandshakeHello((HelloMessage) msg);
            break;
        case HANDSHAKE_WORLD:
            if (useNewHandShake)
                onHandshakeWorld((WorldMessage) msg);
            break;

        /* sync */
        case GET_BLOCK:
        case BLOCK:
        case GET_BLOCK_HEADER:
        case BLOCK_HEADER:
        case GET_BLOCK_PARTS:
        case BLOCK_PARTS:
            onSync(msg);
            break;

        /* bft */
        case BFT_NEW_HEIGHT:
        case BFT_NEW_VIEW:
        case BFT_PROPOSAL:
        case BFT_VOTE:
            onBft(msg);
            break;

        default:
            ctx.fireChannelRead(msg);
            break;
        }
    }

    protected void onDisconnect(ChannelHandlerContext ctx, DisconnectMessage msg) {
        ReasonCode reason = msg.getReason();
        logger.info("Received a DISCONNECT message: reason = {}, remoteIP = {}",
                reason, channel.getRemoteIp());

        ctx.close();
    }

    protected void onHello(org.semux.net.msg.p2p.handshake.v1.HelloMessage msg) {
        Peer peer = msg.getPeer();

        // check peer
        ReasonCode code = checkPeer(peer, false);
        if (code != null) {
            msgQueue.disconnect(code);
            return;
        }

        // check message
        if (!msg.validate(config) || (config.network() == Network.MAINNET && !channel.getRemoteIp()
                .equals(msg.getPeer().getIp()))) {
            msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
            return;
        }

        // reply with a WORLD message
        msgQueue.sendMessage(new org.semux.net.msg.p2p.handshake.v1.WorldMessage(
                config.network(), config.networkVersion(), client.getPeerId(),
                client.getIp(), client.getPort(),
                config.getClientId(),
                chain.getLatestBlockNumber(),
                client.getCoinbase()));

        // handshake done
        onHandshakeDone(peer);
    }

    protected void onWorld(org.semux.net.msg.p2p.handshake.v1.WorldMessage msg) {
        Peer peer = msg.getPeer();

        // check peer
        ReasonCode code = checkPeer(peer, false);
        if (code != null) {
            msgQueue.disconnect(code);
            return;
        }

        // check message
        if ((config.network() == Network.MAINNET && !channel.getRemoteIp().equals(msg.getPeer().getIp()))
                || !msg.validate(config)) {
            msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
            return;
        }

        // handshake done
        onHandshakeDone(peer);
    }

    protected void onPing() {
        PongMessage pong = new PongMessage();
        msgQueue.sendMessage(pong);
    }

    protected void onPong(MessageWrapper request) {
        if (request != null) {
            long latency = TimeUtil.currentTimeMillis() - request.getLastTimestamp();
            channel.getRemotePeer().setLatency(latency);
        }
    }

    protected void onGetNodes() {
        List<InetSocketAddress> activeAddresses = new ArrayList<>(channelMgr.getActiveAddresses());
        Collections.shuffle(activeAddresses); // shuffle the list to balance the load on nodes
        NodesMessage nodesMsg = new NodesMessage(activeAddresses.stream()
                .limit(MAX_NODES).map(Node::new).collect(Collectors.toList()));
        msgQueue.sendMessage(nodesMsg);
    }

    protected void onNodes(NodesMessage msg) {
        if (msg.validate()) {
            nodeMgr.addNodes(msg.getNodes());
        }
    }

    protected void onTransaction(TransactionMessage msg) {
        pendingMgr.addTransaction(msg.getTransaction());
    }

    protected void onHandshakeInit(InitMessage msg) {
        // unexpected
        if (channel.isInbound()) {
            return;
        }

        // check message
        if (!msg.validate()) {
            this.msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
            return;
        }

        // record the secret
        this.secret = msg.getSecret();
        this.timestamp = msg.getTimestamp();

        // send the HELLO message
        this.msgQueue.sendMessage(new HelloMessage(config.network(), config.networkVersion(), client.getPeerId(),
                client.getPort(), config.getClientId(), config.getClientCapabilities().toArray(),
                chain.getLatestBlockNumber(),
                secret, client.getCoinbase()));
    }

    protected void onHandshakeHello(HelloMessage msg) {
        // unexpected
        if (channel.isOutbound()) {
            return;
        }
        Peer peer = msg.getPeer(channel.getRemoteIp());

        // check peer
        ReasonCode code = checkPeer(peer, true);
        if (code != null) {
            msgQueue.disconnect(code);
            return;
        }

        // check message
        if (!Arrays.equals(secret, msg.getSecret()) || !msg.validate(config)) {
            msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
            return;
        }

        // send the WORLD message
        this.msgQueue.sendMessage(new WorldMessage(config.network(), config.networkVersion(), client.getPeerId(),
                client.getPort(), config.getClientId(), config.getClientCapabilities().toArray(),
                chain.getLatestBlockNumber(),
                secret, client.getCoinbase()));

        // handshake done
        onHandshakeDone(peer);
    }

    protected void onHandshakeWorld(WorldMessage msg) {
        // unexpected
        if (channel.isInbound()) {
            return;
        }
        Peer peer = msg.getPeer(channel.getRemoteIp());

        // check peer
        ReasonCode code = checkPeer(peer, true);
        if (code != null) {
            msgQueue.disconnect(code);
            return;
        }

        // check message
        if (!Arrays.equals(secret, msg.getSecret()) || !msg.validate(config)) {
            msgQueue.disconnect(ReasonCode.INVALID_HANDSHAKE);
            return;
        }

        // handshake done
        onHandshakeDone(peer);
    }

    protected void onSync(Message msg) {
        if (!isHandshakeDone.get()) {
            return;
        }

        switch (msg.getCode()) {
        case GET_BLOCK: {
            GetBlockMessage m = (GetBlockMessage) msg;
            Block block = chain.getBlock(m.getNumber());
            channel.getMessageQueue().sendMessage(new BlockMessage(block));
            break;
        }
        case GET_BLOCK_HEADER: {
            GetBlockHeaderMessage m = (GetBlockHeaderMessage) msg;
            BlockHeader header = chain.getBlockHeader(m.getNumber());
            channel.getMessageQueue().sendMessage(new BlockHeaderMessage(header));
            break;
        }
        case GET_BLOCK_PARTS: {
            GetBlockPartsMessage m = (GetBlockPartsMessage) msg;
            long number = m.getNumber();
            int parts = m.getParts();

            List<byte[]> partsSerialized = new ArrayList<>();
            Block block = chain.getBlock(number);
            for (BlockPart part : BlockPart.decode(parts)) {
                switch (part) {
                case HEADER:
                    partsSerialized.add(block.getEncodedHeader());
                    break;
                case TRANSACTIONS:
                    partsSerialized.add(block.getEncodedTransactions());
                    break;
                case RESULTS:
                    partsSerialized.add(block.getEncodedResults());
                    break;
                case VOTES:
                    partsSerialized.add(block.getEncodedVotes());
                    break;
                default:
                    throw new UnreachableException();
                }
            }

            channel.getMessageQueue().sendMessage(new BlockPartsMessage(number, parts, partsSerialized));
            break;
        }
        case BLOCK:
        case BLOCK_HEADER:
        case BLOCK_PARTS: {
            sync.onMessage(channel, msg);
            break;
        }
        default:
            throw new UnreachableException();
        }
    }

    protected void onBft(Message msg) {
        if (!isHandshakeDone.get()) {
            return;
        }

        bft.onMessage(channel, msg);
    }

    // =========================
    // Helper methods below
    // =========================

    /**
     * Check whether the peer is valid to connect.
     */
    private ReasonCode checkPeer(Peer peer, boolean newHandShake) {
        // has to be same network
        if (newHandShake && !config.network().equals(peer.getNetwork())) {
            return ReasonCode.BAD_NETWORK;
        }

        // has to be compatible version
        if (config.networkVersion() != peer.getNetworkVersion()) {
            return ReasonCode.BAD_NETWORK_VERSION;
        }

        // not connected
        if (client.getPeerId().equals(peer.getPeerId()) || channelMgr.isActivePeer(peer.getPeerId())) {
            return ReasonCode.DUPLICATED_PEER_ID;
        }

        // validator can't share IP address
        if (chain.getValidators().contains(peer.getPeerId()) // is a validator
                && channelMgr.isActiveIP(channel.getRemoteIp()) // already connected
                && config.network() == Network.MAINNET) { // on main net
            return ReasonCode.VALIDATOR_IP_LIMITED;
        }

        return null;
    }

    /**
     * When handshake is done.
     */
    private void onHandshakeDone(Peer peer) {
        if (isHandshakeDone.compareAndSet(false, true)) {
            // register into channel manager
            channelMgr.onChannelActive(channel, peer);

            // notify bft about peer height
            bft.onMessage(channel, new NewHeightMessage(peer.getLatestBlockNumber() + 1));

            // start peers exchange
            getNodes = exec.scheduleAtFixedRate(() -> msgQueue.sendMessage(new GetNodesMessage()),
                    channel.isInbound() ? 2 : 0, 2, TimeUnit.MINUTES);

            // start ping pong
            pingPong = exec.scheduleAtFixedRate(() -> msgQueue.sendMessage(new PingMessage()),
                    channel.isInbound() ? 1 : 0, 1, TimeUnit.MINUTES);
        } else {
            msgQueue.disconnect(ReasonCode.HANDSHAKE_EXISTS);
        }
    }
}
