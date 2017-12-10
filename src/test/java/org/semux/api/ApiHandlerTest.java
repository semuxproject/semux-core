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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.api.response.AddNodeResponse;
import org.semux.api.response.ApiHandlerResponse;
import org.semux.api.response.CreateAccountResponse;
import org.semux.api.response.DoTransactionResponse;
import org.semux.api.response.GetAccountResponse;
import org.semux.api.response.GetAccountTransactionsResponse;
import org.semux.api.response.GetBlockResponse;
import org.semux.api.response.GetDelegateResponse;
import org.semux.api.response.GetDelegatesResponse;
import org.semux.api.response.GetInfoResponse;
import org.semux.api.response.GetLatestBlockNumberResponse;
import org.semux.api.response.GetLatestBlockResponse;
import org.semux.api.response.GetPeersResponse;
import org.semux.api.response.GetPendingTransactionsResponse;
import org.semux.api.response.GetRootResponse;
import org.semux.api.response.GetTransactionResponse;
import org.semux.api.response.GetValidatorsResponse;
import org.semux.api.response.GetVoteResponse;
import org.semux.api.response.GetVotesResponse;
import org.semux.api.response.ListAccountsResponse;
import org.semux.api.response.SendTransactionResponse;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.core.Genesis.Premine;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.net.Peer;
import org.semux.net.filter.FilterRule;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

import io.netty.handler.ipfilter.IpFilterRuleType;
import net.bytebuddy.utility.RandomString;

public class ApiHandlerTest extends ApiHandlerTestBase {

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    @Rule
    public ApiServerRule apiServerRule = new ApiServerRule(temporaryDBFactory);

    @Before
    public void setUp() {
        api = apiServerRule.getApi();
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
        if (wallet.exists()) {
            wallet.delete();
        }
    }

    @Test
    public void testInvalidCommand() throws IOException {
        String uri = "/" + RandomString.make(32);
        ApiHandlerResponse response = request(uri, ApiHandlerResponse.class);
        assertFalse(response.success);
    }

    @Test
    public void testRoot() throws IOException {
        String uri = "/";
        GetRootResponse response = request(uri, GetRootResponse.class);
        assertTrue(response.success);
    }

    @Test
    public void testGetInfo() throws IOException {
        String uri = "/get_info";
        GetInfoResponse response = request(uri, GetInfoResponse.class);
        assertTrue(response.success);
        assertNotNull(response.info);
        assertEquals(0, response.info.latestBlockNumber);
    }

    @Test
    public void testGetPeers() throws IOException {
        channelMgr = spy(api.getKernel().getChannelManager());
        List<Peer> peers = Arrays.asList(new Peer("1.2.3.4", 5161, (short) 1, "client1", "peer1", 1),
                new Peer("2.3.4.5", 5171, (short) 2, "client2", "peer2", 2));
        when(channelMgr.getActivePeers()).thenReturn(peers);
        api.getKernel().setChannelManager(channelMgr);

        GetPeersResponse response = request("/get_peers", GetPeersResponse.class);
        assertTrue(response.success);
        List<GetPeersResponse.Result> result = response.peers;

        assertNotNull(result);
        assertEquals(peers.size(), result.size());
        for (int i = 0; i < peers.size(); i++) {
            GetPeersResponse.Result peerJson = result.get(i);
            Peer peer = peers.get(i);
            assertEquals(peer.getIp(), peerJson.ip);
            assertEquals(peer.getPort(), peerJson.port.intValue());
            assertEquals(peer.getNetworkVersion(), peerJson.networkVersion.shortValue());
            assertEquals(peer.getClientId(), peerJson.clientId);
            assertEquals(Hex.PREF + peer.getPeerId(), peerJson.peerId);
            assertEquals(peer.getLatestBlockNumber(), peerJson.latestBlockNumber.longValue());
            assertEquals(peer.getLatency(), peerJson.latency.longValue());
        }
    }

    @Test
    public void testAddNode() throws IOException {
        String uri = "/add_node?node=127.0.0.1:5162";
        AddNodeResponse response = request(uri, AddNodeResponse.class);
        assertTrue(response.success);
        assertEquals(1, nodeMgr.queueSize());
    }

