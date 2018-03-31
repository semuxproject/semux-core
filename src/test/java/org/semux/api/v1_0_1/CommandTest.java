/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_0_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @deprecated
 */
public class CommandTest {

    @Test
    public void testParseCommand101() {
        assertNull(Command.of("not_exists"));
        assertEquals(Command.ADD_NODE, Command.of("add_node"));
    }
}
