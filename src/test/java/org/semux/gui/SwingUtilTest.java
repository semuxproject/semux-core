package org.semux.gui;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class SwingUtilTest {

    private String goodDoubleString1 = "1.0000";
    private String goodDoubleString2 = "2.25";

    private String badDoubleString1 = "2f.25test";
    private String badDoubleString2 = "worstWordt";

    @Test
    public void testNumberComparator() {
        Locale.setDefault(new Locale("us", "US"));

        // String 1 < String 2
        long compareResult1 = SwingUtil.NUMBER_COMPARATOR.compare(goodDoubleString1, goodDoubleString2);
        assertEquals(-1L, compareResult1, 0L);

        // String 1 > String 2
        long compareResult2 = SwingUtil.NUMBER_COMPARATOR.compare(goodDoubleString2, goodDoubleString1);
        assertEquals(1L, compareResult2, 0L);

        // String 1 == String 2
        long compareResult3 = SwingUtil.NUMBER_COMPARATOR.compare(goodDoubleString1, goodDoubleString1);
        assertEquals(0L, compareResult3, 0L);
    }

    @Test(expected = NumberFormatException.class)
    public void testNumberComparatorExceptionPartiallyWrongInput() {
        SwingUtil.NUMBER_COMPARATOR.compare(goodDoubleString1, badDoubleString1);
    }

    @Test(expected = NumberFormatException.class)
    public void testNumberComparatorExceptionTotallyWrongInput() {
        SwingUtil.NUMBER_COMPARATOR.compare(goodDoubleString1, badDoubleString2);
    }

}
