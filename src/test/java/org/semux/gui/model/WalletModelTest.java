/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.semux.core.Block;
import org.semux.crypto.EdDSA;
import org.semux.net.Peer;

@RunWith(MockitoJUnitRunner.class)
public class WalletModelTest {

    private WalletModel model;

    @Before
    public void setUp() throws IOException {
        model = spy(new WalletModel());
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFireEvent() throws InterruptedException {
        AtomicBoolean actionPerformed = new AtomicBoolean(false);
        ActionListener listener = mock(ActionListener.class);
        doAnswer((Answer<Object>) invocation -> {
            actionPerformed.set(true);
            return null;
        }).when(listener).actionPerformed(any());
        model.addListener(listener);

        new Thread(() -> model.fireUpdateEvent()).start();

        await().until(actionPerformed::get);

        InOrder inOrder = Mockito.inOrder(model, listener);
        inOrder.verify(model).updateView();
        inOrder.verify(listener).actionPerformed(any());
    }

    @Test
    public void testCoinbase() {
        EdDSA key = new EdDSA();

        assertNull(model.getCoinbase());
        model.setCoinbase(key);
        assertEquals(key.toAddressString(), model.getCoinbase().toAddressString());
    }

    @Test
    public void testLatestBlock() {
        Block block = mock(Block.class);
        when(block.getNumber()).thenReturn(1L);

        assertEquals(null, model.getLatestBlock());
        model.setLatestBlock(block);
        assertEquals(1, model.getLatestBlock().getNumber());
    }

    @Test
    public void testDelegates() {
        WalletDelegate d = mock(WalletDelegate.class);

        assertEquals(0, model.getDelegates().size());
        model.setDelegates(Collections.singletonList(d));
        assertEquals(d, model.getDelegates().get(0));
    }

    @Test
    public void testActivePeers() {
        Peer peer = mock(Peer.class);
        Map<String, Peer> map = new HashMap<>();
        map.put("p", peer);

        assertEquals(0, model.getActivePeers().size());
        model.setActivePeers(map);
        assertEquals(peer, model.getActivePeers().get("p"));
    }

    @Test
    public void testTotalAvailable() {
        WalletAccount wa1 = mock(WalletAccount.class);
        WalletAccount wa2 = mock(WalletAccount.class);
        when(wa1.getKey()).thenReturn(new EdDSA());
        when(wa1.getAvailable()).thenReturn(1L);
        when(wa2.getKey()).thenReturn(new EdDSA());
        when(wa2.getAvailable()).thenReturn(2L);

        assertEquals(0, model.getTotalAvailable());
        model.setAccounts(Arrays.asList(wa1, wa2));
        assertEquals(3, model.getTotalAvailable());

        assertThat(model.getAccounts(), equalTo(Arrays.asList(wa1, wa2)));
    }

    @Test
    public void testTotalLocked() {
        WalletAccount wa1 = mock(WalletAccount.class);
        WalletAccount wa2 = mock(WalletAccount.class);
        when(wa1.getKey()).thenReturn(new EdDSA());
        when(wa1.getLocked()).thenReturn(1L);
        when(wa2.getKey()).thenReturn(new EdDSA());
        when(wa2.getLocked()).thenReturn(2L);

        assertEquals(0, model.getTotalLocked());
        model.setAccounts(Arrays.asList(wa1, wa2));
        assertEquals(3, model.getTotalLocked());
    }
}
