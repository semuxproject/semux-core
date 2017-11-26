/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import org.junit.Test;
import org.semux.util.SystemUtil;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class HeapSizeAllocatorTest {

    @Test
    public void testGetDynamicHeapAllocationInMB() {
        assumeTrue(SystemUtil.getOS() != SystemUtil.OS.WINDOWS);

        HeapSizeAllocator heapSizeAllocator = new HeapSizeAllocator();
        assertTrue(heapSizeAllocator.getDynamicHeapAllocationInMB() > 0);
    }
}
