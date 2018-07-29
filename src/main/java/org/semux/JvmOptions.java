/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.nio.file.Paths;

import org.semux.util.SystemUtil;

public class JvmOptions {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(" -Xmx" + SystemUtil.getAvailableMemorySize() * 75 / 100 / 1024 / 1024 + "m");
        sb.append(" -Dlog4j2.garbagefreeThreadContextMap=true");
        sb.append(" -Dlog4j2.shutdownHookEnabled=false");
        sb.append(" -Dlog4j2.disableJmx=true");
        sb.append(" --add-opens=java.base/sun.net.dns=ALL-UNNAMED");
        sb.append(" --add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        sb.append(" -splash:" + Paths.get("resources", "splash.png").toAbsolutePath());

        System.out.println(sb);
    }
}
