package org.semux.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Genesis;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.utils.ByteArray;
import org.semux.utils.MerkleTree;

public class APIHandlerTest {

    private static APIServerMock api;

    @BeforeClass
    public static void setup() {
        Config.init();

        api = new APIServerMock();
        api.start(Config.API_LISTEN_IP, Config.API_LISTEN_PORT);
    }

    @Before
    public void lockWallet() {
        Wallet.getInstance().lock();
    }

    private static JSONObject request(String uri) throws IOException {
        URL u = new URL("http://127.0.0.1:" + Config.API_LISTEN_PORT + uri);
        try (Scanner s = new Scanner(u.openStream())) {
            return new JSONObject(s.nextLine());
        }
    }

    @Test
    public void testRoot() throws IOException {
        String uri = "/";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
    }

    @Test
    public void testGetInfo() throws IOException {
        String uri = "/get_info";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        JSONObject result = response.getJSONObject("result");

        assertNotNull(result);
        assertEquals(0, result.getInt("latestBlockNumber"));
    }

    @Test
    public void testGetActiveNodes() throws IOException {
        String uri = "/get_active_nodes";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        JSONArray result = response.getJSONArray("result");

        assertNotNull(result);
        assertEquals(0, result.length());
    }

    @Test
    public void testAddNode() throws IOException {
        String uri = "/add_node?node=127.0.0.1:5162";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        assertEquals(1, api.nodeMgr.queueSize());
    }

    @Test
    public void testBlockIp() throws IOException {
        String uri = "/block_ip?ip=8.8.8.8";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        assertTrue(Config.NET_BLACKLIST.contains("8.8.8.8"));
    }

    @Test
    public void testGetLatestBlockNumber() throws IOException {
        String uri = "/get_latest_block_number";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(0, response.getLong("result"));
    }

    @Test
    public void testGetLatestBlock() throws IOException {
        String uri = "/get_latest_block";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        JSONObject block = response.getJSONObject("result");

        Genesis gen = Genesis.getInstance();
        assertArrayEquals(gen.getHash(), Hex.parse(block.getString("hash")));
        assertEquals(gen.getNumber(), block.getLong("number"));
        assertArrayEquals(gen.getCoinbase(), Hex.parse(block.getString("coinbase")));
        assertArrayEquals(gen.getPrevHash(), Hex.parse(block.getString("prevHash")));
        assertEquals(gen.getTimestamp(), block.getLong("timestamp"));
        assertArrayEquals(gen.getMerkleRoot(), Hex.parse(block.getString("merkleRoot")));
        assertArrayEquals(gen.getData(), Hex.parse(block.getString("data")));
    }

    @Test
    public void testGetBlock() throws IOException {
        Genesis gen = Genesis.getInstance();

        String uri = "/get_block?number=" + gen.getNumber();
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertArrayEquals(gen.getHash(), Hex.parse(response.getJSONObject("result").getString("hash")));

        uri = "/get_block?hash=" + Hex.encode(gen.getHash());
        response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertArrayEquals(gen.getHash(), Hex.parse(response.getJSONObject("result").getString("hash")));
    }

