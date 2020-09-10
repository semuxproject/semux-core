/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.semux.Kernel;
import org.semux.net.msg.MessageQueue;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class Channel {
    private final NioSocketChannel socket;

    private boolean isInbound;
    private InetSocketAddress remoteAddress;
    private Peer remotePeer;

    private MessageQueue msgQueue;

    private boolean isActive;

    /**
     * Creates a new channel instance.
     * 
     */
    public Channel(NioSocketChannel socket) {
        this.socket = socket;
    }

    /**
     * Initializes this channel.
     * 
     * @param pipe
     * @param isInbound
     * @param remoteAddress
     * @param kernel
     */
    public void init(ChannelPipeline pipe, boolean isInbound, InetSocketAddress remoteAddress, Kernel kernel) {
        this.isInbound = isInbound;
        this.remoteAddress = remoteAddress;
        this.remotePeer = null;

        this.msgQueue = new MessageQueue(kernel.getConfig());

        // register channel handlers
        if (isInbound) {
            pipe.addLast("inboundLimitHandler",
                    new ConnectionLimitHandler(kernel.getConfig().netMaxInboundConnectionsPerIp()));
        }
        pipe.addLast("readTimeoutHandler",
                new ReadTimeoutHandler(kernel.getConfig().netChannelIdleTimeout(), TimeUnit.MILLISECONDS));
        pipe.addLast("frameHandler", new SemuxFrameHandler(kernel.getConfig()));
        pipe.addLast("messageHandler", new SemuxMessageHandler(kernel.getConfig()));
        pipe.addLast("p2pHandler", new SemuxP2pHandler(this, kernel));
    }

    /**
     * Closes the underlying socket channel.
     */
    public void close() {
        socket.close();
    }

    /**
     * Returns the message queue.
     * 
     * @return
     */
    public MessageQueue getMessageQueue() {
        return msgQueue;
    }

    /**
     * Returns whether this is an inbound channel.
     * 
     * @return
     */
    public boolean isInbound() {
        return isInbound;
    }

    /**
     * Returns whether this is an outbound channel.
     *
     * @return
     */
    public boolean isOutbound() {
        return !isInbound();
    }

    /**
     * Returns the remote peer.
     * 
     * @return
     */
    public Peer getRemotePeer() {
        return remotePeer;
    }

    /**
     * Returns whether this channel is active.
     * 
     * @return
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets this channel to be active.
     * 
     * @param remotePeer
     */
    public void setActive(Peer remotePeer) {
        this.remotePeer = remotePeer;
        this.isActive = true;
    }

    /**
     * Sets this channel to be inactive.
     */
    public void setInactive() {
        /*
         * Remote peer is not reset because other thread may still hold a reference to
         * this channel
         */
        // this.remotePeer = null;

        this.isActive = false;
    }

    /**
     * Returns the remote address.
     * 
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns remote IP address.
     * 
     * @return
     */
    public String getRemoteIp() {
        return remoteAddress.getAddress().getHostAddress();
    }

    /**
     * Returns remote port.
     * 
     * @return
     */
    public int getRemotePort() {
        return remoteAddress.getPort();
    }

    @Override
    public String toString() {
        return "Channel [" + (isInbound ? "Inbound" : "Outbound") + ", remoteIp = " + getRemoteIp() + ", remotePeer = "
                + remotePeer + "]";
    }
}
