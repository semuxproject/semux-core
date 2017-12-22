/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.semux.wrapper.Wrapper.MINIMUM_HEAP_SIZE_MB;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.semux.cli.SemuxCLI;
import org.semux.gui.SemuxGUI;
import org.semux.util.SystemUtil;
import org.semux.util.SystemUtil.OsName;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({ ProcessBuilder.class, Process.class, Wrapper.class, SystemUtil.class })
public class WrapperTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays
                .asList(new Object[][] {
                        { new String[] { "--gui", "--jvmoptions", "-Xmx1G -Xms1G" },
                                new String[] { getJavaBinPath(), "-Xmx1G", "-Xms1G", "-cp", "semux.jar",
                                        SemuxGUI.class.getCanonicalName() },
                                null },
                        { new String[] { "--gui" },
                                new String[] { getJavaBinPath(), "-Xmx800M", "-cp", "semux.jar",
                                        SemuxGUI.class.getCanonicalName() },
                                1000L * 1024 * 1024 },
                        { new String[] { "--cli" }, new String[] { getJavaBinPath(), "-Xmx800M", "-cp", "semux.jar",
                                SemuxCLI.class.getCanonicalName() }, 1000L * 1024 * 1024 },
                        { new String[] { "--gui" },
                                new String[] { getJavaBinPath(), String.format("-Xmx%dM", MINIMUM_HEAP_SIZE_MB), "-cp",
                                        "semux.jar",
                                        SemuxGUI.class.getCanonicalName() },
                                MINIMUM_HEAP_SIZE_MB * 1024 * 1024 - 1 },
                        { new String[] { "--cli" },
                                new String[] { getJavaBinPath(), String.format("-Xmx%dM", MINIMUM_HEAP_SIZE_MB), "-cp",
                                        "semux.jar",
                                        SemuxCLI.class.getCanonicalName() },
                                MINIMUM_HEAP_SIZE_MB * 1024 * 1024 - 1 } });
    }

    String[] inputArgs, javaArgs;
    Long mockAvailableMB;

    public WrapperTest(String[] inputArgs, String[] javaArgs, Long mockAvailableBytes) {
        this.inputArgs = inputArgs;
        this.javaArgs = javaArgs;
        this.mockAvailableMB = mockAvailableBytes;
    }

    @Test
    public void testMain() throws Exception {
        assumeTrue(SystemUtil.getOsName() != OsName.WINDOWS);

        // mock ProcessBuilder & Process
        ProcessBuilder processBuilderMock = mock(ProcessBuilder.class);
        Process process = mock(Process.class);
        when(process.exitValue()).thenReturn(0);
        doReturn(process).when(processBuilderMock).start();
        whenNew(ProcessBuilder.class).withNoArguments().thenReturn(processBuilderMock);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        if (mockAvailableMB != null) {
            when(SystemUtil.getAvailableMemorySize()).thenReturn(mockAvailableMB);
        }

        // execution
        Wrapper.main(inputArgs);

        // verify
        verify(processBuilderMock).command(javaArgs);
        verifyStatic(SystemUtil.class);
        SystemUtil.exit(0);
    }

    protected static String getJavaBinPath() {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Paths.get(javaHome, "bin", "java");
        return javaBin.toString();
    }
}