    private Block createBlock(Blockchain chain, List<Transaction> transactions) {
        EdDSA key = new EdDSA();

        long number = chain.getLatestBlockNumber() + 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = chain.getLatestBlockHash();
        long timestamp = System.currentTimeMillis();
        byte[] merkleRoot = {};
        byte[] data = {};

        List<byte[]> hashes = new ArrayList<>();
        for (Transaction tx : transactions) {
            hashes.add(tx.getHash());
        }
        merkleRoot = new MerkleTree(hashes).getRootHash();

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, merkleRoot, data);
        return new Block(header.sign(key), transactions);
    }

    private Transaction createTransaction() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = key.toAddress();
        long value = 0;
        long fee = 0;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = {};

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        return tx;
    }

    @Test
    public void testGetPendingTransactions() throws IOException {
        Transaction tx = createTransaction();
        Block block = createBlock(api.chain, Collections.singletonList(tx));
        api.chain.addBlock(block);

        try {
            String uri = "/get_pending_transactions";
            JSONObject response = request(uri);
            assertTrue(response.getBoolean("success"));

            JSONArray arr = response.getJSONArray("result");
            assertNotNull(arr);
        } finally {
            // Reset the API server
            teardown();
            setup();
        }
    }

    @Test
    public void testGetTransaction() throws IOException {
        Transaction tx = createTransaction();
        Block block = createBlock(api.chain, Collections.singletonList(tx));
        api.chain.addBlock(block);

        try {
            String uri = "/get_transaction?hash=" + Hex.encode(tx.getHash());
            JSONObject response = request(uri);
            assertTrue(response.getBoolean("success"));

            JSONObject obj = response.getJSONObject("result");
            assertArrayEquals(tx.getHash(), Hex.parse(obj.getString("hash")));
        } finally {
            // Reset the API server
            teardown();
            setup();
        }
    }

    @Test
    public void testNewTransaction() throws IOException, InterruptedException {
        Transaction tx = createTransaction();

        String uri = "/send_transaction?raw=" + Hex.encode(tx.toBytes());
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        Thread.sleep(200);
        List<Transaction> list = api.pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), tx.getHash());
    }

    @Test
    public void testUnlockWallet() throws IOException {
        String uri = "/unlock_wallet?password=12345678";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertTrue(response.getBoolean("result"));

        assertFalse(Wallet.getInstance().isLocked());
        Wallet.getInstance().lock();
    }

    @Test
    public void testLockWallet() throws IOException {
        assertTrue(Wallet.getInstance().unlock("12345678"));

        String uri = "/lock_wallet";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));

        assertTrue(Wallet.getInstance().isLocked());
    }

    @Test
    public void testGetAccounts() throws IOException {
        String uri = "/get_accounts";
        JSONObject response = request(uri);
        assertFalse(response.getBoolean("success"));

        Wallet.getInstance().unlock("12345678");
        response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getJSONArray("result"));
    }

    @Test
    public void testNewAccount() throws IOException {
        Wallet w = Wallet.getInstance();
        w.unlock("12345678");
        int size = w.getAccounts().size();

        String uri = "/new_account";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(size + 1, w.getAccounts().size());
    }

    @Test
    public void testGetBalance() throws IOException {
        Genesis gen = Genesis.getInstance();
        Entry<ByteArray, Long> entry = gen.getPremine().entrySet().iterator().next();

        String uri = "/get_balance?address=" + entry.getKey();
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals((long) entry.getValue(), response.getLong("result"));
    }

    @Test
    public void testGetNonce() throws IOException {
        Genesis gen = Genesis.getInstance();
        Entry<ByteArray, Long> entry = gen.getPremine().entrySet().iterator().next();

        String uri = "/get_nonce?address=" + entry.getKey();
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getLong("result"));
    }

    @Test
    public void testGetDelegates() throws IOException {
        String uri = "/get_delegates";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertTrue(response.getJSONArray("result").length() > 0);
    }

    @Test
    public void testGetDelegate() throws IOException {
        Genesis gen = Genesis.getInstance();
        Entry<String, byte[]> entry = gen.getDelegates().entrySet().iterator().next();

        String uri = "/get_delegate?name=" + entry.getKey();
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(entry.getKey(), response.getJSONObject("result").getString("name"));

        uri = "/get_delegate?address=" + Hex.encode(entry.getValue());
        response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertEquals(entry.getKey(), response.getJSONObject("result").getString("name"));
    }

    @Test
    public void testTransfer() throws IOException, InterruptedException {
        Wallet.getInstance().unlock("12345678");

        EdDSA key = new EdDSA();
        String uri = "/transfer?from=0&to=" + key.toAddressString() + "&value=1000000000&fee=5000000&data=test";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = api.pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.TRANSFER);
    }

    @Test
    public void testDelegate() throws IOException, InterruptedException {
        Wallet.getInstance().unlock("12345678");

        EdDSA key = Wallet.getInstance().getAccounts().get(0);
        String uri = "/delegate?from=0&to=" + key.toAddressString() + "&value=" + Config.BFT_REGISTRATION_FEE
                + "&fee=5000000&data=test";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = api.pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.DELEGATE);
    }

    @Test
    public void testVote() throws IOException, InterruptedException {
        Wallet.getInstance().unlock("12345678");

        EdDSA key = new EdDSA();
        String uri = "/vote?from=0&to=" + key.toAddressString() + "&value=1000000000&fee=5000000&data=test";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = api.pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(0).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(0).getType(), TransactionType.VOTE);
    }

    @Test
    public void testUnvote() throws IOException, InterruptedException {
        Wallet.getInstance().unlock("12345678");

        EdDSA key = new EdDSA();
        String uri = "/unvote?from=0&to=" + key.toAddressString() + "&value=1000000000&fee=5000000&data=test";
        JSONObject response = request(uri);
        assertTrue(response.getBoolean("success"));
        assertNotNull(response.getString("result"));

        Thread.sleep(200);

        List<Transaction> list = api.pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.getString("result")));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.UNVOTE);
    }

    @AfterClass
    public static void teardown() {
        api.stop();
    }
}
