/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class BasicAuthTest {

    @Test
    public void testAuth() {
        String username = "name";
        String password = "password";
        String auth = BasicAuth.generateAuth(username, password);

        Pair<String, String> p = BasicAuth.parseAuth(auth);
        assertEquals(username, p.getKey());
        assertEquals(password, p.getValue());
    }

    @Test
    public void testInvalid() {
        assertNull(BasicAuth.parseAuth("invalid_auth_string"));
    }

}
