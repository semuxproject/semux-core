/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.semux.core.Amount.ZERO;
import static org.semux.core.Unit.NANO_SEM;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.semux.config.MainnetConfig;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.state.Delegate;
import org.semux.crypto.Key;
import org.semux.net.Peer;

@RunWith(MockitoJUnitRunner.class)
public class WalletModelTest {

    private MainnetConfig config;

    private WalletModel model;

    @Before
    public void setUp() {
        config = spy(new MainnetConfig("data"));
        model = spy(new WalletModel(config));
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
        Key key = new Key();

        assertNull(model.getCoinbase());
        model.setCoinbase(key);
        assertEquals(key.toAddressString(), model.getCoinbase().toAddressString());
    }

    @Test
    public void testLatestBlock() {
        Block block = mock(Block.class);
        when(block.getNumber()).thenReturn(1L);

        assertNull(model.getLatestBlock());
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
        when(wa1.getKey()).thenReturn(new Key());
        when(wa1.getAvailable()).thenReturn(NANO_SEM.of(1));
        when(wa2.getKey()).thenReturn(new Key());
        when(wa2.getAvailable()).thenReturn(NANO_SEM.of(2));

        assertEquals(ZERO, model.getTotalAvailable());
        model.setAccounts(Arrays.asList(wa1, wa2));
        assertEquals(NANO_SEM.of(3), model.getTotalAvailable());

        assertThat(model.getAccounts(), equalTo(Arrays.asList(wa1, wa2)));
    }

    @Test
    public void testTotalLocked() {
        WalletAccount wa1 = mock(WalletAccount.class);
        WalletAccount wa2 = mock(WalletAccount.class);
        when(wa1.getKey()).thenReturn(new Key());
        when(wa1.getLocked()).thenReturn(NANO_SEM.of(1));
        when(wa2.getKey()).thenReturn(new Key());
        when(wa2.getLocked()).thenReturn(NANO_SEM.of(2));

        assertEquals(ZERO, model.getTotalLocked());
        model.setAccounts(Arrays.asList(wa1, wa2));
        assertEquals(NANO_SEM.of(3), model.getTotalLocked());
    }

    @Test
    public void testGetPrimaryValidatorDelegate() {
        model = new WalletModel(config);
        List<WalletDelegate> delegates = Arrays.asList(
                new WalletDelegate(new Delegate(new Key().toAddress(), "delegate1".getBytes(), 0, Amount.ZERO)),
                new WalletDelegate(new Delegate(new Key().toAddress(), "delegate2".getBytes(), 0, Amount.ZERO)));
        model.setDelegates(delegates);
        model.setValidators(delegates.stream().map(Delegate::getAddressString).collect(Collectors.toList()));
        model.setUniformDistributionEnabled(true);

        for (int i = 0; i < 10; i++) {
            Block block = mock(Block.class);
            when(block.getNumber()).thenReturn((long) i);
            model.setLatestBlock(block);
            assertEquals(delegates.get((i + 1) % delegates.size()), model.getValidatorDelegate(0).get());
        }
    }

    @Test
    public void testGetNextPrimaryValidatorDelegate() {
        model = new WalletModel(config);
        List<WalletDelegate> delegates = Arrays.asList(
                new WalletDelegate(new Delegate(new Key().toAddress(), "delegate1".getBytes(), 0, Amount.ZERO)),
                new WalletDelegate(new Delegate(new Key().toAddress(), "delegate2".getBytes(), 0, Amount.ZERO)));
        model.setDelegates(delegates);
        model.setValidators(delegates.stream().map(Delegate::getAddressString).collect(Collectors.toList()));
        model.setUniformDistributionEnabled(true);

        Map<Long, WalletDelegate> testCases = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            testCases.put((long) i, delegates.get((i + 2) % delegates.size()));
        }

        testCases.put(198L, null);

        testCases.forEach((key, value) -> {
            Block block = mock(Block.class);
            when(block.getNumber()).thenReturn(key);
            model.setLatestBlock(block);

            if (value == null) {
                assertFalse(model.getNextPrimaryValidatorDelegate().isPresent());
            } else {
                assertEquals(value, model.getNextPrimaryValidatorDelegate().get());
            }
        });
    }

    @Test
    public void testGetNextValidatorSetUpdate() {
        model = new WalletModel(config);

        Map<Long, Long> testCases = new HashMap<>();

        for (long i = 0; i <= 198; i++) {
            testCases.put(i, 200L);
        }
        for (long i = 199; i <= 398; i++) {
            testCases.put(i, 400L);
        }
        testCases.put(399L, 600L);

        testCases.forEach((key, value) -> {
            Block block = mock(Block.class);
            when(block.getNumber()).thenReturn(key);
            model.setLatestBlock(block);
            assertEquals(value, model.getNextValidatorSetUpdate().get());
        });

    }
}
