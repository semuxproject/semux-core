/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.semux.core.Unit.SEM;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.semux.config.Config;
import org.semux.core.Amount;
import org.semux.core.Blockchain;
import org.semux.core.Fork;
import org.semux.core.PendingManager;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.rules.KernelRule;

public abstract class SemuxApiTestBase {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

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
        accountState.adjustAvailable(wallet.getAccount(0).toAddress(), Amount.of(5000, SEM));
        delegateState = apiMock.getKernel().getBlockchain().getDelegateState();
        pendingMgr = apiMock.getKernel().getPendingManager();
        nodeMgr = apiMock.getKernel().getNodeManager();
        channelMgr = apiMock.getKernel().getChannelManager();

        kernelRule.enableForks(Fork.VIRTUAL_MACHINE, Fork.VOTING_PRECOMPILED_UPGRADE);
    }

    @After
    public void tearDown() {
        apiMock.stop();
    }
}
