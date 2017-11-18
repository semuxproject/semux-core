/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
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
