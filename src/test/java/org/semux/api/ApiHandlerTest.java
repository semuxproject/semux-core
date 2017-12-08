/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Genesis;
import org.semux.core.Genesis.Premine;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.Peer;
import org.semux.net.filter.CIDRFilterRule;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

import io.netty.handler.ipfilter.IpFilterRuleType;

public class ApiHandlerTest {

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    private static final String API_IP = "127.0.0.1";
    private static final int API_PORT = 15171;

    private static SemuxAPIMock api;

    private static Config config;
    private static Wallet wallet;

    private static Blockchain chain;
    private static AccountState accountState;
    private static DelegateState delegateState;
    private static PendingManager pendingMgr;
    private static NodeManager nodeMgr;
    private static ChannelManager channelMgr;

    @Before
    public void setUp() {
        api = new SemuxAPIMock(temporaryDBFactory);
        api.start(API_IP, API_PORT);

        config = api.getKernel().getConfig();
        wallet = api.getKernel().getWallet();

        chain = api.getKernel().getBlockchain();
        accountState = api.getKernel().getBlockchain().getAccountState();
        accountState.adjustAvailable(wallet.getAccount(0).toAddress(), 5000 * Unit.SEM);
        delegateState = api.getKernel().getBlockchain().getDelegateState();
        pendingMgr = api.getKernel().getPendingManager();
        nodeMgr = api.getKernel().getNodeManager();
        channelMgr = api.getKernel().getChannelManager();
    }

    @After
    public void tearDown() throws IOException {
        api.stop();
        if (wallet.exists()) {
            wallet.delete();
        }
    }

    private static JsonObject request(String uri) throws IOException {
        URL u = new URL("http://" + API_IP + ":" + API_PORT + uri);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();

        Optional<String> username = config.apiUsername();
        Optional<String> password = config.apiPassword();
        if (username.isPresent() && password.isPresent()) {
            con.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(Bytes.of(config.apiUsername().get() + ":" + config.apiPassword().get())));
        }

