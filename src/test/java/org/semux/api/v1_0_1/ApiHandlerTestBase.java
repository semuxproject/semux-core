/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_0_1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.semux.Network;
import org.semux.TestUtils;
import org.semux.api.SemuxApiMock;
import org.semux.api.v1_0_1.ApiHandlerResponse;
import org.semux.config.Config;
import org.semux.core.Amount;
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
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.util.BasicAuth;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @deprecated
 */
public abstract class ApiHandlerTestBase {

    protected SemuxApiMock api;

    protected Config config;
    protected Wallet wallet;

    protected Blockchain chain;
    protected AccountState accountState;
    protected DelegateState delegateState;
    protected PendingManager pendingMgr;
    protected NodeManager nodeMgr;
    protected ChannelManager channelMgr;

    protected <T extends ApiHandlerResponse> T request(String uri, Class<T> clazz) throws IOException {
        URL u = new URL("http://" + config.apiListenIp() + ":" + config.apiListenPort() + uri);

        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestProperty("Authorization", BasicAuth.generateAuth(config.apiUsername(), config.apiPassword()));

        return new ObjectMapper().readValue(con.getInputStream(), clazz);
    }

    protected <T extends ApiHandlerResponse> T postRequest(String uri, String body, Class<T> clazz)
            throws IOException {
        URL u = new URL("http://" + config.apiListenIp() + ":" + config.apiListenPort() + uri);

        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", BasicAuth.generateAuth(config.apiUsername(), config.apiPassword()));
        con.setDoOutput(true);

        // write body
        OutputStream os = con.getOutputStream();
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        bufferedWriter.write(body);
        bufferedWriter.flush();
        bufferedWriter.close();
        os.close();

        return new ObjectMapper().readValue(con.getInputStream(), clazz);
    }

    protected Block createBlock(Blockchain chain, List<Transaction> transactions, List<TransactionResult> results) {
        return TestUtils.createBlock(
                chain.getLatestBlockNumber() + 1,
                transactions,
                results);
    }

    protected Transaction createTransaction() {
        return TestUtils.createTransaction(config);
    }

    protected Transaction createTransaction(Key from, Key to, Amount value) {
        return TestUtils.createTransaction(config, from, to, value);
    }
}