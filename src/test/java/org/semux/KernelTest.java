/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import static org.awaitility.Awaitility.await;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.semux.integration.KernelTestRule;

public class KernelTest {

    @Rule
    public KernelTestRule kernelRule = new KernelTestRule(15160, 15170);

    public KernelTest() throws IOException {
    }

    @Test
    public void testStart() {
        Kernel kernel = kernelRule.getKernelMock();

        // start kernel
        kernel.start();
        await().until(() -> kernel.getNodeManager().isRunning() &&
                kernel.getPendingManager().isRunning() &&
                kernel.getApi().isRunning() &&
                kernel.getP2p().isRunning() &&
                kernel.getConsensus().isRunning() &&
                !kernel.getSyncManager().isRunning());

        // stop kernel
        kernel.stop();
        await().until(() -> !kernel.isRunning() //
                && !kernel.getNodeManager().isRunning() //
                && !kernel.getPendingManager().isRunning() //
                && !kernel.getApi().isRunning() //
                && !kernel.getP2p().isRunning() //
                && !kernel.getConsensus().isRunning() //
                && !kernel.getSyncManager().isRunning());
    }
}
