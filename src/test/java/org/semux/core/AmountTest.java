/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.semux.core.Amount.ZERO;
import static org.semux.core.Amount.neg;
import static org.semux.core.Amount.sub;
import static org.semux.core.Amount.sum;
import static org.semux.core.Amount.Unit.KILO_SEM;
import static org.semux.core.Amount.Unit.MEGA_SEM;
import static org.semux.core.Amount.Unit.MICRO_SEM;
import static org.semux.core.Amount.Unit.MILLI_SEM;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.semux.core.Amount.Unit;

public class AmountTest {

    @Test
    public void testUnits() {
        assertEquals(ZERO, ZERO);
        assertEquals(NANO_SEM.of(1), NANO_SEM.of(1));
        assertEquals(NANO_SEM.of(1000), MICRO_SEM.of(1));
        assertEquals(MICRO_SEM.of(1000), MILLI_SEM.of(1));
        assertEquals(MILLI_SEM.of(1000), SEM.of(1));
        assertEquals(SEM.of(1000), KILO_SEM.of(1));
        assertEquals(KILO_SEM.of(1000), MEGA_SEM.of(1));
    }

    @Test
    public void testFromDecimal() {
        assertEquals(ZERO, SEM.fromDecimal(BigDecimal.ZERO));
        assertEquals(SEM.of(10), SEM.fromDecimal(new BigDecimal("10.000")));
        assertEquals(SEM.of(1000), KILO_SEM.fromDecimal(BigDecimal.ONE));
        assertEquals(MILLI_SEM.of(1), SEM.fromDecimal(new BigDecimal("0.001")));
    }

    @Test(expected = NoSuchElementException.class)
    public void testOfSymbol() {
        assertEquals(NANO_SEM, Unit.ofSymbol("nSEM"));
        assertEquals(MICRO_SEM, Unit.ofSymbol("μSEM"));
        assertEquals(MILLI_SEM, Unit.ofSymbol("mSEM"));
        assertEquals(SEM, Unit.ofSymbol("SEM"));
        assertEquals(KILO_SEM, Unit.ofSymbol("kSEM"));
        assertEquals(MEGA_SEM, Unit.ofSymbol("MSEM"));

        Unit.ofSymbol("???");
    }

    @Test
    public void testToDecimal() {
        assertEquals(new BigDecimal("0"), SEM.toDecimal(ZERO, 0));
        assertEquals(new BigDecimal("0.000"), SEM.toDecimal(ZERO, 3));

        Amount oneSem = SEM.of(1);
        assertEquals(new BigDecimal("1.000"), SEM.toDecimal(oneSem, 3));
        assertEquals(new BigDecimal("1000.000"), MILLI_SEM.toDecimal(oneSem, 3));
        assertEquals(new BigDecimal("0.001000"), KILO_SEM.toDecimal(oneSem, 6));
    }

    @Test
    public void testCompareTo() {
        assertEquals(ZERO.compareTo(ZERO), 0);
        assertEquals(NANO_SEM.of(1000).compareTo(MICRO_SEM.of(1)), 0);

        assertEquals(NANO_SEM.of(10).compareTo(NANO_SEM.of(10)), 0);
        assertEquals(NANO_SEM.of(5).compareTo(NANO_SEM.of(10)), -1);
        assertEquals(NANO_SEM.of(10).compareTo(NANO_SEM.of(5)), 1);
    }

    @Test
    public void testHashCode() {
        assertEquals(ZERO.hashCode(), SEM.of(0).hashCode());
        assertEquals(SEM.of(999).hashCode(), SEM.of(999).hashCode());
        assertEquals(SEM.of(1000).hashCode(), KILO_SEM.of(1).hashCode());
        assertNotEquals(SEM.of(1).hashCode(), KILO_SEM.of(1).hashCode());
    }

    @Test
    public void testGtLtEtc() {
        assertTrue(SEM.of(19).gt0());
        assertTrue(SEM.of(-9).lt0());
        assertFalse(ZERO.gt0());
        assertFalse(ZERO.lt0());

        assertTrue(ZERO.gte0());
        assertTrue(ZERO.lte0());
        assertFalse(SEM.of(-9).gte0());
        assertFalse(SEM.of(99).lte0());

        assertTrue(SEM.of(999).gt(MILLI_SEM.of(999)));
        assertTrue(SEM.of(999).gte(MILLI_SEM.of(999)));
        assertFalse(SEM.of(999).lt(MILLI_SEM.of(999)));
        assertFalse(SEM.of(999).lte(MILLI_SEM.of(999)));
    }

    @Test
    public void testMath() {
        assertEquals(sum(SEM.of(1000), KILO_SEM.of(1)), KILO_SEM.of(2));
        assertEquals(sub(SEM.of(1000), KILO_SEM.of(1)), ZERO);
        assertEquals(neg(SEM.of(1000)), KILO_SEM.of(-1));
        assertEquals(neg(ZERO), ZERO);
    }
}
