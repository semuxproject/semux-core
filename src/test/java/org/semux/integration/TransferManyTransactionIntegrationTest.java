/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
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
import org.semux.core.Transaction;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.core.state.Account;
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

    private static final long PREMINE = 1000000;

    @Rule
    public TemporaryFolder kernel1Folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder kernel2Folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder kernel3Folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder kernel4Folder = new TemporaryFolder();

    public KernelMock kernelValidator;

    public KernelMock kernelPremine;

    public KernelMock kernelReceiver1;

    public KernelMock kernelReceiver2;

    @Before
    public void setUp() throws IOException {
        kernelValidator = mockKernel(51610, 51710, kernel1Folder);
        kernelPremine = mockKernel(51620, 51720, kernel2Folder);
        kernelReceiver1 = mockKernel(51630, 51730, kernel3Folder);
        kernelReceiver2 = mockKernel(51640, 51740, kernel4Folder);

        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // mock seed nodes
        Set<InetSocketAddress> nodes = new HashSet<>();
        nodes.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 51610));
        nodes.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 51620));
        nodes.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 51630));
        nodes.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 51640));
        mockStatic(NodeManager.class);
        when(NodeManager.getSeedNodes(Constants.DEV_NET_ID)).thenReturn(nodes);
    }

    /**
     * Expectations:
     * 1. kernelReceiver1 and kernelReceiver2 should receive 1000 SEM from
     * kernelPremine 2. kernelPremine's account should be deducted with 1000 SEM * 2
     * + 0.05 SEM * 2
     */
    @Test
    public void testTransferManyTransaction() throws IOException {
        // start kernels
        new Thread(kernelValidator::start, "kernelValidator").start();
        new Thread(kernelPremine::start, "kernelPremine").start();
        new Thread(kernelReceiver1::start, "kernelReceiver1").start();
        new Thread(kernelReceiver2::start, "kernelReceiver2").start();
        await().until(() -> kernelValidator.getApi() != null && kernelValidator.getApi().isRunning());
        await().until(() -> kernelPremine.getApi() != null && kernelPremine.getApi().isRunning());
        await().until(() -> kernelReceiver1.getApi() != null && kernelReceiver1.getApi().isRunning());
        await().until(() -> kernelReceiver2.getApi() != null && kernelReceiver2.getApi().isRunning());

        // make transfer_many request from kernelPremine to kernelReceiver1 and
        // kernelReceiver2
        final long value = 1000 * Unit.SEM;
        final long fee = kernelPremine.getConfig().minTransactionFee() * 2;
        ApiClient apiClient = new ApiClient(
                new InetSocketAddress("127.0.0.1", 51720),
                "user",
                "pass");
        HashMap<String, Object> params = new HashMap<>();
        params.put("from", kernelPremine.getWallet().getAccount(0).toAddressString());
        params.put("to", kernelReceiver1.getWallet().getAccount(0).toAddressString() + ","
                + kernelReceiver2.getWallet().getAccount(0).toAddressString());
        params.put("value", String.valueOf(value));
        params.put("fee", String.valueOf(fee));
        String response = apiClient.request("transfer_many", params);
        Map<String, String> result = new ObjectMapper().readValue(response, new TypeReference<Map<String, String>>() {
        });
        assertEquals("true", result.get("success"));

        // wait until both of kernelReceiver1 and kernelReceiver2 have received the
        // transaction
        await().atMost(3, TimeUnit.MINUTES).until(() -> kernelReceiver1.getBlockchain().getAccountState()
                .getAccount(kernelReceiver1.getWallet().getAccounts().get(0).toAddress()).getAvailable() == value &&
                kernelReceiver2.getBlockchain().getAccountState()
                        .getAccount(kernelReceiver2.getWallet().getAccounts().get(0).toAddress())
                        .getAvailable() == value);

        // (2x transaction value + 2x min transaction fee) should be deducted from
        // kernelPremine's account
        Account accountPremine = kernelPremine.getBlockchain().getAccountState()
                .getAccount(kernelPremine.getWallet().getAccount(0).toAddress());
        Account accountReceiver1 = kernelReceiver1.getBlockchain().getAccountState()
                .getAccount(kernelReceiver1.getWallet().getAccount(0).toAddress());
        Account accountReceiver2 = kernelReceiver2.getBlockchain().getAccountState()
                .getAccount(kernelReceiver2.getWallet().getAccount(0).toAddress());
        assertEquals(0, kernelValidator.getPendingManager().getTransactions().size());
        List<Transaction> transactions = kernelPremine.getBlockchain().getTransactions(
                kernelPremine.getWallet().getAccount(0).toAddress(),
                0,
                1);
        assertArrayEquals(accountPremine.getAddress(), transactions.get(0).getFrom());
        assertArrayEquals(accountReceiver1.getAddress(), transactions.get(0).getRecipient(0));
        assertArrayEquals(accountReceiver2.getAddress(), transactions.get(0).getRecipient(1));
        assertEquals(value, transactions.get(0).getValue());
        assertEquals(PREMINE * Unit.SEM - value * 2 - fee, accountPremine.getAvailable());
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

    private Genesis mockGenesis() {
        String premineAddress = kernelPremine.getWallet().getAccount(0).toAddressString();
        String delegateAddress = kernelValidator.getWallet().getAccount(0).toAddressString();

        // mock premine
        Genesis.Premine premine = new Genesis.Premine(premineAddress, PREMINE, "premine");
        List<Genesis.Premine> premines = new ArrayList<>();
        premines.add(premine);

        // mock delegates
        HashMap<String, String> delegates = new HashMap<>();
        delegates.put("delegate", delegateAddress);

        return Genesis.jsonCreator(
                0,
                "0x0000000000000000000000000000000000000000",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                1504742400000L,
                "semux",
                premines,
                delegates,
                new HashMap<>());
    }

    private Wallet mockWallet(TemporaryFolder folder) throws IOException {
        Wallet wallet = new Wallet(folder.newFile("wallet.data"));
        wallet.unlock("password");
        wallet.addAccount(new EdDSA());
        return wallet;
    }
}
