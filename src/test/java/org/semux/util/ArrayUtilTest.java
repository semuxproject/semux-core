package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.TreeSet;

import org.junit.Test;
import org.semux.util.ArrayUtil;

public class ArrayUtilTest {

    @Test
    public void testPermutation() {
        int n = 100;
        int[] arr = ArrayUtil.permutation(n);

        TreeSet<Integer> set = new TreeSet<>();
        for (int i : arr) {
            set.add(i);
        }

        assertEquals(0, set.first().intValue());
        assertEquals(n - 1, set.last().intValue());
    }

    @Test
    public void testShuffle() {
        int n = 100;

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }

        int[] arr2 = arr.clone();
        ArrayUtil.shuffle(arr2);
        assertFalse(Arrays.equals(arr, arr2));
    }
}
