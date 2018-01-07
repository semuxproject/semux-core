/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.semux.IntegrationTest;
import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.Genesis;
import org.semux.net.ConnectionLimitHandler;
import org.semux.net.NodeManager;
import org.semux.rules.KernelRule;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
public class ConnectionTest {

    @Rule
    KernelRule kernelRule1 = new KernelRule(51610, 51710);

    Thread serverThread;

    private final int netMaxInboundConnectionsPerIp = 5;

    @Before
    public void setUp() {
        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // configure kernel
        // netMaxInboundConnectionsPerIp = 5
        Config config = kernelRule1.getKernel().getConfig();
        Whitebox.setInternalState(config, "netMaxInboundConnectionsPerIp", netMaxInboundConnectionsPerIp);
        kernelRule1.getKernel().setConfig(config);

        // start kernel
        serverThread = new Thread(() -> kernelRule1.getKernel().start());
        serverThread.start();

        // await until the P2P server has started
        await().until(() -> kernelRule1.getKernel().state() == Kernel.State.RUNNING
                && kernelRule1.getKernel().getP2p().isRunning());
    }

    @After
    public void tearDown() {
        serverThread.interrupt();
    }

    @Test
    public void testConnectionLimit() throws IOException, InterruptedException {
        // create 100 idle connections to the P2P server
        final int connections = 100;
        Collection<Callable<Void>> threads = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Socket> sockets = new ArrayList<>(); // keep reference to created sockets
        for (int i = 1; i <= connections; i++) {
            threads.add(() -> {
                Socket socket = new Socket();
                sockets.add(socket);
                try {
                    socket.connect(
                            new InetSocketAddress(kernelRule1.getKernel().getConfig().p2pListenIp(),
                                    kernelRule1.getKernel().getConfig().p2pListenPort()),
                            100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        List<Future<Void>> futures = executorService.invokeAll(threads);
        await().until(() -> futures.stream().allMatch(Future::isDone));
        TimeUnit.MILLISECONDS.sleep(500);

        // the number of connections should be capped to netMaxInboundConnectionsPerIp
        assertEquals(netMaxInboundConnectionsPerIp, kernelRule1.getKernel().getChannelManager().size());

        // close all connections
        sockets.parallelStream().forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // ensure that all connections have been removed from the channel manager
        await().until(() -> sockets.parallelStream().allMatch(Socket::isClosed));
        await().until(() -> kernelRule1.getKernel().getChannelManager().size() == 0);
        assertFalse(ConnectionLimitHandler.containsAddress(InetAddress.getByName("127.0.0.1")));
    }

    private Genesis mockGenesis() {
        // mock premine
        List<Genesis.Premine> premines = new ArrayList<>();

        // mock delegates
        HashMap<String, String> delegates = new HashMap<>();
        // mock genesis
        return Genesis.jsonCreator(0,
                "0x0000000000000000000000000000000000000000",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                1504742400000L,
                "semux",
                premines,
                delegates,
                new HashMap<>());
    }
}
