/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HeapSizeAllocatorTest {

    @Test
    public void testGetDynamicHeapAllocationInMB() {
        HeapSizeAllocator heapSizeAllocator = new HeapSizeAllocator();
        assertTrue(heapSizeAllocator.getDynamicHeapAllocationInMB() > 0);
    }
}
