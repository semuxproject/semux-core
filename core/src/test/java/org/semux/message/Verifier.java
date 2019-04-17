/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.semux.util.Bytes;

public class Verifier {

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(Verifier.class.getResourceAsStream("/org/semux/gui/messages.properties"));
        props.load(Verifier.class.getResourceAsStream("/org/semux/cli/messages.properties"));

        Collection<File> files = FileUtils.listFiles(new File("src/main/java/org/semux"), new String[] { "java" },
                true);
        int n = 0;
        for (File file : files) {
            String content = FileUtils.readFileToString(file, Bytes.CHARSET);
            Pattern p = Pattern.compile("Messages.get\\(\"(.+?)\"");
            Matcher m = p.matcher(content);
            while (m.find()) {
                n++;
                if (!props.containsKey(m.group(1))) {
                    System.err.println(m.group(1) + " is not in the messages.properties!");
                }
            }
        }
        System.out.println(n);
    }
}
