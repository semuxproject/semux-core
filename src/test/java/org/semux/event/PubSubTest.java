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

import org.junit.Test;

public class PubSubTest {

    @Test
    public void testPubSub() {
        final int repeats = 10;
        AtomicInteger dispatched = new AtomicInteger(0);
        PubSub.getInstance().subscribe(TestEvent1.class, event -> dispatched.incrementAndGet());
        for (int i = 0; i < repeats; i++) {
            new Thread(() -> PubSub.getInstance().publish(new TestEvent1())).run();
        }
        await().until(() -> dispatched.get() == repeats);
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
