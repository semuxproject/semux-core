package org.semux.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CommandTest {

    @Test
    public void testParseCommand() {
        assertNull(Command.of("not_exists"));
        assertEquals(Command.ADD_NODE, Command.of("add_node"));
    }
}
