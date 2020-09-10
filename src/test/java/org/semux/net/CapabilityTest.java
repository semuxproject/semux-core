/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CapabilityTest {

    @Test
    public void testIsSupported() {
        assertFalse(CapabilityTreeSet.emptyList().isSupported(Capability.SEMUX));
        assertFalse(CapabilityTreeSet.of("SEMUX").isSupported(Capability.FAST_SYNC));
        assertTrue(CapabilityTreeSet.of("SEMUX").isSupported(Capability.SEMUX));
        assertTrue(CapabilityTreeSet.of(Capability.SEMUX).isSupported(Capability.SEMUX));
        assertEquals(CapabilityTreeSet.of(Capability.SEMUX), CapabilityTreeSet.of(Capability.SEMUX));
    }
}
