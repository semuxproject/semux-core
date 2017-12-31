/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.semux.Kernel.State;
import org.semux.KernelMock;
import org.semux.api.response.DoTransactionResponse;
import org.semux.api.response.GetAccountResponse;
import org.semux.api.response.GetAccountTransactionsResponse;
import org.semux.api.response.GetTransactionResponse;
import org.semux.config.Constants;
import org.semux.core.Genesis;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;
import org.semux.net.NodeManager;
import org.semux.rules.KernelRule;
import org.semux.util.ApiClient;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
public class TransactionTest {

    private static Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    private static final long PREMINE = 5000L * Unit.SEM;

    @Rule
    KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    KernelRule kernelRule2 = new KernelRule(51620, 51720);

    @Rule
    KernelRule kernelRule3 = new KernelRule(51630, 51730);

    public KernelMock kernel1; // validator node
    public KernelMock kernel2; // normal node with balance
    public KernelMock kernel3; // normal node with no balance

    @Before
    public void setUp() throws Exception {
        // prepare kernels
        kernelRule1.speedUpCosnensus();
        kernelRule2.speedUpCosnensus();
        kernelRule3.speedUpCosnensus();
        kernel1 = kernelRule1.getKernel();
        kernel2 = kernelRule2.getKernel();
        kernel3 = kernelRule3.getKernel();

        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);

        // mock seed nodes
        Set<InetSocketAddress> nodes = new HashSet<>();
        nodes.add(new InetSocketAddress(InetAddress.getByName(kernel1.getConfig().p2pListenIp()),
                kernel1.getConfig().p2pListenPort()));
        mockStatic(NodeManager.class);
        when(NodeManager.getSeedNodes(Constants.DEV_NET_ID)).thenReturn(nodes);

        // start kernels
        kernel1.start();
        kernel2.start();
        kernel3.start();

