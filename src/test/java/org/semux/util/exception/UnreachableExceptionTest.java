/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.util.exception.UnreachableException;

public class UnreachableExceptionTest {

    @Test
    public void testUnreachableException() {
        assertTrue(new UnreachableException() instanceof RuntimeException);
    }
}
