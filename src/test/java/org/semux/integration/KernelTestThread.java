/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import org.semux.KernelMock;

public class KernelTestThread extends Thread {

    public KernelMock kernel;

    public KernelTestThread(KernelMock kernel) {
        this.kernel = kernel;
    }

    @Override
    public void run() {
        super.run();
        this.kernel.start();
    }
}
