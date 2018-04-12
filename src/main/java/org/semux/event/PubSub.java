/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.semux.Launcher;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PubSub is a global service that provides a communication channel between
 * different components of Semux wallet. This service holds a singleton
 * {@link PubSub#instance} which automatically starts at process startup and
 * stops at process shutdown.
 */
public class PubSub {

    private static Logger logger = LoggerFactory.getLogger(PubSub.class);

    private static PubSub instance = new PubSub();

    static {
        instance.start();
        Launcher.registerShutdownHook("pubsub", () -> instance.stop());
    }

    private LinkedBlockingQueue<PubSubEvent> queue;

    /**
     * [event] => [list of subscribers]
     */
    private ConcurrentHashMap<Class<? extends PubSubEvent>, ConcurrentLinkedQueue<PubSubSubscriber>> subscribers;

    private Thread eventProcessingThread;

    private AtomicBoolean isRunning;

    private PubSub() {
        queue = new LinkedBlockingQueue<>();
        subscribers = new ConcurrentHashMap<>();
        eventProcessingThread = new EventProcessingThread();
        isRunning = new AtomicBoolean(false);
    }

    public static PubSub getInstance() {
        return instance;
    }

    /**
     * Add an event to {@link this#queue}.
     *
     * @param event
     *            the event to be subscribed.
     * @return whether the event is successfully added.
     */
    public boolean publish(PubSubEvent event) {
        return queue
                .add(event);
    }

    /**
     * Subscribe to an event.
     *
     * @param event
     *            the event.
     * @param subscriber
     *            the subscriber.
     * @return whether the event is successfully subscribed.
     */
    public boolean subscribe(Class<? extends PubSubEvent> event, PubSubSubscriber subscriber) {
        return subscribers
                .computeIfAbsent(event, k -> new ConcurrentLinkedQueue<>())
                .add(subscriber);
    }

    /**
     * Unsubscribe an event.
     *
     * @param event
     *            the event to be unsubscribed.
     * @param subscriber
     *            the subscriber.
     * @return whether the event is successfully unsubscribed.
     */
    public boolean unsubscribe(Class<? extends PubSubEvent> event, PubSubSubscriber subscriber) {
        ConcurrentLinkedQueue q = subscribers.get(event);
        if (q != null) {
            return q.remove(subscriber);
        } else {
            return false;
        }
    }

    /**
     * Unsubscribe from all events.
     *
     * @param subscriber
     *            the subscriber.
     */
    public void unsubscribeAll(final PubSubSubscriber subscriber) {
        subscribers.values().forEach(q -> q.remove(subscriber));
    }

    /**
     * Start the {@link this#eventProcessingThread}.
     *
     * @throws UnreachableException
     *             this method should only be called for once, otherwise an
     *             exception will be thrown.
     */
    private synchronized void start() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new UnreachableException("PubSub service can be started for only once");
        }

        eventProcessingThread.start();
        logger.info("PubSub service started");
    }

    /**
     * Stop the {@link this#eventProcessingThread}.
     */
    private synchronized void stop() {
        eventProcessingThread.interrupt();
        logger.info("PubSub service stopped");
    }

    /**
     * This thread will be continuously polling for new events until PubSub is
     * stopped.
     */
    private class EventProcessingThread extends Thread {

        private EventProcessingThread() {
            super("pubsub-event-processing");
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                final PubSubEvent event;
                try {
                    event = queue.take();
                } catch (InterruptedException e) {
                    interrupt();
                    return;
                }

                ConcurrentLinkedQueue<PubSubSubscriber> q = subscribers.get(event.getClass());
                if (q != null) {
                    for (PubSubSubscriber subscriber : q) {
                        subscriber.onPubSubEvent(event);
                    }
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isRunning.set(false);
        }
    }
}
