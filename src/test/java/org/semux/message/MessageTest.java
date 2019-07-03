/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTest {

    private static final Logger logger = LoggerFactory.getLogger(MessageTest.class);

    @Test
    public void testMessages() throws IOException {
        Properties props = new Properties();
        props.load(MessageTest.class.getResourceAsStream("/org/semux/gui/messages.properties"));
        props.load(MessageTest.class.getResourceAsStream("/org/semux/cli/messages.properties"));

        Collection<File> files = FileUtils.listFiles(new File("src/main/java/org/semux"), new String[] { "java" },
                true);
        int n = 0;
        for (File file : files) {
            String content = FileUtils.readFileToString(file, Bytes.CHARSET);
            Pattern p = Pattern.compile("Messages.get\\(\"(.+?)\"");
            Matcher m = p.matcher(content);
            while (m.find()) {
                n++;
                assertTrue(props.containsKey(m.group(1)));
            }
        }
        logger.info("Total number of items = {}", n);
    }
}
