/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnitTest {

    @Test
    public void testUnit() {
        assertEquals(1, Unit.NANO_SEM);
        assertEquals(1000 * Unit.NANO_SEM, Unit.MICRO_SEM);
        assertEquals(1000 * Unit.MICRO_SEM, Unit.MILLI_SEM);
        assertEquals(1000 * Unit.MILLI_SEM, Unit.SEM);
        assertEquals(1000 * Unit.SEM, Unit.KILO_SEM);
        assertEquals(1000 * Unit.KILO_SEM, Unit.MEGA_SEM);
    }

}
