/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CapabilityTest {

    @Test
    public void testIsSupported() {
        assertFalse(CapabilitySet.emptySet().isSupported(Capability.SEM));
        assertFalse(CapabilitySet.of("ETH").isSupported(Capability.SEM));
        assertTrue(CapabilitySet.of("SEM").isSupported(Capability.SEM));
        assertTrue(CapabilitySet.of(Capability.SEM).isSupported(Capability.SEM));
    }
}
