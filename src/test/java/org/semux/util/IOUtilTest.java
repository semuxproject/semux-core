/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Test;

public class IOUtilTest {

    private static final Charset CHARSET = UTF_8;

    private File f1 = new File("test1");
    private File f2 = new File("test2");

    @Test
    public void testWriteToFileExists() throws IOException {
        IOUtil.writeToFile(Bytes.of("123"), f1);
        assertEquals("123", Bytes.toString(IOUtil.readFile(f1)));

        IOUtil.writeToFile(Bytes.of("456"), f1);
        assertEquals("456", IOUtil.readFileAsString(f1));
    }

    @Test
    public void testCopyFile() throws IOException {
        IOUtil.writeToFile(Bytes.of("123"), f1);
        f2.createNewFile();

        IOUtil.copyFile(f1, f2, false);
        assertEquals("", IOUtil.readFileAsString(f2));

        IOUtil.copyFile(f1, f2, true);
        assertEquals("123", IOUtil.readFileAsString(f2));
    }

    @Test
    public void testReadFileAsLines() throws IOException {
        assertTrue(IOUtil.readFileAsLines(f1, CHARSET).isEmpty());

        IOUtil.writeToFile(Bytes.of("123\n456\n"), f1);
        assertThat(IOUtil.readFileAsLines(f1, CHARSET), contains("123", "456"));

        IOUtil.writeToFile(Bytes.of("123\n456\n\n"), f1);
        assertThat(IOUtil.readFileAsLines(f1, CHARSET), contains("123", "456", ""));
    }

    @Test
    public void testReadFile() throws IOException {
        assertThat(IOUtil.readFile(f1), equalTo(new byte[0]));

        IOUtil.writeToFile(Bytes.of("abc"), f1);
        assertThat(IOUtil.readFile(f1), equalTo(new byte[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testReadStreamAsString() throws IOException {
        IOUtil.writeToFile("abc", f1);

        FileInputStream in = new FileInputStream(f1);
        assertThat(IOUtil.readStreamAsString(in), equalTo("abc"));
        in.close();
    }

    @After
    public void deleteFiles() {
        f1.delete();
        f2.delete();
    }
}
