/**
 * Copyright (c) 2017 The Semux Developers
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

import org.semux.Config;
import org.semux.net.msg.p2p.DisconnectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * This class contains the logic for sending messages.
 * 
 */
public class MessageQueue {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueue.class);

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            return new Thread(r, "msg-queue-" + cnt.getAndIncrement());
        }
    });

    private Queue<MessageRoundtrip> requests = new ConcurrentLinkedQueue<>();
    private Queue<MessageRoundtrip> responses = new ConcurrentLinkedQueue<>();
    private Queue<MessageRoundtrip> prioritizedResponses = new ConcurrentLinkedQueue<>();

    private ChannelHandlerContext ctx = null;
    private int maxQueueSize;

    private ScheduledFuture<?> timerTask;
    private volatile boolean isRunning;

    /**
     * Create a message queue, with the default maximum queue size.
     * 
     */
    public MessageQueue() {
        this(Config.NET_MAX_QUEUE_SIZE);
    }

    /**
     * Create a message queue with the specified maximum queue size.
     * 
     * @param maxQueueSize
     */
    public MessageQueue(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Bind this message queue to a channel, and start scheduled sending.
     * 
     * @param ctx
     */
    public void activate(ChannelHandlerContext ctx) {
        if (!isRunning) {
            this.ctx = ctx;
            this.timerTask = timer.scheduleAtFixedRate(() -> {
                try {
                    nudgeQueue();
                } catch (Throwable t) {
                    logger.error("Exception in MessageQueue", t);
                }
            }, Config.NET_MAX_QUEUE_RATE, Config.NET_MAX_QUEUE_RATE, TimeUnit.MILLISECONDS);

            this.isRunning = true;
        }
    }

    /**
     * close this message queue.
     */
    public void close() {
        if (isRunning) {
            this.timerTask.cancel(false);

            this.isRunning = false;
        }
    }

    /**
     * Check if this message queue is idle.
     * 
     * @return true if both request and response queues are empty, otherwise false
     */
    public boolean isIdle() {
        return requests.isEmpty() && responses.isEmpty() && prioritizedResponses.isEmpty();
    }

    /**
     * Disconnect aggressively.
     * 
     * @param code
     */
    public void disconnect(ReasonCode code) {
        logger.debug("Disconnect: reason = {}", code);

        // Turn off message queue, and stop sending/receiving messages imediately.
        close();

        // Send reason code and flush all enqueued message (to avoid
        // ClosedChannelException)
        try {
            ctx.writeAndFlush(new DisconnectMessage(code)).await(10_000);
        } catch (InterruptedException e) {
            // do nothing
        } finally {
            ctx.close();
        }
    }

    /**
     * Add a message to the sending queue.
     * 
     * @param msg
     *            the message to be sent
     * @return true if the message is successfully added to the queue, otherwise
     *         false
     */
    public boolean sendMessage(Message msg) {
        if (!isRunning) {
            return false;
        }

        if (requests.size() >= maxQueueSize || responses.size() >= maxQueueSize
                || prioritizedResponses.size() >= maxQueueSize) {
            disconnect(ReasonCode.BAD_PEER);
            return false;
        }

        if (msg.getResponseMessageClass() != null) {
            requests.add(new MessageRoundtrip(msg));
        } else {
            if (Config.PRIORITIZED_MESSAGES.contains(msg.getCode())) {
                prioritizedResponses.add(new MessageRoundtrip(msg));
            } else {
                responses.add(new MessageRoundtrip(msg));
            }
        }
        return true;
    }

    /**
     * Notify this message queue that a new message has been received.
     * 
     * @param msg
     */
    public MessageRoundtrip receivedMessage(Message msg) {
        if (requests.peek() != null) {
            MessageRoundtrip mr = requests.peek();
            Message m = mr.getMessage();

            if (m.getResponseMessageClass() != null && msg.getClass() == m.getResponseMessageClass()) {
                mr.answer();
                return mr;
            }
        }

        return null;
    }

    private void nudgeQueue() {
        removeAnsweredMessage(requests.peek());

        // send responses
        MessageRoundtrip msg = prioritizedResponses.poll();
        sendToWire(msg == null ? responses.poll() : msg);

        // send requests
        sendToWire(requests.peek());
    }

    private void removeAnsweredMessage(MessageRoundtrip mr) {
        if (mr != null && mr.isAnswered()) {
            requests.remove();
        }
    }

    private void sendToWire(MessageRoundtrip mr) {

        if (mr != null && mr.getRetries() == 0) {
            Message msg = mr.getMessage();

            logger.trace("Wiring message: {}", msg);
            ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (msg.getResponseMessageClass() != null) {
                mr.increseRetries();
                mr.saveTime();
            }
        }
    }
}
