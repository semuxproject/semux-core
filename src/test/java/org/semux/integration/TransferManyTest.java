/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.IntegrationTest;
import org.semux.KernelMock;
import org.semux.api.response.GetAccountResponse;
import org.semux.api.response.GetAccountTransactionsResponse;
import org.semux.api.response.GetTransactionResponse;
import org.semux.config.Constants;
import org.semux.core.Genesis;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.net.NodeManager;
import org.semux.util.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
public class TransferManyTest {

    private static Logger logger = LoggerFactory.getLogger(TransferManyTest.class);

    private static final long PREMINE = 1000000;

    @Rule
    KernelTestRule kernelValidatorRule = new KernelTestRule(51610, 51710);

    @Rule
    KernelTestRule kernelPremineRule = new KernelTestRule(51620, 51720);

    @Rule
    KernelTestRule kernelReceiver1Rule = new KernelTestRule(51630, 51730);

    @Rule
    KernelTestRule kernelReceiver2Rule = new KernelTestRule(51640, 51740);

    /**
     * The kernel that is solely responsible of forging blocks
     */
    public KernelMock kernelValidator;

    /**
     * The kernel that has 1000000 SEM available from height 0
     */
    public KernelMock kernelPremine;

    /**
     * The kernels who will receive transaction from kernelPremine
     */
    public KernelMock kernelReceiver1, kernelReceiver2;

    public TransferManyTest() throws IOException {
    }

    @Before
    public void setUp() throws Exception {
        // prepare kernels
        kernelValidator = kernelValidatorRule.getKernelMock();
        kernelPremine = kernelPremineRule.getKernelMock();
        kernelReceiver1 = kernelReceiver1Rule.getKernelMock();
        kernelReceiver2 = kernelReceiver2Rule.getKernelMock();

        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // mock seed nodes
        Set<InetSocketAddress> nodes = new HashSet<>();
        nodes.add(new InetSocketAddress(
                InetAddress.getByName(kernelValidator.getConfig().p2pListenIp()),
                kernelValidator.getConfig().p2pListenPort()));
        mockStatic(NodeManager.class);
        when(NodeManager.getSeedNodes(Constants.DEV_NET_ID)).thenReturn(nodes);

        // start kernels
        kernelValidator.start();
        kernelPremine.start();
        kernelReceiver1.start();
        kernelReceiver2.start();

        // the kernel will start component threads asynchronously
        // have to wait a bit
        Thread.sleep(1000);
    }

    @After
    public void teardown() {
        // stop kernels
        kernelValidator.stop();
        kernelPremine.stop();
        kernelReceiver1.stop();
        kernelReceiver2.stop();
    }

    /**
     * Expectations:
     * <ul>
     * <li>kernelReceiver1 and kernelReceiver2 should receive <code>1000 SEM</code>
     * from kernelPremine</li>
     * <li>kernelPremine's account should be deducted with
     * <code>1000 SEM * 2 + 0.05 SEM * 2</code></li>
     * </ul>
     */
    @Test
    public void testTransferManyTransaction() throws IOException {
        // make transfer_many request from kernelPremine to kernelReceiver1 and
        // kernelReceiver2
        final long value = 1000 * Unit.SEM;
        final long fee = kernelPremine.getConfig().minTransactionFee() * 2;
        HashMap<String, Object> params = new HashMap<>();
        params.put("from", addressStringOf(kernelPremine));
        params.put("to", addressStringOf(kernelReceiver1) + "," + addressStringOf(kernelReceiver2));
        params.put("value", String.valueOf(value));
        params.put("fee", String.valueOf(fee));
        logger.info("Making transfer_many request", params);
        String response = kernelPremine.getApiClient().request("transfer_many", params);
        Map<String, String> result = new ObjectMapper().readValue(response, new TypeReference<Map<String, String>>() {
        });
        assertEquals("true", result.get("success"));

        // wait until both of kernelReceiver1 and kernelReceiver2 have received the
        // transaction.
        // (2x transaction value + 2x min transaction fee) should be deducted from
        // kernelPremine's account
        logger.info("Waiting for the transaction to be processed...");
        await().until(availableOf(kernelPremine), equalTo(PREMINE * Unit.SEM - value * 2 - fee));
        await().until(availableOf(kernelReceiver1), equalTo(value));
        await().until(availableOf(kernelReceiver2), equalTo(value));

        // assert that the transaction has been recorded across nodes
        assertTransferManyTransaction(kernelPremine);
        assertTransferManyTransaction(kernelReceiver1);
        assertTransferManyTransaction(kernelReceiver2);
    }

    private void assertTransferManyTransaction(KernelMock kernelMock) throws IOException {
        GetTransactionResponse.Result transactionResultPremine = getTransactionResultOf(kernelMock, 0);
        assertEquals(addressStringOf(kernelPremine), transactionResultPremine.from);
        assertNull(transactionResultPremine.to);
        assertThat(transactionResultPremine.toMany,
                contains(addressStringOf(kernelReceiver1), addressStringOf(kernelReceiver2)));
    }

    private Callable<Long> availableOf(KernelMock kernelMock) {
        return () -> {
            ApiClient apiClient = kernelMock.getApiClient();
            GetAccountResponse response = new ObjectMapper().readValue(
                    apiClient.request("get_account", "address", addressStringOf(kernelMock)),
                    GetAccountResponse.class);
            logger.info("Available of {} = {}", addressStringOf(kernelMock), response.account.available);
            return response.account.available;
        };
    }

    private GetTransactionResponse.Result getTransactionResultOf(KernelMock kernelMock, int n) throws IOException {
        ApiClient apiClient = kernelMock.getApiClient();
        GetAccountTransactionsResponse response = new ObjectMapper().readValue(
                apiClient.request(
                        "get_account_transactions",
                        "address", addressStringOf(kernelMock),
                        "from", String.valueOf(n),
                        "to", String.valueOf(n + 1)),
                GetAccountTransactionsResponse.class);
        return response.transactions.get(0);
    }

    private String addressStringOf(KernelMock kernelMock) {
        return Hex.encode0x(kernelMock.getWallet().getAccount(0).toAddress());
    }

    private Genesis mockGenesis() {
        String premineAddress = addressStringOf(kernelPremine);
        String delegateAddress = addressStringOf(kernelValidator);

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
}
