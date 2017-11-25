/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.semux.cli.SemuxCLI;
import org.semux.gui.SemuxGUI;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({ ProcessBuilder.class, Wrapper.class })
public class WrapperTest {

    @Rule
    public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new String[] { "--gui", "--jvm-options", "-Xmx1G -Xms1G" },
                        new String[] { getJavaBinPath(), "-Xmx1G", "-Xms1G", "-cp", "semux.jar",
                                SemuxGUI.class.getCanonicalName() },
                        null },
                { new String[] { "--gui" },
                        new String[] { getJavaBinPath(), "-Xmx100M", "-cp", "semux.jar",
                                SemuxGUI.class.getCanonicalName() },
                        100 },
                { new String[] { "--cli" }, new String[] { getJavaBinPath(), "-Xmx100M", "-cp", "semux.jar",
                        SemuxCLI.class.getCanonicalName() }, 100 } });
    }

    String[] inputArgs, javaArgs;
    Integer mockAvailableMB;

    public WrapperTest(String[] inputArgs, String[] javaArgs, Integer mockAvailableMB) {
        this.inputArgs = inputArgs;
        this.javaArgs = javaArgs;
        this.mockAvailableMB = mockAvailableMB;
    }

    @Test
    public void testJvmOptions() throws Exception {
        ProcessBuilder processBuilderSpy = spy(new ProcessBuilder());
        whenNew(ProcessBuilder.class).withNoArguments().thenReturn(processBuilderSpy);

        Wrapper wrapper = spy(new Wrapper());
        if (mockAvailableMB != null) {
            doReturn(mockAvailableMB).when(wrapper).getAvailableMemoryInMB();
        }

        wrapper.parseAndExecute(inputArgs);

        verifyNew(ProcessBuilder.class).withNoArguments();
        verify(processBuilderSpy).command(javaArgs);
    }

    protected static String getJavaBinPath() {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Paths.get(javaHome, "bin", "java");
        return javaBin.toString();
    }
}
