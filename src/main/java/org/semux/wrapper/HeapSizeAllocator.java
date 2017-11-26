/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import oshi.SystemInfo;

public class HeapSizeAllocator {

    public Long getDynamicHeapAllocationInMB() {
        SystemInfo systemInfo = new SystemInfo();
        return (long) ((double) systemInfo.getHardware().getMemory().getTotal() / 1024 / 1024 * 0.8);
    }
}
