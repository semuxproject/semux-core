/**
 * Copyright (c) 2017-2018 The Semux Developers
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
        assertFalse(CapabilitySet.emptySet().isSupported(Capability.SEMUX));
        assertFalse(CapabilitySet.of("SEMUX").isSupported(Capability.LIGHT));
        assertTrue(CapabilitySet.of("SEMUX").isSupported(Capability.SEMUX));
        assertTrue(CapabilitySet.of(Capability.SEMUX).isSupported(Capability.SEMUX));
        assertEquals(CapabilitySet.of(Capability.SEMUX), CapabilitySet.of(Capability.SEMUX));
    }
}
