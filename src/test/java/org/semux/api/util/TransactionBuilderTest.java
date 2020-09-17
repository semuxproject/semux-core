/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.semux.core.Unit.SEM;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semux.Kernel;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.util.Bytes;

public class TransactionBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testDelegateWithTo() {
        expectedException.expect(IllegalArgumentException.class);
        new TransactionBuilder(mock(Kernel.class)).withType(TransactionType.DELEGATE).withTo("0xabc");
    }

    @Test
    public void testDelegateWithWrongValue() {
        Kernel kernel = mock(Kernel.class, RETURNS_DEEP_STUBS);
        when(kernel.getConfig().spec().minDelegateBurnAmount()).thenReturn(Amount.of(5, SEM));

        Transaction tx = new TransactionBuilder(kernel)
                .withType(TransactionType.DELEGATE)
                .withValue("6")
                .withNonce("7")
                .withFee("8")
                .buildUnsigned(Bytes.random(20));
        assertEquals(Amount.of(6), tx.getValue());
        assertEquals(7L, tx.getNonce());
    }
}
