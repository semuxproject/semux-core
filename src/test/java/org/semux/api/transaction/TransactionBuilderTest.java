/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.transaction;

import static org.mockito.Mockito.mock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semux.Kernel;
import org.semux.core.TransactionType;

public class TransactionBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testDelegateWithTo() {
        expectedException.expect(IllegalArgumentException.class);
        new TransactionBuilder(mock(Kernel.class)).withType(TransactionType.DELEGATE).withTo("0xabc");
    }

    @Test
    public void testDelegateWithValue() {
        expectedException.expect(IllegalArgumentException.class);
        new TransactionBuilder(mock(Kernel.class)).withType(TransactionType.DELEGATE).withValue("10");
    }
}
