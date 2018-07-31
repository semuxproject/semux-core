/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.event;

import static junit.framework.TestCase.fail;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PubSubTest {

    private static final PubSub pubSub = PubSubFactory.getDefault();

    @Before
    public void setUp() {
        pubSub.start();
    }

    @After
    public void tearDown() {
        pubSub.stop();
    }

    @Test
    public void testPubSub() {
        final int fuzz1 = RandomUtils.nextInt(0, 10);
        final int fuzz2 = RandomUtils.nextInt(0, 10);
        AtomicInteger dispatched1 = new AtomicInteger(0);
        AtomicInteger dispatched2 = new AtomicInteger(0);

        pubSub.subscribe(event -> dispatched1.incrementAndGet(), TestEvent1.class);
        pubSub.subscribe(event -> dispatched2.incrementAndGet(), TestEvent2.class);
        for (int i = 0; i < fuzz1; i++) {
            new Thread(() -> pubSub.publish(new TestEvent1())).start();
        }
        for (int i = 0; i < fuzz2; i++) {
            new Thread(() -> pubSub.publish(new TestEvent2())).start();
        }
        await().atMost(30, TimeUnit.SECONDS).until(() -> dispatched1.get() == fuzz1);
        await().atMost(30, TimeUnit.SECONDS).until(() -> dispatched2.get() == fuzz2);
    }

    @Test
    public void testUnsubscribe() {
        PubSubSubscriber subscriber = event -> fail();
        pubSub.subscribe(subscriber, TestEvent1.class);
        pubSub.unsubscribe(subscriber, TestEvent1.class);
        pubSub.publish(new TestEvent1());
    }

    @Test
    public void testUnsubscribeAll() {
        PubSubSubscriber subscriber = event -> fail();
        pubSub.subscribe(subscriber, TestEvent1.class);
        pubSub.subscribe(subscriber, TestEvent2.class);
        pubSub.unsubscribeAll(subscriber);
        pubSub.publish(new TestEvent1());
        pubSub.publish(new TestEvent1());
    }

    private static class TestEvent1 implements PubSubEvent {
    }

    private static class TestEvent2 implements PubSubEvent {
    }
}
