/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayUtil {

    private ArrayUtil() {
    }

    /**
     * Generate a random permutation of [0...n)
     * 
     * @param n
     * @return
     */
    public static int[] permutation(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }

        shuffle(arr);
        return arr;
    }

    /**
     * Shuffle an integer array.
     * 
     * @param arr
     */
    public static void shuffle(int[] arr) {
        Random r = ThreadLocalRandom.current();

        for (int i = arr.length - 1; i > 0; i--) {
            int index = r.nextInt(i + 1);

            int tmp = arr[index];
            arr[index] = arr[i];
            arr[i] = tmp;
        }
    }
}
