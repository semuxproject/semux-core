/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32.util;

public class BitUtil {

    /**
     * Checks bit value from the left, 1 based
     *
     * @param data
     *            data
     * @param index
     *            index to check
     * @return true if set
     */
    public static boolean checkBit(byte data, int index) {
        byte bit = (byte) ((data >> (8 - index)) & 1);
        return bit == 0x1;
    }

    /**
     * Set a bit of a byte
     *
     * @param data
     *            data
     * @param index
     *            index to set
     * @return byte with bit set
     */
    public static byte setBit(byte data, int index) {
        data |= 1 << (8 - index);
        return data;
    }

    /**
     * Unset a bit of a byte
     *
     * @param data
     *            data
     * @param index
     *            index to clear
     * @return byte with bit unset
     */
    public static byte unsetBit(byte data, int index) {
        data &= ~(1 << (8 - index));
        return data;
    }
}
