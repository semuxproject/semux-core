/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import static org.junit.Assert.assertNotNull;

import java.util.MissingResourceException;

import org.junit.Test;

public class GUIMessageTest {

    @Test
    public void testExists() {
        assertNotNull(GUIMessages.get("Address"));
    }

    @Test(expected = MissingResourceException.class)
    public void testNotExists() {
        assertNotNull(GUIMessages.get("NotExist"));
    }
}
