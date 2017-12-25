/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.rules;

import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.rules.TemporaryFolder;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.config.DevNetConfig;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.db.DBFactory;
import org.semux.db.LevelDB.LevelDBFactory;
import org.semux.util.Bytes;

/**
 * A kernel rule creates a temporary folder as the data directory. Ten accounts
 * will be created automatically and the first one will be used as coinbase.
 */
public class KernelRule extends TemporaryFolder {

    private int p2pPort;
    private int apiPort;

    private String password;
    private KernelMock kernel;

    public KernelRule(int p2pPort, int apiPort) {
        super();

        this.p2pPort = p2pPort;
        this.apiPort = apiPort;
    }

    @Override
    protected void before() throws Throwable {
        create();

        // generate random password
        this.password = Hex.encode(Bytes.random(12));

        // generate kernel mock
        Config config = mockConfig(p2pPort, apiPort);
        Wallet wallet = new Wallet(new File(getRoot(), "wallet.data"));
        wallet.unlock(password);
        for (int i = 0; i < 10; i++) {
            wallet.addAccount(new EdDSA());
        }
        EdDSA coinbase = wallet.getAccount(0);
        this.kernel = new KernelMock(config, wallet, coinbase);
    }

    @Override
    protected void after() {
        delete();
    }

    protected Config mockConfig(int p2pPort, int apiPort) {
        Config config = spy(new DevNetConfig(getRoot().getAbsolutePath()));

        when(config.p2pListenPort()).thenReturn(p2pPort);
        when(config.p2pListenIp()).thenReturn("127.0.0.1");
        when(config.p2pDeclaredIp()).thenReturn(Optional.of("127.0.0.1"));
        when(config.apiListenIp()).thenReturn("127.0.0.1");
        when(config.apiListenPort()).thenReturn(apiPort);
        when(config.apiEnabled()).thenReturn(true);
        when(config.apiUsername()).thenReturn("username");
        when(config.apiPassword()).thenReturn("password");

        return config;
    }

    /**
     * Returns the password.
     * 
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the kernel.
     * 
     * @return
     */
    public KernelMock getKernel() {
        return kernel;
    }

    /**
     * Returns the database factory.
     * 
     * @return
     */
    public DBFactory getDatabaseFactory() {
        return new LevelDBFactory(getRoot());
    }

    /**
     * Speed up the consensus.
     */
    public void speedUpCosnensus() throws IOException {
        Config config = kernel.getConfig();

        // speed up consensus
        when(config.bftNewHeightTimeout()).thenReturn(1000L);
        when(config.bftProposeTimeout()).thenReturn(1000L);
        when(config.bftValidateTimeout()).thenReturn(1000L);
        when(config.bftPreCommitTimeout()).thenReturn(1000L);
        when(config.bftCommitTimeout()).thenReturn(1000L);
        when(config.bftFinalizeTimeout()).thenReturn(1000L);
    }
}
