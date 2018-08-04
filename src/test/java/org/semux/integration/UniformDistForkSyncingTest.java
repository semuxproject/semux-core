/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.IntegrationTest;
import org.semux.config.Config;
import org.semux.consensus.ValidatorActivatedFork;
import org.semux.core.Genesis;
import org.semux.net.NodeManager;

/**
 * The test ensures that a client that disabled the fork can still accept blocks
 * from validators who activated the fork.
 */
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class, ValidatorActivatedFork.class })
public class UniformDistForkSyncingTest extends SyncingTest {

    @Override
    protected int targetHeight() {
        // it needs more blocks to cover the case where validators disagree with each
        // other
        return 5;
    }

    @Override
    public void beforeStart() {
        super.beforeStart();

        // forcibly activate the fork
        ValidatorActivatedFork fork = mock(ValidatorActivatedFork.class);
        setInternalState(fork, "number", (short) 1);
        setInternalState(fork, "name", "UNIFORM_DISTRIBUTION");
        setInternalState(fork, "activationBlocks", 0);
        setInternalState(fork, "activationBlocksLookup", 0);
        setInternalState(ValidatorActivatedFork.class, "UNIFORM_DISTRIBUTION", fork);

        // disable the fork on kernel3 (validator)
        Config config3 = kernelRule3.getKernel().getConfig();
        setInternalState(config3, "forkUniformDistributionEnabled", false);
        kernelRule3.getKernel().setConfig(config3);

        // disable the fork on kernel4 (client)
        Config config4 = kernelRule4.getKernel().getConfig();
        setInternalState(config4, "forkUniformDistributionEnabled", false);
        kernelRule4.getKernel().setConfig(config4);
    }
}