    @Test
    public void testAddToBlacklist() throws IOException {
        // blacklist 8.8.8.8
        assertTrue(request("/add_to_blacklist?ip=8.8.8.8", ApiHandlerResponse.class).success);

        // assert that 8.8.8.8 is no longer acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertFalse(channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void testAddToWhitelist() throws IOException {
        // reject all connections
        channelMgr.getIpFilter().appendRule(new FilterRule("0.0.0.0/0", IpFilterRuleType.REJECT));

        // whitelist 8.8.8.8
        assertTrue(request("/add_to_whitelist?ip=8.8.8.8", ApiHandlerResponse.class).success);

        // assert that 8.8.8.8 is acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void tesAddToBlacklistThenWhitelist() throws IOException {
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));

        ApiHandlerResponse response;

        response = request("/add_to_blacklist?ip=8.8.8.8", ApiHandlerResponse.class);
        assertTrue(response.success);
        assertTrue(!channelMgr.isAcceptable(inetSocketAddress));

        response = request("/add_to_whitelist?ip=8.8.8.8", ApiHandlerResponse.class);
        assertTrue(response.success);
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));

        assertTrue(request("/add_to_blacklist?ip=8.8.8.8", ApiHandlerResponse.class).success);
        assertTrue(!channelMgr.isAcceptable(inetSocketAddress));
    }

    @Test
    public void testGetLatestBlockNumber() throws IOException {
        String uri = "/get_latest_block_number";
        GetLatestBlockNumberResponse response = request(uri, GetLatestBlockNumberResponse.class);
        assertTrue(response.success);
    }

    @Test
    public void testGetLatestBlock() throws IOException {
        Genesis genesisBlock = chain.getGenesis();

        String uri = "/get_latest_block";
        GetLatestBlockResponse response = request(uri, GetLatestBlockResponse.class);
        assertTrue(response.success);

        GetBlockResponse.Result blockJson = response.block;
        assertEquals(Hex.encode0x(genesisBlock.getHash()), blockJson.hash);
        assertEquals(genesisBlock.getNumber(), blockJson.number.longValue());
        assertEquals(Hex.encode0x(genesisBlock.getCoinbase()), blockJson.coinbase);
        assertEquals(Hex.encode0x(genesisBlock.getPrevHash()), blockJson.prevHash);
        assertEquals(genesisBlock.getTimestamp(), blockJson.timestamp.longValue());
        assertEquals(Hex.encode0x(genesisBlock.getTransactionsRoot()), blockJson.transactionsRoot);
        assertEquals(Hex.encode0x(genesisBlock.getData()), blockJson.data);
    }

    @Test
    public void testGetBlock() throws IOException {
        Genesis gen = chain.getGenesis();

        String uri = "/get_block?number=" + gen.getNumber();
        GetBlockResponse response = request(uri, GetBlockResponse.class);
        assertTrue(response.success);
        assertEquals(Hex.encode0x(gen.getHash()), response.block.hash);
        assertNotNull(response.block.transactions);

        uri = "/get_block?hash=" + Hex.encode(gen.getHash());
        response = request(uri, GetBlockResponse.class);
        assertTrue(response.success);
        assertEquals(Hex.encode0x(gen.getHash()), response.block.hash);
        assertNotNull(response.block.transactions);
    }

    @Test
    public void testGetPendingTransactions() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_pending_transactions";
        GetPendingTransactionsResponse response = request(uri, GetPendingTransactionsResponse.class);
        assertTrue(response.success);
        assertNotNull(response.pendingTransactions);
    }

    @Test
    public void testGetAccountTransactions() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_account_transactions?address=" + Hex.encode(tx.getFrom()) + "&from=0&to=1024";
        GetAccountTransactionsResponse response = request(uri, GetAccountTransactionsResponse.class);
        assertTrue(response.success);
        assertNotNull(response.transactions);
    }

    @Test
    public void testGetTransaction() throws IOException {
        Transaction tx = createTransaction();
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain, Collections.singletonList(tx), Collections.singletonList(res));
        chain.addBlock(block);

        String uri = "/get_transaction?hash=" + Hex.encode(tx.getHash());
        GetTransactionResponse response = request(uri, GetTransactionResponse.class);
        assertTrue(response.success);
        assertEquals(Hex.encode0x(tx.getHash()), response.transaction.hash);
    }

    @Test
    public void testSendTransaction() throws IOException, InterruptedException {
        Transaction tx = createTransaction();

        String uri = "/send_transaction?raw=" + Hex.encode(tx.toBytes());
        SendTransactionResponse response = request(uri, SendTransactionResponse.class);
        assertTrue(response.success);

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
        GetAccountResponse response = request(uri, GetAccountResponse.class);
        assertTrue(response.success);
        assertEquals(entry.getValue().getAmount() * Unit.SEM, response.account.available);
    }

    @Test
    public void testGetDelegate() throws IOException {
        Genesis gen = chain.getGenesis();
        Entry<String, byte[]> entry = gen.getDelegates().entrySet().iterator().next();

        String uri = "/get_delegate?address=" + Hex.encode(entry.getValue());
        GetDelegateResponse response = request(uri, GetDelegateResponse.class);
        assertTrue(response.success);
        assertEquals(entry.getKey(), response.delegateResult.name);
    }

    @Test
    public void testGetDelegates() throws IOException {
        String uri = "/get_delegates";
        GetDelegatesResponse response = request(uri, GetDelegatesResponse.class);
        assertTrue(response.success);
        assertTrue(response.delegateResults.size() > 0);
    }

    @Test
    public void testGetValidators() throws IOException {
        String uri = "/get_validators";
        GetValidatorsResponse response = request(uri, GetValidatorsResponse.class);
        assertTrue(response.success);
        assertTrue(response.validators.size() > 0);
    }

    @Test
    public void testGetVote() throws IOException {
        EdDSA key = new EdDSA();
        EdDSA key2 = new EdDSA();
        DelegateState ds = chain.getDelegateState();
        ds.register(key2.toAddress(), Bytes.of("test"));
        ds.vote(key.toAddress(), key2.toAddress(), 200L);

        String uri = "/get_vote?voter=" + key.toAddressString() + "&delegate=" + key2.toAddressString();
        GetVoteResponse response = request(uri, GetVoteResponse.class);
        assertTrue(response.success);
        assertEquals(200L, response.vote.longValue());
    }

    @Test
    public void testGetVotes() throws IOException {
        EdDSA voterKey = new EdDSA();
        EdDSA delegateKey = new EdDSA();
        DelegateState ds = chain.getDelegateState();
        assertTrue(ds.register(delegateKey.toAddress(), Bytes.of("test")));
        assertTrue(ds.vote(voterKey.toAddress(), delegateKey.toAddress(), 200L));
        ds.commit();

        GetVotesResponse response = request("/get_votes?delegate=" + delegateKey.toAddressString(),
                GetVotesResponse.class);
        assertTrue(response.success);
        assertEquals(200L, response.votes.get(Hex.PREF + voterKey.toAddressString()).longValue());
    }

    @Test
    public void testListAccounts() throws IOException {
        String uri = "/list_accounts";
        ListAccountsResponse response = request(uri, ListAccountsResponse.class);
        assertTrue(response.success);
        assertNotNull(response.accounts);
        assertTrue(response.accounts.size() > 0);
    }

    @Test
    public void testCreateAccount() throws IOException {
        int size = wallet.getAccounts().size();

        String uri = "/create_account";
        CreateAccountResponse response = request(uri, CreateAccountResponse.class);
        assertTrue(response.success);
        assertEquals(size + 1, wallet.getAccounts().size());
    }

    @Test
    public void testTransfer() throws IOException, InterruptedException {
        EdDSA key = new EdDSA();
        String uri = "/transfer?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + key.toAddressString()
                + "&value=1000000000&fee=" + config.minTransactionFee() + "&data="
                + Hex.encode(Bytes.of("test_transfer"));
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txId);

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.txId));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.TRANSFER);
    }

    @Test
    public void testTransferMany() throws IOException, InterruptedException {
        EdDSA key1 = new EdDSA(), key2 = new EdDSA();
        String uri = "/transfer_many?&from=" + wallet.getAccount(0).toAddressString() +
                "&to[]=" + key1.toAddressString() +
                "&to[]=" + key2.toAddressString() +
                "&value=1000000000&fee=" + config.minTransactionFee() +
                "&data=test";
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txId);

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.txId));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.TRANSFER_MANY);
    }

    @Test
    public void testDelegate() throws IOException, InterruptedException {
        String uri = "/delegate?&from=" + wallet.getAccount(0).toAddressString() + "&fee=" + config.minTransactionFee()
                + "&data=" + Hex.encode(Bytes.of("test_delegate"));
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txId);

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.txId));
        assertEquals(list.get(list.size() - 1).getType(), TransactionType.DELEGATE);
    }

    @Test
    public void testVote() throws IOException, InterruptedException {
        EdDSA delegate = new EdDSA();
        delegateState.register(delegate.toAddress(), Bytes.of("test_vote"));

        String uri = "/vote?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + delegate.toAddressString()
                + "&value=1000000000&fee=50000000";
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txId);

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(0).getHash(), Hex.parse(response.txId));
        assertEquals(TransactionType.VOTE, list.get(0).getType());
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
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txId);

        Thread.sleep(200);

        List<Transaction> list = pendingMgr.getTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), Hex.parse(response.txId));
        assertEquals(TransactionType.UNVOTE, list.get(list.size() - 1).getType());
    }
}