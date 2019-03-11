/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static org.mockito.Mockito.mock;

import org.junit.experimental.categories.Category;
import org.semux.IntegrationTest;
import org.semux.TestUtils;
import org.semux.config.AbstractConfig;
import org.semux.config.Config;
import org.semux.core.Fork;

/**
 * The test ensures that a client that disabled the fork can still accept blocks
 * from validators who activated the fork.
 */
@Category(IntegrationTest.class)
public class UniformDistForkSyncingTest extends SyncingTest {

    @Override
    protected int targetHeight() {
        // it needs more blocks to cover the case where validators disagree with each
        // other
        return 5;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // forcibly activate the fork
        Fork fork = mock(Fork.class);
        TestUtils.setInternalState(fork, "number", (short) 1, Fork.class);
        TestUtils.setInternalState(fork, "name", "UNIFORM_DISTRIBUTION", Fork.class);
        TestUtils.setInternalState(fork, "activationBlocks", 0, Fork.class);
        TestUtils.setInternalState(fork, "activationBlocksLookup", 0, Fork.class);
        TestUtils.setInternalState(Fork.UNIFORM_DISTRIBUTION, "number", fork.number, Fork.class);
        TestUtils.setInternalState(Fork.UNIFORM_DISTRIBUTION, "name", fork.name, Fork.class);
        TestUtils.setInternalState(Fork.UNIFORM_DISTRIBUTION, "activationBlocks", fork.activationBlocks, Fork.class);
        TestUtils.setInternalState(Fork.UNIFORM_DISTRIBUTION, "activationBlocksLookup", fork.activationBlocksLookup,
                Fork.class);
        TestUtils.setInternalState(Fork.UNIFORM_DISTRIBUTION, "activationDeadline", fork.activationDeadline,
                Fork.class);

        // disable the fork on kernel3 (validator)
        Config config3 = kernelRule3.getKernel().getConfig();
        TestUtils.setInternalState(config3, "forkUniformDistributionEnabled", false, AbstractConfig.class);
        kernelRule3.getKernel().setConfig(config3);

        // disable the fork on kernel4 (client)
        Config config4 = kernelRule4.getKernel().getConfig();
        TestUtils.setInternalState(config4, "forkUniformDistributionEnabled", false, AbstractConfig.class);
        kernelRule4.getKernel().setConfig(config4);
    }
}
