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
 * from validators who activated the forkm.
 */
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class, ValidatorActivatedFork.class })
public class UniformDistributionForkCompatibilityTest extends SyncingTest {

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

        // disable the fork on kernel4
        Config config = kernelRule4.getKernel().getConfig();
        setInternalState(config, "forkUniformDistributionEnabled", false);
        kernelRule4.getKernel().setConfig(config);
    }
}