        // wait for kernels
        await().atMost(20, SECONDS).until(() -> kernel1.state() == State.RUNNING
                && kernel2.state() == State.RUNNING
                && kernel3.state() == State.RUNNING
                && kernel3.getChannelManager().getActivePeers().size() >= 2);
    }

    @After
    public void tearDown() {
        // stop kernels
        kernel1.stop();
        kernel2.stop();
        kernel3.stop();
    }

    @Test
    public void testTransfer() throws IOException {
        final long value = 1000 * Unit.SEM;
        final long fee = kernel2.getConfig().minTransactionFee();

        // prepare transaction
        HashMap<String, Object> params = new HashMap<>();
        params.put("from", coinbaseOf(kernel2));
        params.put("to", coinbaseOf(kernel3));
        params.put("value", String.valueOf(value));
        params.put("fee", String.valueOf(fee));

        // send transaction
        logger.info("Making transfer request", params);
        DoTransactionResponse response = new ObjectMapper().readValue(
                kernel2.getApiClient().request("transfer", params),
                DoTransactionResponse.class);
        assertTrue(response.success);

        // wait for transaction to be processed
        logger.info("Waiting for the transaction to be processed...");
        await().atMost(20, SECONDS).until(availableOf(kernel2, coinbaseOf(kernel2)),
                equalTo(PREMINE * Unit.SEM - value - fee));
        await().atMost(20, SECONDS).until(availableOf(kernel3, coinbaseOf(kernel3)),
                equalTo(value));

        // assert that the transaction has been recorded across nodes
        assertTransaction(kernel2, coinbaseOf(kernel2),
                TransactionType.TRANSFER, coinbaseOf(kernel2), coinbaseOf(kernel3), value, fee, Bytes.EMPTY_BYTES);
        assertTransaction(kernel3, coinbaseOf(kernel3),
                TransactionType.TRANSFER, coinbaseOf(kernel2), coinbaseOf(kernel3), value, fee, Bytes.EMPTY_BYTES);

        // assert the state
        List<Delegate> delegates = kernel2.getBlockchain().getDelegateState().getDelegates();
        assertThat(delegates).anySatisfy((d) -> Arrays.equals(d.getAddress(), coinbaseOf(kernel2)));
    }

    @Test
    public void testDelegate() throws IOException {
        final long fee = kernel2.getConfig().minTransactionFee();

        // prepare transaction
        HashMap<String, Object> params = new HashMap<>();
        params.put("from", coinbaseOf(kernel2));
        params.put("fee", fee);
        params.put("data", Bytes.of("test"));

        // send transaction
        logger.info("Making delegate request", params);
        DoTransactionResponse response = new ObjectMapper().readValue(
                kernel2.getApiClient().request("delegate", params),
                DoTransactionResponse.class);
        assertTrue(response.success);

        // wait for transaction processing
        logger.info("Waiting for the transaction to be processed...");
        await().atMost(20, SECONDS).until(availableOf(kernel2, coinbaseOf(kernel2)),
                equalTo(PREMINE * Unit.SEM - kernel2.getConfig().minDelegateBurnAmount() - fee));

        // // assert that the transaction has been recorded across nodes
        assertTransaction(kernel2, coinbaseOf(kernel2),
                TransactionType.DELEGATE, coinbaseOf(kernel2), Bytes.EMPTY_ADDRESS,
                kernel2.getConfig().minDelegateBurnAmount(), fee, Bytes.of("test"));
    }

    /**
     * Assert the latest transaction of the given address, by querying the specified
     * kernel.
     * 
     * @param kernel
     * @param address
     * @param type
     * @param from
     * @param to
     * @param value
     * @param fee
     * @param data
     * @throws IOException
     */
    private void assertTransaction(KernelMock kernel, byte[] address,
            TransactionType type, byte[] from, byte[] to, long value, long fee, byte[] data)
            throws IOException {
        GetTransactionResponse.Result result = latestTransactionOf(kernel, address);
        assertEquals(type.name(), result.type);
        assertEquals(Hex.encode0x(from), result.from);
        assertEquals(Hex.encode0x(to), result.to);
        assertEquals((Long) value, result.value);
        assertEquals((Long) fee, result.fee);
        assertEquals(Hex.encode0x(data), result.data);
    }

    /**
     * Returns the callable which can be used to get the balance of given address.
     * 
     * @param kernelMock
     * @param address
     * @return
     */
    private Callable<Long> availableOf(KernelMock kernelMock, byte[] address) {
        return () -> {
            ApiClient apiClient = kernelMock.getApiClient();

            GetAccountResponse response = new ObjectMapper().readValue(
                    apiClient.request("get_account",
                            "address", address),
                    GetAccountResponse.class);

            return response.account.available;
        };
    }

    /**
     * Returns the nth transaction of the given address, by querify the specified
     * kernel.
     * 
     * @param kernel
     * @param address
     * @return
     * @throws IOException
     */
    private GetTransactionResponse.Result latestTransactionOf(KernelMock kernel, byte[] address)
            throws IOException {
        ApiClient apiClient = kernel.getApiClient();

        GetAccountTransactionsResponse response = new ObjectMapper().readValue(
                apiClient.request("get_account_transactions",
                        "address", address,
                        "from", 0,
                        "to", 1000),
                GetAccountTransactionsResponse.class);

        return response.transactions.get(response.transactions.size() - 1);
    }

    /**
     * Returns the coinbase address of the given kernel.
     * 
     * @param kernelMock
     * @return
     */
    private byte[] coinbaseOf(KernelMock kernelMock) {
        return kernelMock.getCoinbase().toAddress();
    }

    /**
     * Mocks a genesis instance where kernel1 is the only validator and kernel2 has
     * some premined balance.
     * 
     * @return
     */
    private Genesis mockGenesis() {
        // mock premine
        List<Genesis.Premine> premines = new ArrayList<>();
        premines.add(new Genesis.Premine(kernel2.getCoinbase().toAddress(), PREMINE, ""));

        // mock delegates
        HashMap<String, String> delegates = new HashMap<>();
        delegates.put("delegate1", kernel1.getCoinbase().toAddressString());

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
