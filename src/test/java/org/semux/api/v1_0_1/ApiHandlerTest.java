/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_0_1;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.api.SemuxApiMock;
import org.semux.api.v1_0_1.response.AddNodeResponse;
import org.semux.api.v1_0_1.response.CreateAccountResponse;
import org.semux.api.v1_0_1.response.DoTransactionResponse;
import org.semux.api.v1_0_1.response.GetAccountResponse;
import org.semux.api.v1_0_1.response.GetAccountTransactionsResponse;
import org.semux.api.v1_0_1.response.GetBlockResponse;
import org.semux.api.v1_0_1.response.GetDelegateResponse;
import org.semux.api.v1_0_1.response.GetDelegatesResponse;
import org.semux.api.v1_0_1.response.GetInfoResponse;
import org.semux.api.v1_0_1.response.GetLatestBlockNumberResponse;
import org.semux.api.v1_0_1.response.GetLatestBlockResponse;
import org.semux.api.v1_0_1.response.GetPeersResponse;
import org.semux.api.v1_0_1.response.GetPendingTransactionsResponse;
import org.semux.api.v1_0_1.response.GetRootResponse;
import org.semux.api.v1_0_1.response.GetTransactionLimitsResponse;
import org.semux.api.v1_0_1.response.GetTransactionResponse;
import org.semux.api.v1_0_1.response.GetValidatorsResponse;
import org.semux.api.v1_0_1.response.GetVoteResponse;
import org.semux.api.v1_0_1.response.GetVotesResponse;
import org.semux.api.v1_0_1.response.ListAccountsResponse;
import org.semux.api.v1_0_1.response.SendTransactionResponse;
import org.semux.api.v1_0_1.response.SignMessageResponse;
import org.semux.api.v1_0_1.response.Types;
import org.semux.api.v1_0_1.response.VerifyMessageResponse;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.Peer;
import org.semux.net.filter.FilterRule;
import org.semux.net.filter.SemuxIpFilter;
import org.semux.rules.KernelRule;
import org.semux.util.Bytes;

import io.netty.handler.ipfilter.IpFilterRuleType;
import net.bytebuddy.utility.RandomString;

/**
 * @deprecated
 */
