package org.semux.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

public class IOUtilTest {

    private File f = new File("test");

    @Test
    public void testWriteToFileExists() throws IOException {
        IOUtil.writeToFile(Bytes.of("123"), f);
        assertEquals("123", Bytes.toString(IOUtil.readFile(f)));

        IOUtil.writeToFile(Bytes.of("456"), f);
        assertEquals("456", IOUtil.readFileAsString(f));
    }

    @After
    public void teardown() {
        f.delete();
    }
}
