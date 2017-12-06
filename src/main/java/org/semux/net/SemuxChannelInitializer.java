/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;

import org.semux.Kernel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SemuxChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxChannelInitializer.class);

    private Kernel kernel;
    private ChannelManager channelMgr;

    private InetSocketAddress remoteAddress;
    private boolean discoveryMode;

    /**
     * Create an instance of SemuxChannelInitializer.
     *
     * @param kernel
     *            the kernel instance
     * @param remoteAddress
     *            the address of the remote peer, or null if in server mode
     * @param discoveryMode
     *            whether in discovery mode or not
     */
    public SemuxChannelInitializer(Kernel kernel, InetSocketAddress remoteAddress, boolean discoveryMode) {
        this.kernel = kernel;
        this.channelMgr = kernel.getChannelManager();

        this.remoteAddress = remoteAddress;
        this.discoveryMode = discoveryMode;
    }

    public SemuxChannelInitializer(Kernel kernel, InetSocketAddress remoteAddress) {
        this(kernel, remoteAddress, false);
    }

    @Override
    public void initChannel(NioSocketChannel ch) throws Exception {
        try {
            InetSocketAddress address = isServerMode() ? ch.remoteAddress() : remoteAddress;
            logger.debug("New {} channel: remoteAddress = {}:{}", isServerMode() ? "inbound" : "outbound",
                    address.getAddress().getHostAddress(), address.getPort());

            if (isInbound() && channelMgr.isBlocked(address)) {
                // avoid too frequent connection attempts
                logger.debug("Drop inbound connection {}:{}", address.getAddress().getHostAddress(), address.getPort());
                ch.disconnect();
                return;
            }

            Channel channel = new Channel();
            channel.init(ch.pipeline(), isServerMode(), address, kernel);
            if (!isDiscoveryMode()) {
                channelMgr.add(channel);
            }

            // limit the size of receiving buffer
            int bufferSize = Frame.HEADER_SIZE + kernel.getConfig().netMaxFrameSize();
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(bufferSize));
            ch.config().setOption(ChannelOption.SO_RCVBUF, bufferSize);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

            // notify disconnection to channel manager
            ch.closeFuture().addListener(future -> {
                if (!isDiscoveryMode()) {
                    channelMgr.remove(channel);
                }
            });
        } catch (Exception e) {
            logger.error("Exception in channel initializer", e);
        }
    }

    /**
     * Returns whether is in server mode.
     *
     * @return
     */
    public boolean isServerMode() {
        return remoteAddress == null;
    }

    /**
     * Returns whether is in discovery mode.
     *
     * @return
     */
    public boolean isDiscoveryMode() {
        return discoveryMode;
    }
}