@SuppressWarnings("Duplicates")
public class ApiHandlerTest extends ApiHandlerTestBase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Before
    public void setUp() {
        api = new SemuxApiMock(kernelRule.getKernel());
        api.start();

        config = api.getKernel().getConfig();
        wallet = api.getKernel().getWallet();

        chain = api.getKernel().getBlockchain();
        accountState = api.getKernel().getBlockchain().getAccountState();
        accountState.adjustAvailable(wallet.getAccount(0).toAddress(), SEM.of(5000));
        delegateState = api.getKernel().getBlockchain().getDelegateState();
        pendingMgr = api.getKernel().getPendingManager();
        nodeMgr = api.getKernel().getNodeManager();
        channelMgr = api.getKernel().getChannelManager();
    }

    @After
    public void tearDown() {
        api.stop();
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
        assertEquals(Long.valueOf(0), response.info.latestBlockNumber);
        assertEquals(Integer.valueOf(0), response.info.activePeers);
        assertEquals(Integer.valueOf(0), response.info.pendingTransactions);
    }

    @Test
    public void testSignatures() throws IOException {

        String address = wallet.getAccount(0).toAddressString();
        String addressOther = wallet.getAccount(1).toAddressString();

        String message = "helloworld";
        String uri = "/sign_message?address=0x" + address + "&message=" + message;
        SignMessageResponse response = request(uri, SignMessageResponse.class);
        assertTrue(response.success);
        String signature = response.signature;
        uri = "/verify_message?address=" + address + "&message=" + message + "&signature=" + signature;
        VerifyMessageResponse verifyMessageResponse = request(uri, VerifyMessageResponse.class);
        assertTrue(verifyMessageResponse.success);
        assertTrue(verifyMessageResponse.validSignature);

        // verify no messing with fromaddress
        uri = "/verify_message?address=" + addressOther + "&message=" + message + "&signature=" + signature;
        verifyMessageResponse = request(uri, VerifyMessageResponse.class);
        assertTrue(verifyMessageResponse.success);
        assertFalse(verifyMessageResponse.validSignature);

        // verify no messing with message
        uri = "/verify_message?address=" + addressOther + "&message=" + message + "hi" + "&signature=" + signature;
        verifyMessageResponse = request(uri, VerifyMessageResponse.class);
        assertTrue(verifyMessageResponse.success);
        assertFalse(verifyMessageResponse.validSignature);
    }

    @Test
    public void testGetPeers() throws IOException {
        channelMgr = spy(api.getKernel().getChannelManager());
        List<Peer> peers = Arrays.asList(
                new Peer("1.2.3.4", 5161, (short) 1, "client1", "peer1", 1, config.capabilitySet()),
                new Peer("2.3.4.5", 5171, (short) 2, "client2", "peer2", 2, config.capabilitySet()));
        when(channelMgr.getActivePeers()).thenReturn(peers);
        api.getKernel().setChannelManager(channelMgr);

        GetPeersResponse response = request("/get_peers", GetPeersResponse.class);
        assertTrue(response.success);
        List<Types.PeerType> result = response.peers;

        assertNotNull(result);
        assertEquals(peers.size(), result.size());
        for (int i = 0; i < peers.size(); i++) {
            Types.PeerType peerJson = result.get(i);
            Peer peer = peers.get(i);
            assertEquals(peer.getIp(), peerJson.ip);
            assertEquals(peer.getPort(), peerJson.port.intValue());
            assertEquals(peer.getNetworkVersion(), peerJson.networkVersion.shortValue());
            assertEquals(peer.getClientId(), peerJson.clientId);
            assertEquals(Hex.PREF + peer.getPeerId(), peerJson.peerId);
            assertEquals(peer.getLatestBlockNumber(), peerJson.latestBlockNumber.longValue());
            assertEquals(peer.getLatency(), peerJson.latency.longValue());
            assertEquals(peer.getCapabilities().toList(), peerJson.capabilities);
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
        ChannelManager channelManagerSpy = spy(kernelRule.getKernel().getChannelManager());
        kernelRule.getKernel().setChannelManager(channelManagerSpy);

        // blacklist 8.8.8.8
        assertTrue(request("/add_to_blacklist?ip=8.8.8.8", ApiHandlerResponse.class).success);
        verify(channelManagerSpy).removeBlacklistedChannels();

        // assert that 8.8.8.8 is no longer acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertFalse(channelMgr.isAcceptable(inetSocketAddress));

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
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

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
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

        Types.BlockType blockJson = response.block;
        assertEquals(Hex.encode0x(genesisBlock.getHash()), blockJson.hash);
        assertEquals(genesisBlock.getNumber(), blockJson.number.longValue());
        assertEquals(Hex.encode0x(genesisBlock.getCoinbase()), blockJson.coinbase);
        assertEquals(Hex.encode0x(genesisBlock.getParentHash()), blockJson.parentHash);
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
        TransactionResult result = new TransactionResult(true);
        PendingManager pendingManager = spy(kernelRule.getKernel().getPendingManager());
        when(pendingManager.getPendingTransactions()).thenReturn(
                Collections.singletonList(new PendingManager.PendingTransaction(tx, result)));
        kernelRule.getKernel().setPendingManager(pendingManager);

        String uri = "/get_pending_transactions";
        GetPendingTransactionsResponse response = request(uri, GetPendingTransactionsResponse.class);
        assertTrue(response.success);
        assertNotNull(response.pendingTransactions);
        assertThat(response.pendingTransactions, hasSize(1));
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
        for (Types.TransactionType txType : response.transactions) {
            assertEquals(block.getNumber(), txType.blockNumber.longValue());
        }
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
        assertEquals(block.getNumber(), response.transaction.blockNumber.longValue());
        assertNotNull(response.transaction.to);
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
        // create an account
        Key key = new Key();
        accountState.adjustAvailable(key.toAddress(), SEM.of(1000));
        chain.addBlock(createBlock(
                chain,
                Collections.singletonList(createTransaction(key, key, Amount.ZERO)),
                Collections.singletonList(new TransactionResult(true))));

        // request api endpoint
        String uri = "/get_account?address=" + key.toAddressString();
        GetAccountResponse response = request(uri, GetAccountResponse.class);
        assertTrue(response.success);
        assertEquals(SEM.of(1000).getNano(), response.account.available);
        assertEquals(1, response.account.transactionCount);
    }

    @Test
    public void testGetDelegate() throws IOException {
        Genesis gen = chain.getGenesis();
        Entry<String, byte[]> entry = gen.getDelegates().entrySet().iterator().next();

        String uri = "/get_delegate?address=" + Hex.encode(entry.getValue());
        GetDelegateResponse response = request(uri, GetDelegateResponse.class);
        assertTrue(response.success);
        assertEquals(entry.getKey(), response.delegate.name);
    }

    @Test
    public void testGetDelegates() throws IOException {
        String uri = "/get_delegates";
        GetDelegatesResponse response = request(uri, GetDelegatesResponse.class);
        assertTrue(response.success);
        assertTrue(response.delegates.size() > 0);
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
        Key key = new Key();
        Key key2 = new Key();
        DelegateState ds = chain.getDelegateState();
        ds.register(key2.toAddress(), Bytes.of("test"));
        ds.vote(key.toAddress(), key2.toAddress(), NANO_SEM.of(200));

        String uri = "/get_vote?voter=" + key.toAddressString() + "&delegate=" + key2.toAddressString();
        GetVoteResponse response = request(uri, GetVoteResponse.class);
        assertTrue(response.success);
        assertEquals(200L, response.vote.longValue());
    }

    @Test
    public void testGetVotes() throws IOException {
        Key voterKey = new Key();
        Key delegateKey = new Key();
        DelegateState ds = chain.getDelegateState();
        assertTrue(ds.register(delegateKey.toAddress(), Bytes.of("test")));
        assertTrue(ds.vote(voterKey.toAddress(), delegateKey.toAddress(), NANO_SEM.of(200)));
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
    public void testCreateAccountWithName() throws IOException {
        int size = wallet.getAccounts().size();

        String uri = "/create_account?name=test_name";
        CreateAccountResponse response = request(uri, CreateAccountResponse.class);
        assertTrue(response.success);
        List<Key> accounts = wallet.getAccounts();
        assertEquals(size + 1, accounts.size());
        assertEquals(Optional.of("test_name"), wallet.getAddressAlias(accounts.get(size).toAddress()));
    }

    @Test
    public void testGetTransactionLimits() throws IOException {
        for (TransactionType type : TransactionType.values()) {
            String uri = "/get_transaction_limits?type=" + type.toString();
            GetTransactionLimitsResponse response = request(uri, GetTransactionLimitsResponse.class);
            assertTrue(response.success);
            assertEquals(config.maxTransactionDataSize(type),
                    response.transactionLimits.maxTransactionDataSize.intValue());
            assertEquals(config.minTransactionFee().getNano(),
                    response.transactionLimits.minTransactionFee.longValue());

            if (type.equals(TransactionType.DELEGATE)) {
                assertEquals(config.minDelegateBurnAmount().getNano(),
                        response.transactionLimits.minDelegateBurnAmount.longValue());
            } else {
                assertNull(response.transactionLimits.minDelegateBurnAmount);
            }
        }
    }

    @Test
    public void testTransfer() throws IOException, InterruptedException {
        Key key = new Key();
        String uri = "/transfer?"
                + "&from=" + wallet.getAccount(0).toAddressString()
                + "&to=" + key.toAddressString()
                + "&value=1000000000&fee=" + config.minTransactionFee().getNano()
                + "&data=" + Hex.encode(Bytes.of("test_transfer"));
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txHash);

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.txHash));
        assertEquals(list.get(list.size() - 1).transaction.getType(), TransactionType.TRANSFER);
    }

    @Test
    public void testDelegate() throws IOException, InterruptedException {
        String uri = "/delegate?"
                + "&from=" + wallet.getAccount(0).toAddressString()
                + "&fee=" + config.minTransactionFee().getNano()
                + "&data=" + Hex.encode(Bytes.of("test_delegate"));
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txHash);

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.txHash));
        assertEquals(list.get(list.size() - 1).transaction.getType(), TransactionType.DELEGATE);
    }

    @Test
    public void testVote() throws IOException, InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_vote"));

        String uri = "/vote?&from=" + wallet.getAccount(0).toAddressString() + "&to=" + delegate.toAddressString()
                + "&value=1000000000&fee=50000000";
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txHash);

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(0).transaction.getHash(), Hex.decode0x(response.txHash));
        assertEquals(TransactionType.VOTE, list.get(0).transaction.getType());
    }

    @Test
    public void testUnvote() throws IOException, InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        Amount amount = NANO_SEM.of(1000000000);
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String uri = "/unvote?"
                + "&from=" + wallet.getAccount(0).toAddressString()
                + "&to=" + delegate.toAddressString()
                + "&value=" + amount.getNano()
                + "&fee=50000000";
        DoTransactionResponse response = request(uri, DoTransactionResponse.class);
        assertTrue(response.success);
        assertNotNull(response.txHash);

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.txHash));
        assertEquals(TransactionType.UNVOTE, list.get(list.size() - 1).transaction.getType());
    }
}