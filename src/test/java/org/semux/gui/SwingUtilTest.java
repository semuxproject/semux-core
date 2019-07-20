/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semux.core.Amount;

public class SwingUtilTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        reset();
    }

    @After
    public void tearDown() {
        reset();
    }

    private void reset() {
        Locale.setDefault(new Locale("en", "US"));
        SwingUtil.setDefaultUnit("SEM");
        SwingUtil.setDefaultFractionDigits(3);
    }

    @Test
    public void testFormatNumber() {
        BigDecimal x = new BigDecimal("12345678.1234");
        assertEquals("12,345,678", SwingUtil.formatNumber(x, 0));
        assertEquals("12,345,678.12", SwingUtil.formatNumber(x, 2));
    }

    @Test
    public void testParseNumber() throws ParseException {
        assertEquals(new BigDecimal("12345678.12"), SwingUtil.parseNumber("12,345,678.12"));
    }

    @Test
    public void testParseNumberEmpty() throws ParseException {
        expectedException.expect(ParseException.class);
        SwingUtil.parseNumber("");
    }

    @Test
    public void testParseTimestampEmpty() throws ParseException {
        expectedException.expect(ParseException.class);
        SwingUtil.parseTimestamp("");
    }

    @Test
    public void testFormatAndEncodeValue() throws ParseException {
        Amount x = Amount.of(1_234_456_789_000L);
        assertEquals("1,234.456 SEM", SwingUtil.formatAmount(x));
        assertEquals(x, SwingUtil.parseAmount("1,234.456789"));
        assertEquals(x, SwingUtil.parseAmount("1,234.456789 SEM"));
        assertEquals(x, SwingUtil.parseAmount("1,234,456.789 mSEM"));
        assertEquals(x, SwingUtil.parseAmount("1,234,456,789 μSEM"));
    }

    @Test
    public void testFormatValueWithCustomUnit() {
        Amount x = Amount.of(1_234_456_789_123L);
        assertEquals("1,234.456 SEM", SwingUtil.formatAmount(x));
        SwingUtil.setDefaultUnit("mSEM");
        assertEquals("1,234,456.789 mSEM", SwingUtil.formatAmount(x));
        SwingUtil.setDefaultUnit("μSEM");
        assertEquals("1,234,456,789.123 μSEM", SwingUtil.formatAmount(x));
    }

    @Test
    public void testFormatValueWithCustomFractionDigits() {
        Amount x = Amount.of(1_234_456_789_123L);
        SwingUtil.setDefaultUnit("SEM");
        SwingUtil.setDefaultFractionDigits(9);
        assertEquals("1,234.456789123 SEM", SwingUtil.formatAmount(x));
    }

    @Test
    public void testFormatValueFull() {
        Amount x = Amount.of(1_234_456_789_123L);
        SwingUtil.setDefaultFractionDigits(0);
        assertEquals("1,234.456789123 SEM", SwingUtil.formatAmountFull(x));
    }

    @Test
    public void testFormatAndEncodePercentage() throws ParseException {
        double x = 12.3456;
        assertEquals("12.3 %", SwingUtil.formatPercentage(x));
        assertEquals(12.3, SwingUtil.parsePercentage("12.3 %"), 10e-9);
    }

    @Test
    public void testNumberComparator() {
        // String 1 < String 2
        long compareResult1 = SwingUtil.NUMBER_COMPARATOR.compare("1.0000", "2.25");
        assertEquals(-1L, compareResult1, 0L);

        // String 1 > String 2
        long compareResult2 = SwingUtil.NUMBER_COMPARATOR.compare("2.25", "1.0000");
        assertEquals(1L, compareResult2, 0L);

        // String 1 == String 2
        long compareResult3 = SwingUtil.NUMBER_COMPARATOR.compare("1.0000", "1.0000");
        assertEquals(0L, compareResult3, 0L);
    }

    @Test(expected = NumberFormatException.class)
    public void testNumberComparatorExceptionPartiallyWrongInput() {
        SwingUtil.NUMBER_COMPARATOR.compare("1.0000", "2f.25test");
    }

    @Test(expected = NumberFormatException.class)
    public void testNumberComparatorExceptionTotallyWrongInput() {
        SwingUtil.NUMBER_COMPARATOR.compare("1.0000", "worstWord");
    }

}
