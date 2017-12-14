/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.semux.api.response.ApiHandlerResponse;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.util.BasicAuth;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ApiHandlerTestBase {

    protected SemuxAPIMock api;
    protected static Config config;
    protected Wallet wallet;
    protected Blockchain chain;
    protected AccountState accountState;
    protected DelegateState delegateState;
    protected PendingManager pendingMgr;
    protected NodeManager nodeMgr;
    protected ChannelManager channelMgr;

    protected static <T extends ApiHandlerResponse> T request(String uri, Class<T> clazz) throws IOException {
        URL u = new URL("http://" + ApiServerRule.API_IP + ":" + ApiServerRule.API_PORT + uri);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();

        con.setRequestProperty("Authorization", BasicAuth.generateAuth(config.apiUsername(), config.apiPassword()));

        InputStream inputStream = con.getResponseCode() < 400 ? con.getInputStream() : con.getErrorStream();
        return new ObjectMapper().readValue(inputStream, clazz);
    }

    protected Block createBlock(Blockchain chain, List<Transaction> transactions, List<TransactionResult> results) {
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

    protected Transaction createTransaction() {
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