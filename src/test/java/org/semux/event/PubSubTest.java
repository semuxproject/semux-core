/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.event;

import static junit.framework.TestCase.fail;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

public class PubSubTest {

    @Test
    public void testPubSub() {
        final int fuzz1 = RandomUtils.nextInt(0, 10);
        final int fuzz2 = RandomUtils.nextInt(0, 10);
        AtomicInteger dispatched1 = new AtomicInteger(0);
        AtomicInteger dispatched2 = new AtomicInteger(0);

        PubSub.getInstance().subscribe(TestEvent1.class, event -> dispatched1.incrementAndGet());
        PubSub.getInstance().subscribe(TestEvent2.class, event -> dispatched2.incrementAndGet());
        for (int i = 0; i < fuzz1; i++) {
            new Thread(() -> PubSub.getInstance().publish(new TestEvent1())).run();
        }
        for (int i = 0; i < fuzz2; i++) {
            new Thread(() -> PubSub.getInstance().publish(new TestEvent2())).run();
        }
        await().until(() -> dispatched1.get() == fuzz1);
        await().until(() -> dispatched2.get() == fuzz2);
    }

    @Test
    public void testUnsubscribe() {
        PubSubSubscriber subscriber = event -> fail();
        PubSub.getInstance().subscribe(TestEvent1.class, subscriber);
        PubSub.getInstance().unsubscribe(TestEvent1.class, subscriber);
        PubSub.getInstance().publish(new TestEvent1());
    }

    @Test
    public void testUnsubscribeAll() {
        PubSubSubscriber subscriber = event -> fail();
        PubSub.getInstance().subscribe(TestEvent1.class, subscriber);
        PubSub.getInstance().subscribe(TestEvent2.class, subscriber);
        PubSub.getInstance().unsubscribeAll(subscriber);
        PubSub.getInstance().publish(new TestEvent1());
        PubSub.getInstance().publish(new TestEvent1());
    }

    private static class TestEvent1 implements PubSubEvent {
    }

    private static class TestEvent2 implements PubSubEvent {
    }
}
