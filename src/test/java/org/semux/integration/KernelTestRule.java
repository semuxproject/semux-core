/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;

public class KernelTestRule extends TemporaryFolder {

    private KernelMock kernelMock;

    private int p2pPort, apiPort;

    public KernelTestRule(int p2pPort, int apiPort) {
        super();
        this.p2pPort = p2pPort;
        this.apiPort = apiPort;
    }

    public KernelMock getKernelMock() {
        return kernelMock;
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        kernelMock = mockKernel(p2pPort, apiPort);
    }

    private KernelMock mockKernel(int p2pPort, int apiPort) throws IOException {
        // create a new data directory
        FileUtils.copyDirectory(Paths.get(Constants.DEFAULT_DATA_DIR, "config").toFile(),
                Paths.get(getRoot().getAbsolutePath(), "config").toFile());

        Config config = spy(new DevNetConfig(getRoot().getAbsolutePath()));
        when(config.p2pListenPort()).thenReturn(p2pPort);
        when(config.p2pListenIp()).thenReturn("127.0.0.1");
        when(config.p2pDeclaredIp()).thenReturn(Optional.of("127.0.0.1"));
        when(config.apiListenIp()).thenReturn("127.0.0.1");
        when(config.apiListenPort()).thenReturn(apiPort);
        when(config.apiEnabled()).thenReturn(true);
        when(config.apiUsername()).thenReturn("user");
        when(config.apiPassword()).thenReturn("pass");

        // speed up consensus
        when(config.bftNewHeightTimeout()).thenReturn(1000L);
        when(config.bftProposeTimeout()).thenReturn(1000L);
        when(config.bftValidateTimeout()).thenReturn(1000L);
        when(config.bftPreCommitTimeout()).thenReturn(1000L);
        when(config.bftCommitTimeout()).thenReturn(1000L);
        when(config.bftFinalizeTimeout()).thenReturn(1000L);

        Wallet wallet = mockWallet();

        return new KernelMock(config, wallet, wallet.getAccount(0));
    }

    private Wallet mockWallet() throws IOException {
        Wallet wallet = new Wallet(newFile("wallet.data"));
        wallet.unlock("password");
        wallet.addAccount(new EdDSA());
        return wallet;
    }
}
