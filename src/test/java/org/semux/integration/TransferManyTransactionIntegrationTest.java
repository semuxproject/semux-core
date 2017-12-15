/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.IntegrationTest;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.Genesis;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.net.NodeManager;
import org.semux.util.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
public class TransferManyTransactionIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TransferManyTransactionIntegrationTest.class);

    @Rule
    public TemporaryFolder kernel1Folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder kernel2Folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder kernel3Folder = new TemporaryFolder();

    private List<KernelMock> kernels;

    @Before
    public void setUp() throws IOException {
        kernels = new ArrayList<>();
        kernels.add(mockKernel(51610, 51710, kernel1Folder));
        kernels.add(mockKernel(51620, 51720, kernel2Folder));
        kernels.add(mockKernel(51630, 51730, kernel3Folder));

        // mock genesis.json
        Genesis genesis = mockGenesis(kernels.get(0).getWallet().getAccount(0).toAddressString());
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // mock seed nodes
        Set<InetSocketAddress> nodes = new HashSet<>();
        nodes.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 51610));
        mockStatic(NodeManager.class);
        when(NodeManager.getSeedNodes(Constants.DEV_NET_ID)).thenReturn(nodes);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTransferManyTransaction() throws IOException {
        KernelMock kernel1 = kernels.get(0);
        KernelMock kernel2 = kernels.get(1);
        KernelMock kernel3 = kernels.get(2);

        // start kernels
        new Thread(kernel1::start, "kernel1").start();
        await().until(() -> kernel1.getApi() != null && kernel1.getApi().isRunning());
        logger.info("Kernel-1 API Started");

        new Thread(kernel2::start, "kernel2").start();
        new Thread(kernel3::start, "kernel3").start();

        // make transfer_many request
        final long value = 1000 * Unit.SEM;
        ApiClient apiClient = new ApiClient(
                new InetSocketAddress("127.0.0.1", 51710),
                "user",
                "pass");

        HashMap<String, Object> params = new HashMap<>();
        params.put("from", kernel1.getWallet().getAccount(0).toAddressString());
        params.put("to", kernel2.getWallet().getAccount(0).toAddressString() + ","
                + kernel3.getWallet().getAccount(0).toAddressString());
        params.put("value", String.valueOf(value));
        params.put("fee", String.valueOf(kernel1.getConfig().minTransactionFee() * 2));
        String response = apiClient.request("transfer_many", params);
        Map<String, String> result = new ObjectMapper().readValue(response, new TypeReference<Map<String, String>>() {
        });
        assertEquals("true", result.get("success"));

        // wait until both of kernel2 and kernel3 have received the transaction
        await().atMost(3, TimeUnit.MINUTES).until(() -> kernel2.getBlockchain().getAccountState()
                .getAccount(kernel2.getWallet().getAccounts().get(0).toAddress()).getAvailable() == value &&
                kernel3.getBlockchain().getAccountState()
                        .getAccount(kernel2.getWallet().getAccounts().get(0).toAddress()).getAvailable() == value);
    }

    private KernelMock mockKernel(int p2pPort, int apiPort, TemporaryFolder folder) throws IOException {
        // create a new data directory
        FileUtils.copyDirectory(
                Paths.get(Constants.DEFAULT_DATA_DIR, "config").toFile(),
                Paths.get(folder.getRoot().getAbsolutePath(), "config").toFile());

        Config config = spy(new DevNetConfig(folder.getRoot().getAbsolutePath()));
        when(config.p2pListenPort()).thenReturn(p2pPort);
        when(config.p2pListenIp()).thenReturn("127.0.0.1");
        when(config.p2pDeclaredIp()).thenReturn(Optional.of("127.0.0.1"));
        when(config.apiListenIp()).thenReturn("127.0.0.1");
        when(config.apiListenPort()).thenReturn(apiPort);
        when(config.apiEnabled()).thenReturn(true);
        when(config.apiUsername()).thenReturn("user");
        when(config.apiPassword()).thenReturn("pass");
        KernelMock kernelMock = new KernelMock(config);

        Wallet wallet = mockWallet(folder);
        kernelMock.setWallet(wallet);
        kernelMock.setCoinbase(wallet.getAccount(0));

        return kernelMock;
    }

    private Genesis mockGenesis(String premineAddress) {
        // mock premine
        Genesis.Premine premine = new Genesis.Premine(
                premineAddress,
                1000000,
                "premine");
        List<Genesis.Premine> premines = new ArrayList<>();
        premines.add(premine);

        // mock delegates
        HashMap<String, String> delegates = new HashMap<>();
        delegates.put("delegate", premineAddress);

        return Genesis.jsonCreator(
                0,
                "0x0000000000000000000000000000000000000000",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                1504742400000L,
                "semux",
                premines,
                delegates,
                new HashMap<String, Object>());
    }

    private Wallet mockWallet(TemporaryFolder folder) throws IOException {
        Wallet wallet = new Wallet(folder.newFile("wallet.data"));
        wallet.unlock("password");
        wallet.addAccount(new EdDSA());
        return wallet;
    }
}
