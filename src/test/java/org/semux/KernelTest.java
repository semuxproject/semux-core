/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import static org.awaitility.Awaitility.await;

import org.junit.Rule;
import org.junit.Test;
import org.semux.Kernel.State;
import org.semux.rules.KernelRule;

public class KernelTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(15160, 15170);

    @Test
    public void testStart() {
        Kernel kernel = kernelRule.getKernel();

        // start kernel
        kernel.start();
        await().until(() -> kernel.getNodeManager().isRunning() //
                && kernel.getPendingManager().isRunning() //
                && kernel.getApi().isRunning() //
                && kernel.getP2p().isRunning() //
                && kernel.getConsensus().isRunning() //
                && !kernel.getSyncManager().isRunning());

        // stop kernel
        kernel.stop();
        await().until(() -> kernel.state == State.STOPPED//
                && !kernel.getNodeManager().isRunning() //
                && !kernel.getPendingManager().isRunning() //
                && !kernel.getApi().isRunning() //
                && !kernel.getP2p().isRunning() //
                && !kernel.getConsensus().isRunning() //
                && !kernel.getSyncManager().isRunning());
    }
}
