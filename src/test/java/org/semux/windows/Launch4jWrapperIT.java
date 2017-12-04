/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.windows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.semux.util.SystemUtil;
import org.semux.util.SystemUtil.OsName;

@Category(org.semux.windows.WindowsIntegrationTest.class)
public class Launch4jWrapperIT {

    @Test
    public void testLaunch4jWrapper() throws IOException, InterruptedException {
        assumeTrue(SystemUtil.getOsName() == OsName.WINDOWS);

        Path prefix = Paths.get(System.getProperty("user.dir"), "dist", "windows");
        Path semuxExePath = Paths.get(prefix.toString(), "semux.exe");
        Process l4jWrapper = new ProcessBuilder(semuxExePath.toString(), "").directory(prefix.toFile()).start();

        int limit = 5;
        Path l4jLogPath = Paths.get(prefix.toString(), "launch4j.log");
        while (!l4jLogPath.toFile().exists()) {
            TimeUnit.SECONDS.sleep(1);
            if (--limit == 0) {
                assertFalse("launch4j.log is not created", false);
            }
        }

        assertTrue("The exit code of launch4j wrapper should be 0",
                Files.lines(l4jLogPath).anyMatch(str -> str.matches("Exit code:\\s+0")));

        l4jWrapper.destroyForcibly();
    }

}
