/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileUtilTest {

    private static final File dir = new File("test");

    @BeforeClass
    public static void setup() throws IOException {
        dir.mkdirs();
        new File(dir, "file").createNewFile();
    }

    @Test
    public void testRecursiveDelete() {
        FileUtil.recursiveDelete(dir);
        assertFalse(dir.exists());
    }

    @AfterClass
    public static void teardown() {
        dir.delete();
    }
}
