/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
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
import org.semux.rules.KernelRule;
import org.semux.util.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
public class TransferTest {

    private static Logger logger = LoggerFactory.getLogger(TransferTest.class);

    private static final long PREMINE = 1000000;

    @Rule
    KernelRule kernelValidatorRule = new KernelRule(51610, 51710);

    @Rule
    KernelRule kernelSenderRule = new KernelRule(51620, 51720);

    @Rule
    KernelRule kernelReceiverRule = new KernelRule(51630, 51730);

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
    public KernelMock kernelReceiver;

    @Before
    public void setUp() throws Exception {
        // prepare kernels
        kernelValidatorRule.speedUpCosnensus();
        kernelSenderRule.speedUpCosnensus();
        kernelReceiverRule.speedUpCosnensus();
        kernelValidator = kernelValidatorRule.getKernel();
        kernelPremine = kernelSenderRule.getKernel();
        kernelReceiver = kernelReceiverRule.getKernel();

        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // mock seed nodes
        Set<InetSocketAddress> nodes = new HashSet<>();
        nodes.add(new InetSocketAddress(InetAddress.getByName(kernelValidator.getConfig().p2pListenIp()),
                kernelValidator.getConfig().p2pListenPort()));
        mockStatic(NodeManager.class);
        when(NodeManager.getSeedNodes(Constants.DEV_NET_ID)).thenReturn(nodes);

        // start kernels
        kernelValidator.start();
        kernelPremine.start();
        kernelReceiver.start();
    }

    @After
    public void tearDown() {
        // stop kernels
        kernelValidator.stop();
        kernelPremine.stop();
        kernelReceiver.stop();
    }

    /**
     * Expectations:
     * <ul>
     * <li>kernelReceiver should receive <code>1000 SEM</code> from
     * kernelPremine</li>
     * <li>kernelPremine's account should be deducted with
     * <code>1000 SEM + 0.05 SEM </code></li>
     * </ul>
     */
    @Test
    public void testTransfer() throws IOException {
        // wait
        await().until(() -> kernelValidator.isRunning()
                && kernelPremine.isRunning()
                && kernelReceiver.isRunning());

        // make transaction
        final long value = 1000 * Unit.SEM;
        final long fee = kernelPremine.getConfig().minTransactionFee() * 2;
        HashMap<String, Object> params = new HashMap<>();
        params.put("from", addressStringOf(kernelPremine));
        params.put("to", addressStringOf(kernelReceiver));
        params.put("value", String.valueOf(value));
        params.put("fee", String.valueOf(fee));

        // request
        logger.info("Making transfer request", params);
        String response = kernelPremine.getApiClient().request("transfer", params);
        Map<String, String> result = new ObjectMapper().readValue(response, new TypeReference<Map<String, String>>() {
        });
        assertEquals("true", result.get("success"));

        // wait for transaction processing
        logger.info("Waiting for the transaction to be processed...");
        await().pollInterval(1, SECONDS).atMost(60, SECONDS).until(availableOf(kernelPremine),
                equalTo(PREMINE * Unit.SEM - value - fee));
        await().pollInterval(1, SECONDS).atMost(60, SECONDS).until(availableOf(kernelReceiver), equalTo(value));

        // assert that the transaction has been recorded across nodes
        assertTransferTransaction(kernelPremine);
        assertTransferTransaction(kernelReceiver);
    }

    private void assertTransferTransaction(KernelMock kernelMock) throws IOException {
        GetTransactionResponse.Result result = getTransactionResultOf(kernelMock, 0);
        assertEquals(addressStringOf(kernelPremine), result.from);
        assertEquals(result.to, addressStringOf(kernelReceiver));
    }

    private Callable<Long> availableOf(KernelMock kernelMock) {
        return () -> {
            ApiClient apiClient = kernelMock.getApiClient();
            GetAccountResponse response = new ObjectMapper().readValue(
                    apiClient.request("get_account", "address", addressStringOf(kernelMock)), GetAccountResponse.class);
            return response.account.available;
        };
    }

    private GetTransactionResponse.Result getTransactionResultOf(KernelMock kernelMock, int n) throws IOException {
        ApiClient apiClient = kernelMock.getApiClient();
        GetAccountTransactionsResponse response = new ObjectMapper()
                .readValue(apiClient.request("get_account_transactions", "address", addressStringOf(kernelMock), "from",
                        String.valueOf(n), "to", String.valueOf(n + 1)), GetAccountTransactionsResponse.class);
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

        return Genesis.jsonCreator(0, //
                "0x0000000000000000000000000000000000000000", //
                "0x0000000000000000000000000000000000000000000000000000000000000000", //
                1504742400000L, //
                "semux", //
                premines, //
                delegates, //
                new HashMap<>());
    }
}
