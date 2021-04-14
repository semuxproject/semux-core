/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.semux.util.NullPrintStream;
import org.semux.util.SystemUtil;

public class JvmOptions {

    public static void main(String[] args) throws UnsupportedEncodingException {
        // System.out is replaced with a byte buffer to prohibit any outputs from other
        // places. This is a work-around for ISSUE-119
        PrintStream out = System.out;
        System.setOut(new NullPrintStream());

        StringBuilder sb = new StringBuilder();
        sb.append(" -Xmx").append(SystemUtil.getAvailableMemorySize() * 75 / 100 / 1024 / 1024).append("m");
        sb.append(" -Dlog4j2.garbagefreeThreadContextMap=true");
        sb.append(" -Dlog4j2.shutdownHookEnabled=false");
        sb.append(" -Dlog4j2.disableJmx=true");
        sb.append(" -Dfile.encoding=UTF-8");
        if (SystemUtil.isJavaPlatformModuleSystemAvailable()) {
            sb.append(" --add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        }
        if (Arrays.asList(args).contains("--gui")) {
            sb.append(" -splash:./resources/splash.png");
        }

        out.println(sb);
    }
}
