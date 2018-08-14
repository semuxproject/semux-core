package org.ethereum.vm;

import java.util.Arrays;

public class TestUtil {

    public static byte[] address(int n) {
        byte[] a = new byte[20];
        Arrays.fill(a, (byte) n);
        return a;
    }
}
