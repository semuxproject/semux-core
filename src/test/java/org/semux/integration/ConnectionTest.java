/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.semux.IntegrationTest;
import org.semux.Kernel;
import org.semux.TestUtils;
import org.semux.config.AbstractConfig;
import org.semux.config.Config;
import org.semux.core.Genesis;
import org.semux.rules.KernelRule;

@Category(IntegrationTest.class)
public class ConnectionTest {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    Thread serverThread;

    List<Socket> sockets;

    private final int netMaxInboundConnectionsPerIp = 5;

    @Before
    public void setUp() {
        // mock genesis.json
        Genesis genesis = mockGenesis();
        kernelRule1.setGenesis(genesis);

        // configure kernel
        // netMaxInboundConnectionsPerIp = 5
        Config config = kernelRule1.getKernel().getConfig();
        TestUtils.setInternalState(config, "netMaxInboundConnectionsPerIp", netMaxInboundConnectionsPerIp,
                AbstractConfig.class);
        kernelRule1.getKernel().setConfig(config);

        // start kernel
        serverThread = new Thread(() -> kernelRule1.getKernel().start());
        serverThread.start();

        // await until the P2P server has started
        await().until(() -> kernelRule1.getKernel().state() == Kernel.State.RUNNING
                && kernelRule1.getKernel().getP2p().isRunning());

        // keep socket references
        sockets = new CopyOnWriteArrayList<>();
    }

    @After
    public void tearDown() {
        // close all connections
        sockets.parallelStream().filter(Objects::nonNull).forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        kernelRule1.getKernel().stop();
        await().until(() -> kernelRule1.getKernel().state().equals(Kernel.State.STOPPED));
        serverThread.interrupt();
        await().until(() -> !serverThread.isAlive());
    }

    @Test
    public void testConnectionLimit() throws InterruptedException, UnknownHostException {
        // create 100 idle connections to the P2P server
        final int connections = 100;
        Collection<Callable<Void>> threads = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i = 1; i <= connections; i++) {
            threads.add(() -> {
                try {
                    Socket socket = new Socket();
                    socket.connect(
                            new InetSocketAddress(kernelRule1.getKernel().getConfig().p2pListenIp(),
                                    kernelRule1.getKernel().getConfig().p2pListenPort()),
                            100);
                    sockets.add(socket);
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
    }

    @Test
    public void testBlacklistIp() throws IOException, InterruptedException {
        // create an idle connection
        final int connections = 1;
        Collection<Callable<Void>> threads = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(connections);
        final List<InetSocketAddress> clientAddresses = new CopyOnWriteArrayList<>();
        for (int i = 1; i <= connections; i++) {
            threads.add(() -> {
                try {
                    Socket socket = new Socket();
                    socket.bind(new InetSocketAddress("127.0.0.1", getFreePort()));
                    sockets.add(socket);
                    clientAddresses.add((InetSocketAddress) socket.getLocalSocketAddress());
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

        // wait until all channels are connected
        assertEquals(connections, kernelRule1.getKernel().getChannelManager().size());

        // blacklist 127.0.0.1
        final String blacklistedIp = "127.0.0.1";
        kernelRule1.getKernel().getApiClient().put("/blacklist", "ip", blacklistedIp);

        // all IPs should stay connected except for the blacklisted IP
        await().until(() -> kernelRule1.getKernel().getChannelManager().size() == connections - 1);
        for (InetSocketAddress clientAddress : clientAddresses) {
            if (clientAddress.getHostString().equals(blacklistedIp)) {
                assertFalse(kernelRule1.getKernel().getChannelManager().isConnected(clientAddress));
            } else {
                assertTrue(kernelRule1.getKernel().getChannelManager().isConnected(clientAddress));
            }
        }
    }

    private int getFreePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
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
