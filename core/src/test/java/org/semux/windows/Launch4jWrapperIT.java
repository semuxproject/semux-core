/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.windows;

import static org.awaitility.Awaitility.await;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.semux.util.SystemUtil;
import org.semux.util.SystemUtil.OsName;

@Category(WindowsIntegrationTest.class)
public class Launch4jWrapperIT {

    private Process l4jWrapper;

    private Path l4jLogPath;

    @Before
    public void setUp() throws IOException {
        Path prefix = Paths.get(System.getProperty("user.dir"), "target");
        l4jLogPath = Paths.get(prefix.toString(), "launch4j.log");
        FileUtils.deleteQuietly(l4jLogPath.toFile());
        Path semuxExePath = Paths.get(prefix.toString(), "semux.exe");
        ProcessBuilder processBuilder = new ProcessBuilder(semuxExePath.toString(), "").directory(prefix.toFile());
        processBuilder.environment().put("Launch4j", "debug");
        l4jWrapper = processBuilder.start();
    }

    @After
    public void tearDown() {
        l4jWrapper.destroyForcibly();
    }

    @Test
    public void testLaunch4jWrapper() {
        assumeTrue(SystemUtil.getOsName() == OsName.WINDOWS);

        await().until(() -> l4jLogPath.toFile().exists());
        await().until(() -> Files.lines(l4jLogPath).anyMatch(str -> str.matches("Exit code:[\\t\\s]*259")));
    }

}
