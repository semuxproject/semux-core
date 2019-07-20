/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.semux.core.Amount.ZERO;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.semux.KernelMock;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Wallet;
import org.semux.db.LeveldbDatabase;
import org.semux.gui.model.WalletModel;
import org.semux.net.ChannelManager;
import org.semux.rules.KernelRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxGuiTest {

    private static final Logger logger = LoggerFactory.getLogger(SemuxGuiTest.class);

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Test
    public void testStart() throws ParseException, IOException {
        SemuxGui gui = spy(new SemuxGui());

        Mockito.doNothing().when(gui).showWelcome(any());
        Mockito.doNothing().when(gui).checkFilePermissions(any());
        Mockito.doCallRealMethod().when(gui).unlockWallet(any());
        Mockito.doNothing().when(gui).setupSplashScreen();
        Mockito.doReturn(0).when(gui).setupCoinbase(any());
        Mockito.doNothing().when(gui).startKernel(any(), any(), any());

        String[] args = new String[] {
                "--datadir", kernelRule.getKernel().getConfig().dataDir().getAbsolutePath(),
                "--network", "mainnet",
                "--coinbase", "0",
                "--password", kernelRule.getPassword()
        };
        gui.start(args);

        assertThat(gui.getDataDir()).isEqualTo(args[1]);
        assertThat(gui.getNetwork()).isEqualTo(Network.MAINNET);
        assertThat(gui.getCoinbase()).isEqualTo(0);
        assertThat(gui.getPassword()).isEqualTo(kernelRule.getPassword());
        verify(gui).checkFilePermissions(any());
        verify(gui).unlockWallet(any());
        verify(gui).setupSplashScreen();
        verify(gui).setupCoinbase(any());
    }

    @Test
    public void testSetupCoinbase() throws ParseException {
        Wallet wallet = kernelRule.getKernel().getWallet();

        // setup coinbase
        SemuxGui gui = spy(new SemuxGui(new WalletModel(kernelRule.getKernel().getConfig()), kernelRule.getKernel()));
        Mockito.doNothing().when(gui).startKernel(any(), any(), any());
        Mockito.doReturn(3).when(gui).showSelectDialog(any(), any(), any());

        // verify
        assertThat(gui.setupCoinbase(wallet)).isEqualTo(3);
    }

    @Test
    public void testProcessBlock() {
        KernelMock kernel = kernelRule.getKernel();

        WalletModel model = new WalletModel(kernel.getConfig());

        SemuxGui gui = new SemuxGui(model, kernel);

        // prepare kernel
        Config config = kernel.getConfig();
        Blockchain chain = new BlockchainImpl(config,
                new LeveldbDatabase.LeveldbFactory(kernel.getConfig().databaseDir()));
        kernel.setBlockchain(chain);
        ChannelManager channelMgr = new ChannelManager(kernel);
        kernel.setChannelManager(channelMgr);
        SemuxSync syncMgr = new SemuxSync(kernel);
        kernel.setSyncManager(syncMgr);

        // process block
        gui.updateModel();

        // assertions
        assertThat(model.getLatestBlock().getNumber()).isEqualTo(0L);
        assertThat(model.getAccounts().size()).isEqualTo(kernel.getWallet().size());
        assertThat(model.getDelegates().size()).isEqualTo(chain.getDelegateState().getDelegates().size());
        assertThat(model.getTotalAvailable()).isEqualTo(ZERO);
        assertThat(model.getTotalLocked()).isEqualTo(ZERO);
        assertThat(model.getActivePeers().size()).isEqualTo(channelMgr.getActivePeers().size());
        assertThat(model.getSyncProgress().get()).isEqualToComparingOnlyGivenFields(syncMgr.getProgress(),
                "startingHeight",
                "currentHeight", "targetHeight");
    }

    @Test
    public void testGetMinVersion() {
        SemuxGui gui = spy(new SemuxGui());
        SemuxGui.Version v = gui.getCurrentVersions();
        logger.info("Min version: {}", v.minVersion);
        logger.info("Latest version: {}", v.latestVersion);

        assertThat(v).isNotNull();
    }
}
