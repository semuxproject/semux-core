/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_1_0;

import static org.semux.core.Amount.Unit.SEM;

import java.util.Collections;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.semux.api.SemuxApiMock;
import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.rules.KernelRule;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public abstract class SemuxApiTestBase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    protected org.semux.api.v1_1_0.client.SemuxApi api;

    protected SemuxApiMock apiMock;
    protected Config config;
    protected Wallet wallet;
    protected Blockchain chain;
    protected AccountState accountState;
    protected DelegateState delegateState;
    protected PendingManager pendingMgr;
    protected NodeManager nodeMgr;
    protected ChannelManager channelMgr;

    @Before
    public void setUp() {
        apiMock = new SemuxApiMock(kernelRule.getKernel());
        apiMock.start();

        config = apiMock.getKernel().getConfig();
        wallet = apiMock.getKernel().getWallet();

        chain = apiMock.getKernel().getBlockchain();
        accountState = apiMock.getKernel().getBlockchain().getAccountState();
        accountState.adjustAvailable(wallet.getAccount(0).toAddress(), SEM.of(5000));
        delegateState = apiMock.getKernel().getBlockchain().getDelegateState();
        pendingMgr = apiMock.getKernel().getPendingManager();
        nodeMgr = apiMock.getKernel().getNodeManager();
        channelMgr = apiMock.getKernel().getChannelManager();

        api = JAXRSClientFactory.create(
                "http://localhost:51710/v1.1",
                org.semux.api.v1_1_0.client.SemuxApi.class,
                Collections.singletonList(new JacksonJsonProvider()),
                config.apiUsername(),
                config.apiPassword(),
                null);
    }

    @After
    public void tearDown() {
        apiMock.stop();
    }
}
