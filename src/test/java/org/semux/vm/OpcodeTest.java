/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OpcodeTest {

    @Test
    public void testQuery() {
        for (Opcode code : Opcode.values()) {
            assertEquals(code, Opcode.valueOf(code.name()));
            assertEquals(code, Opcode.of(code.getCode()));
        }
        assertNull(Opcode.of(0xff));
    }
}
