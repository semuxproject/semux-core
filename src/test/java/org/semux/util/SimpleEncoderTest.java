/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SimpleEncoderTest {

    @Test
    public void testToAppend() {
        byte[] append = Bytes.of("hello");

        SimpleEncoder enc = new SimpleEncoder(append);
        enc.writeString("s");

        assertThat(enc.toBytes(), equalTo(Bytes.merge(append, Bytes.of((byte) 1), Bytes.of("s"))));
    }
}
