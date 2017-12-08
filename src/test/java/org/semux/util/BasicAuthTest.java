package org.semux.util;

import static org.junit.Assert.assertEquals;

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
}
