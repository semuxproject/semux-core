/*
 * Copyright (c) [2018] [ The Semux Developers ]
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.ethereum.vm.util.HexUtil;
import org.junit.Test;

public class DataWordTest {

    @Test
    public void testAdd() {
        byte[] three = new byte[32];
        for (int i = 0; i < three.length; i++) {
            three[i] = (byte) 0xff;
        }

        DataWord x = new DataWord(three);
        x.add(new DataWord(three));
        assertEquals(32, x.getData().length);
    }

    @Test
    public void testMod() {
        String expected = "000000000000000000000000000000000000000000000000000000000000001a";

        byte[] one = new byte[32];
        one[31] = 0x1e; // 0x000000000000000000000000000000000000000000000000000000000000001e

        byte[] two = new byte[32];
        for (int i = 0; i < two.length; i++) {
            two[i] = (byte) 0xff;
        }
        two[31] = 0x56; // 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff56

        DataWord x = new DataWord(one);// System.out.println(x.value());
        DataWord y = new DataWord(two);// System.out.println(y.value());
        DataWord z = y.mod(x);
        assertEquals(32, z.getData().length);
        assertEquals(expected, HexUtil.toHexString(z.getData()));
    }

    @Test
    public void testMul() {
        byte[] one = new byte[32];
        one[31] = 0x1; // 0x0000000000000000000000000000000000000000000000000000000000000001

        byte[] two = new byte[32];
        two[11] = 0x1; // 0x0000000000000000000000010000000000000000000000000000000000000000

        DataWord x = new DataWord(one);// System.out.println(x.value());
        DataWord y = new DataWord(two);// System.out.println(y.value());
        DataWord z = x.mul(y);
        assertEquals(32, z.getData().length);
        assertEquals("0000000000000000000000010000000000000000000000000000000000000000",
                HexUtil.toHexString(z.getData()));
    }

    @Test
    public void testMulOverflow() {

        byte[] one = new byte[32];
        one[30] = 0x1;

        byte[] two = new byte[32];
        two[0] = 0x1;

        DataWord x = new DataWord(one);
        System.out.println(x);
        DataWord y = new DataWord(two);
        System.out.println(y);
        DataWord z = x.mul(y);
        System.out.println(z);

        assertEquals(32, z.getData().length);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
                HexUtil.toHexString(z.getData()));
    }

    @Test
    public void testDiv() {
        byte[] one = new byte[32];
        one[30] = 0x01;
        one[31] = 0x2c; // 0x000000000000000000000000000000000000000000000000000000000000012c

        byte[] two = new byte[32];
        two[31] = 0x0f; // 0x000000000000000000000000000000000000000000000000000000000000000f

        DataWord x = new DataWord(one);
        DataWord y = new DataWord(two);
        DataWord z = x.div(y);

        assertEquals(32, z.getData().length);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000014",
                HexUtil.toHexString(z.getData()));
    }

    @Test
    public void testDivZero() {
        byte[] one = new byte[32];
        one[30] = 0x05; // 0x0000000000000000000000000000000000000000000000000000000000000500

        byte[] two = new byte[32];

        DataWord x = new DataWord(one);
        DataWord y = new DataWord(two);
        DataWord z = x.div(y);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    @Test
    public void testSDivNegative() {

        // one is -300 as 256-bit signed integer:
        byte[] one = HexUtil.fromHexString("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed4");

        byte[] two = new byte[32];
        two[31] = 0x0f;

        DataWord x = new DataWord(one);
        DataWord y = new DataWord(two);
        DataWord z = x.sDiv(y);

        assertEquals(32, z.getData().length);
        assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffec", z.toString());
    }

    @Test
    public void testSignExtend1() {

        DataWord x = new DataWord(HexUtil.fromHexString("f2"));
        byte k = 0;
        String expected = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff2";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend2() {
        DataWord x = new DataWord(HexUtil.fromHexString("f2"));
        byte k = 1;
        String expected = "00000000000000000000000000000000000000000000000000000000000000f2";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend3() {

        byte k = 1;
        DataWord x = new DataWord(HexUtil.fromHexString("0f00ab"));
        String expected = "00000000000000000000000000000000000000000000000000000000000000ab";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend4() {

        byte k = 1;
        DataWord x = new DataWord(HexUtil.fromHexString("ffff"));
        String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend5() {

        byte k = 3;
        DataWord x = new DataWord(HexUtil.fromHexString("ffffffff"));
        String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend6() {

        byte k = 3;
        DataWord x = new DataWord(HexUtil.fromHexString("ab02345678"));
        String expected = "0000000000000000000000000000000000000000000000000000000002345678";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend7() {

        byte k = 3;
        DataWord x = new DataWord(HexUtil.fromHexString("ab82345678"));
        String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff82345678";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test
    public void testSignExtend8() {

        byte k = 30;
        DataWord x = new DataWord(
                HexUtil.fromHexString("ff34567882345678823456788234567882345678823456788234567882345678"));
        String expected = "0034567882345678823456788234567882345678823456788234567882345678";

        DataWord z = x.signExtend(k);
        System.out.println(z.toString());
        assertEquals(expected, z.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSignExtendException1() {

        byte k = -1;
        DataWord x = new DataWord(0);

        x.signExtend(k); // should throw an exception
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSignExtendException2() {

        byte k = 32;
        DataWord x = new DataWord(0);

        x.signExtend(k); // should throw an exception
    }

    @Test
    public void testAddModOverflow() {
        testAddMod("9999999999999999999999999999999999999999999999999999999999999999",
                "8888888888888888888888888888888888888888888888888888888888888888",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        testAddMod("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    }

    void testAddMod(String v1, String v2, String v3) {
        DataWord dv1 = new DataWord(HexUtil.fromHexString(v1));
        DataWord dv2 = new DataWord(HexUtil.fromHexString(v2));
        DataWord dv3 = new DataWord(HexUtil.fromHexString(v3));
        BigInteger bv1 = new BigInteger(v1, 16);
        BigInteger bv2 = new BigInteger(v2, 16);
        BigInteger bv3 = new BigInteger(v3, 16);

        DataWord z = dv1.addmod(dv2, dv3);
        BigInteger br = bv1.add(bv2).mod(bv3);
        assertEquals(z.value(), br);
    }

    @Test
    public void testMulMod1() {
        DataWord wr = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));
        DataWord w1 = new DataWord(HexUtil.fromHexString("01"));
        DataWord w2 = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999998"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                HexUtil.toHexString(z.getData()));
    }

    @Test
    public void testMulMod2() {
        DataWord wr = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));
        DataWord w1 = new DataWord(HexUtil.fromHexString("01"));
        DataWord w2 = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    @Test
    public void testMulModZero() {
        DataWord wr = new DataWord(HexUtil.fromHexString("00"));
        DataWord w1 = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));
        DataWord w2 = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    @Test
    public void testMulModZeroWord1() {
        DataWord wr = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));
        DataWord w1 = new DataWord(HexUtil.fromHexString("00"));
        DataWord w2 = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    @Test
    public void testMulModZeroWord2() {
        DataWord wr = new DataWord(
                HexUtil.fromHexString("9999999999999999999999999999999999999999999999999999999999999999"));
        DataWord w1 = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        DataWord w2 = new DataWord(HexUtil.fromHexString("00"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    @Test
    public void testMulModOverflow() {
        DataWord wr = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        DataWord w1 = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        DataWord w2 = new DataWord(
                HexUtil.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        DataWord z = wr.mulmod(w1, w2);

        assertEquals(32, z.getData().length);
        assertTrue(z.isZero());
    }

    public static BigInteger pow(BigInteger x, BigInteger y) {
        if (y.compareTo(BigInteger.ZERO) < 0)
            throw new IllegalArgumentException();
        BigInteger z = x; // z will successively become x^2, x^4, x^8, x^16, x^32...
        BigInteger result = BigInteger.ONE;
        byte[] bytes = y.toByteArray();
        for (int i = bytes.length - 1; i >= 0; i--) {
            byte bits = bytes[i];
            for (int j = 0; j < 8; j++) {
                if ((bits & 1) != 0)
                    result = result.multiply(z);
                // short cut out if there are no more bits to handle:
                if ((bits >>= 1) == 0 && i == 0)
                    return result;
                z = z.multiply(z);
            }
        }
        return result;
    }
}
