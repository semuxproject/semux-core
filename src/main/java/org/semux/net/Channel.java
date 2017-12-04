/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.semux.Kernel;
import org.semux.config.Constants;
import org.semux.net.msg.MessageQueue;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class Channel {
    private static final AtomicLong cnt = new AtomicLong(0);

    private long id;

    private boolean isInbound;
    private InetSocketAddress remoteAddress;

    private MessageQueue msgQueue;
    private Peer remotePeer;

    /**
     * Creates a new channel instance.
     * 
     */
    public Channel() {
        this.id = cnt.getAndIncrement();
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

        this.msgQueue = new MessageQueue(kernel.getConfig());
        this.remotePeer = null;

        // register channel handlers
        pipe.addLast("readTimeoutHandler",
                new ReadTimeoutHandler(Constants.DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS));
        pipe.addLast("frameHandler", new SemuxFrameHandler(kernel.getConfig()));
        pipe.addLast("messageHandler", new SemuxMessageHandler(kernel.getConfig()));
        pipe.addLast("p2pHandler", new SemuxP2pHandler(this, kernel));
    }

    /**
     * Returns the channel id.
     * 
     * @return
     */
    public long getId() {
        return id;
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
        return remotePeer != null;
    }

    /**
     * When peer connection become active.
     * 
     * @param remotePeer
     */
    public void onActive(Peer remotePeer) {
        this.remotePeer = remotePeer;
    }

    /**
     * When peer disconnects.
     */
    public void onDisconnect() {
        this.remotePeer = null;
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
        return "Channel [id=" + id + ", " + (isInbound ? "IN" : "OUT") + ", remotePeer=" + remotePeer + "]";
    }

}
