package org.semux.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.util.Bytes;

public class CryptoExceptionTest {

    @Test(expected = CryptoException.class)
    public void testCryptoException() throws CryptoException {
        AES.decrypt(Bytes.EMPY_BYTES, Bytes.EMPY_BYTES, Bytes.EMPY_BYTES);
    }

    @Test
    public void testConstructor() {
        String msg = "test";
        Throwable th = new Throwable();
        CryptoException e = new CryptoException(msg, th);
        assertEquals(msg, e.getMessage());
        assertEquals(th, e.getCause());
    }
}