        try (JsonReader jsonReader = Json.createReader(con.getInputStream())) {
            return jsonReader.readObject();
        }
    }

    @Test
    public void testRoot() throws IOException {
        String uri = "/";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
    }

    @Test
    public void testGetInfo() throws IOException {
        String uri = "/get_info";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        JsonObject result = response.getJsonObject("result");

        assertNotNull(result);
        assertEquals(0, result.getInt("latestBlockNumber"));
    }

    @Test
    public void testGetPeers() throws IOException {
        channelMgr = spy(api.getKernel().getChannelManager());
        List<Peer> peers = Arrays.asList(new Peer("1.2.3.4", 5161, (short) 1, "client1", "peer1", 1),
                new Peer("2.3.4.5", 5171, (short) 2, "client2", "peer2", 2));
        when(channelMgr.getActivePeers()).thenReturn(peers);
        api.getKernel().setChannelManager(channelMgr);

        JsonObject response = request("/get_peers");
        assertTrue(response.getBoolean("success"));
        JsonArray result = response.getJsonArray("result");

        assertNotNull(result);
        assertEquals(peers.size(), result.size());
        for (int i = 0; i < peers.size(); i++) {
            JsonObject peerJson = result.getJsonObject(i);
            Peer peer = peers.get(i);
            assertEquals(peer.getIp(), peerJson.getString("ip"));
            assertEquals(peer.getPort(), peerJson.getInt("port"));
            assertEquals(peer.getNetworkVersion(), peerJson.getInt("networkVersion"));
            assertEquals(peer.getClientId(), peerJson.getString("clientId"));
            assertEquals(Hex.PREF + peer.getPeerId(), peerJson.getString("peerId"));
            assertEquals(peer.getLatestBlockNumber(), peerJson.getInt("latestBlockNumber"));
            assertEquals(peer.getLatency(), peerJson.getInt("latency"));
        }
    }

    @Test
    public void testAddNode() throws IOException {
        String uri = "/add_node?node=127.0.0.1:5162";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        assertEquals(1, nodeMgr.queueSize());
    }

    @Test
    public void testAddToBlacklist() throws IOException {
        // blacklist 8.8.8.8
        assertTrue(request("/add_to_blacklist?ip=8.8.8.8").getBoolean("success"));

        // assert that 8.8.8.8 is no longer acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertFalse(channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void testAddToBlacklistInvalidAddress() throws IOException {
        JsonObject response = request("/add_to_blacklist?ip=I_am_not_an_IP");
        assertFalse(response.getBoolean("success"));
        assertTrue(response.containsKey("message"));
    }

    @Test
    public void testAddToWhitelist() throws IOException {
        // reject all connections
        channelMgr.getIpFilter().appendRule(new CIDRFilterRule("0.0.0.0/0", IpFilterRuleType.REJECT));

        // whitelist 8.8.8.8
        assertTrue(request("/add_to_whitelist?ip=8.8.8.8").getBoolean("success"));

        // assert that 8.8.8.8 is acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void testAddToWhitelistInvalidAddress() throws IOException {
        JsonObject response = request("/add_to_whitelist?ip=I_am_not_an_IP");
        assertFalse(response.getBoolean("success"));
        assertTrue(response.containsKey("message"));
    }

    @Test
    public void tesAddToBlacklistThenWhitelist() throws IOException {
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));

        assertTrue(request("/add_to_blacklist?ip=8.8.8.8").getBoolean("success"));
        assertTrue(!channelMgr.isAcceptable(inetSocketAddress));

        assertTrue(request("/add_to_whitelist?ip=8.8.8.8").getBoolean("success"));
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));

        assertTrue(request("/add_to_blacklist?ip=8.8.8.8").getBoolean("success"));
        assertTrue(!channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void testGetLatestBlockNumber() throws IOException {
        String uri = "/get_latest_block_number";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
    }

    @Test
    public void testGetLatestBlock() throws IOException {
        Genesis gen = chain.getGenesis();

        String uri = "/get_latest_block";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        JsonObject block = response.getJsonObject("result");

        assertArrayEquals(gen.getHash(), Hex.parse(block.getString("hash")));
        assertEquals(gen.getNumber(), block.getJsonNumber("number").longValueExact());
        assertArrayEquals(gen.getCoinbase(), Hex.parse(block.getString("coinbase")));
        assertArrayEquals(gen.getPrevHash(), Hex.parse(block.getString("prevHash")));
        assertEquals(gen.getTimestamp(), block.getJsonNumber("timestamp").longValueExact());
        assertArrayEquals(gen.getTransactionsRoot(), Hex.parse(block.getString("transactionsRoot")));
        assertArrayEquals(gen.getData(), Hex.parse(block.getString("data")));
    }

    @Test
    public void testGetBlock() throws IOException {
        Genesis gen = chain.getGenesis();

        String uri = "/get_block?number=" + gen.getNumber();
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertArrayEquals(gen.getHash(), Hex.parse(response.getJsonObject("result").getString("hash")));

        uri = "/get_block?hash=" + Hex.encode(gen.getHash());
        response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertArrayEquals(gen.getHash(), Hex.parse(response.getJsonObject("result").getString("hash")));
    }

    @Test
    public void testGetPendingTransactions() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_pending_transactions";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        JsonArray arr = response.getJsonArray("result");
        assertNotNull(arr);
    }

    @Test
    public void testGetAccountTransactions() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_account_transactions?address=" + Hex.encode(tx.getFrom()) + "&from=0&to=1024";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        JsonArray arr = response.getJsonArray("result");
        assertNotNull(arr);
    }

    @Test
    public void testGetTransaction() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_transaction?hash=" + Hex.encode(tx.getHash());
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        JsonObject obj = response.getJsonObject("result");
        assertArrayEquals(tx.getHash(), Hex.parse(obj.getString("hash")));
    }

    @Test
    public void testSendTransaction() throws IOException, InterruptedException {
        Transaction tx = createTransaction();

        String uri = "/send_transaction?raw=" + Hex.encode(tx.toBytes());
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        Thread.sleep(200);
        List<Transaction> list = pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), tx.getHash());
    }

    @Test
    public void testGetAccount() throws IOException {
        Genesis gen = chain.getGenesis();
        Entry<ByteArray, Premine> entry = gen.getPremines().entrySet().iterator().next();

        String uri = "/get_account?address=" + entry.getKey();
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(entry.getValue().getAmount(),
                response.getJsonObject("result").getJsonNumber("available").longValueExact());
    }

    @Test
    public void testGetDelegate() throws IOException {
        Genesis gen = chain.getGenesis();
        Entry<String, byte[]> entry = gen.getDelegates().entrySet().iterator().next();

        String uri = "/get_delegate?address=" + Hex.encode(entry.getValue());
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(entry.getKey(), response.getJsonObject("result").getString("name"));
    }

    @Test
    public void testGetDelegates() throws IOException {
        String uri = "/get_delegates";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertTrue(response.getJsonArray("result").size() > 0);
    }

    @Test
    public void testGetValidators() throws IOException {
        String uri = "/get_validators";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertTrue(response.getJsonArray("result").size() > 0);
    }

    @Test
    public void testGetVote() throws IOException {
        EdDSA key = new EdDSA();
        EdDSA key2 = new EdDSA();
        DelegateState ds = chain.getDelegateState();
        ds.register(key2.toAddress(), Bytes.of("test"));
        ds.vote(key.toAddress(), key2.toAddress(), 200L);

        String uri = "/get_vote?voter=" + key.toAddressString() + "&delegate=" + key2.toAddressString();
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(200L, response.getJsonNumber("result").longValueExact());
    }

    @Test
    public void testGetVotes() throws IOException {
        EdDSA voterKey = new EdDSA();
        EdDSA delegateKey = new EdDSA();
        DelegateState ds = chain.getDelegateState();
        assertTrue(ds.register(delegateKey.toAddress(), Bytes.of("test")));
        assertTrue(ds.vote(voterKey.toAddress(), delegateKey.toAddress(), 200L));
        ds.commit();

        JsonObject response = request("/get_votes?delegate=" + delegateKey.toAddressString());
        assertTrue(response.getBoolean("success"));
        assertEquals(200L,
                response.getJsonObject("result").getJsonNumber(Hex.PREF + voterKey.toAddressString()).longValueExact());
    }

    @Test
    public void testGetAccounts() throws IOException {
        String uri = "/list_accounts";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getJsonArray("result"));
    }

    @Test
    public void testCreateAccount() throws IOException {
        int size = wallet.getAccounts().size();

        String uri = "/create_account";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(size + 1, wallet.getAccounts().size());
    }

    @Test
    public void testTransfer() throws IOException, InterruptedException {
        EdDSA key = new EdDSA();
        String uri = "/transfer?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + key.toAddressString()
                + "&value=1000000000&fee=" + config.minTransactionFee() + "&data=test";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.TRANSFER);
    }

    @Test
    public void testDelegate() throws IOException, InterruptedException {
        String uri = "/delegate?&from=" + wallet.getAccount(0).toAddressString() + "&fee=" + config.minTransactionFee()
                + "&data=" + Hex.encode(Bytes.of("test_delegate"));
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.DELEGATE);
    }

    @Test
    public void testVote() throws IOException, InterruptedException {
        EdDSA delegate = new EdDSA();
        delegateState.register(delegate.toAddress(), Bytes.of("test_vote"));

        String uri = "/vote?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + delegate.toAddressString()
                + "&value=1000000000&fee=50000000";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(0).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(0).getType(), TransactionType.VOTE);
    }

    @Test
    public void testUnvote() throws IOException, InterruptedException {
        EdDSA delegate = new EdDSA();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        long amount = 1000000000;
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String uri = "/unvote?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + delegate.toAddressString()
                + "&value=" + amount + "&fee=50000000";
        JsonObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.UNVOTE);
    }

    private Block createBlock(Blockchain chain, List<Transaction> transactions, List<TransactionResult> results) {
        EdDSA key = new EdDSA();

        long number = chain.getLatestBlockNumber() + 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = chain.getLatestBlockHash();
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header, transactions, results);
    }

    private Transaction createTransaction() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] to = key.toAddress();
        long value = 0;
        long fee = 0;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = {};

        Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        return tx;
    }
}