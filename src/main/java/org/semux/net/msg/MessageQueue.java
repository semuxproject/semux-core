/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.config.Config;
import org.semux.net.msg.p2p.DisconnectMessage;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * This class contains the logic for sending messages.
 */
public class MessageQueue {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueue.class);

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private final AtomicInteger cnt = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            return new Thread(r, "msg-" + cnt.getAndIncrement());
        }
    });

    private final Config config;

    private final Queue<MessageWrapper> requests = new ConcurrentLinkedQueue<>();
    private final Queue<MessageWrapper> responses = new ConcurrentLinkedQueue<>();
    private final Queue<MessageWrapper> prioritizedResponses = new ConcurrentLinkedQueue<>();

    private ChannelHandlerContext ctx;
    private ScheduledFuture<?> timerTask;
    private boolean initialized;

    /**
     * Create a message queue with the specified maximum queue size.
     *
     * @param config
     */
    public MessageQueue(Config config) {
        this.config = config;
    }

    /**
     * Activates this message queue and binds it to the channel.
     *
     * @param ctx
     */
    public synchronized void activate(ChannelHandlerContext ctx) {
        if (!initialized) {
            this.ctx = ctx;
            this.timerTask = timer.scheduleAtFixedRate(() -> {
                try {
                    nudgeQueue();
                } catch (Exception t) {
                    logger.error("Exception in MessageQueue", t);
                }
            }, 1, 1, TimeUnit.MILLISECONDS);

            initialized = true;
        }
    }

    /**
     * Deactivates this message queue.
     */
    public synchronized void deactivate() {
        if (initialized) {
            this.timerTask.cancel(false);

            initialized = false;
        }
    }

    /**
     * Returns if this message queue is idle.
     *
     * @return true if request/response queues are empty, otherwise false
     */
    public boolean isIdle() {
        return size() == 0;
    }

    /**
     * Disconnects aggressively.
     *
     * @param code
     */
    public void disconnect(ReasonCode code) {
        logger.debug("Disconnect: reason = {}", code);

        deactivate();

        ctx.writeAndFlush(new DisconnectMessage(code));
        ctx.close();
    }

    /**
     * Adds a message to the sending queue.
     *
     * @param msg
     *            the message to be sent
     * @return true if the message is successfully added to the queue, otherwise
     *         false
     */
    public boolean sendMessage(Message msg) {
        // not atomic or synchronized
        if (!initialized) {
            return false;
        }

        int maxQueueSize = config.netMaxMessageQueueSize();
        if (size() >= maxQueueSize) {
            disconnect(ReasonCode.MESSAGE_QUEUE_FULL);
            return false;
        }

        if (msg.getResponseMessageClass() != null) {
            requests.add(new MessageWrapper(msg));
        } else {
            if (config.netPrioritizedMessages().contains(msg.getCode())) {
                prioritizedResponses.add(new MessageWrapper(msg));
            } else {
                responses.add(new MessageWrapper(msg));
            }
        }
        return true;
    }

    /**
     * Notifies this message queue that a new message has been received.
     *
     * @param msg
     */
    public MessageWrapper onMessageReceived(Message msg) {
        if (requests.peek() != null) {
            MessageWrapper mw = requests.peek();
            Message m = mw.getMessage();

            if (m.getResponseMessageClass() != null && msg.getClass() == m.getResponseMessageClass()) {
                mw.answer();
                return mw;
            }
        }

        return null;
    }

    /**
     * Returns the number of messages in queue.
     *
     * @return
     */
    public int size() {
        return requests.size() + responses.size() + prioritizedResponses.size();
    }

    protected void nudgeQueue() {
        removeAnsweredMessage(requests.peek());

        // send responses
        sendToWire(prioritizedResponses.poll());
        sendToWire(responses.poll());

        // send requests
        sendToWire(requests.peek());
    }

    protected void removeAnsweredMessage(MessageWrapper mw) {
        if (mw != null && mw.isAnswered()) {
            requests.remove();
        }
    }

    protected void sendToWire(MessageWrapper mw) {

        if (mw != null && mw.getRetries() == 0) {
            Message msg = mw.getMessage();

            logger.trace("Wiring message: {}", msg);
            ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            logger.trace("Message {} to {} took {} ms", msg.code, ctx.channel().remoteAddress(),
                    TimeUtil.currentTimeMillis() - mw.getLastTimestamp());

            if (msg.getResponseMessageClass() != null) {
                mw.increaseRetries();
                mw.saveTime();
            }
        }
    }
}
