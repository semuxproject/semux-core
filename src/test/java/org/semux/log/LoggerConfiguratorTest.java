/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.config.Constants;
import org.semux.util.SystemUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "jdk.internal.*", "javax.management.*", "com.sun.*", "javax.xml.*", "org.xml.sax.*",
        "org.w3c.dom.*" })
@PrepareForTest({ LoggerConfigurator.class, SystemUtil.class })
public class LoggerConfiguratorTest {

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    File dataDir = new File(Constants.DEFAULT_DATA_DIR);
    File mockConfigFile;

    @After
    public void tearDown() throws IOException {
        if (mockConfigFile != null && mockConfigFile.exists()) {
            mockConfigFile.delete();
            mockConfigFile = null;
        }
    }

    @Test
    public void testConfigureFactoryDefault() {
        mockStatic(SystemUtil.class);

        // when the user-defined config file doesn't exit
        spy(LoggerConfigurator.class);
        File nonExistFile = mock(File.class);
        when(nonExistFile.exists()).thenReturn(false);
        when(LoggerConfigurator.getConfigurationFile(dataDir)).thenReturn(nonExistFile);

        // execution
        LoggerConfigurator.configure(dataDir);

        // the log level should be the same as the factory default
        // logback-test.xml is used in this case
        assertRootLogLevel(Level.DEBUG);
    }

    @Test
    public void testConfigureUserDefined() throws IOException {
        mockStatic(SystemUtil.class);

        // mock user-defined config file configured as DEBUG
        String usedDefinedConfig = getFactoryDefaultConfig().replace("INFO", "ERROR");
        mockConfigFile(usedDefinedConfig);
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile(dataDir)).thenReturn(mockConfigFile);

        // execution
        LoggerConfigurator.configure(dataDir);

        // the log level should be changed to ERROR
        assertRootLogLevel(Level.ERROR);
    }

    @Test
    public void testConfigureUserDefinedError() throws IOException {
        mockStatic(SystemUtil.class);

        // mock invalid config file
        mockConfigFile("I am not a XML file");
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile(dataDir)).thenReturn(mockConfigFile);

        verifyStatic(SystemUtil.class);
        systemErrRule.enableLog();

        // execution
        LoggerConfigurator.configure(dataDir);

        // there should be error messages
        assertTrue(systemErrRule.getLog().length() > 0);
    }

    private void assertRootLogLevel(Level level) {
        assertEquals(level, LogManager.getRootLogger().getLevel());
    }

    private void mockConfigFile(String content) throws IOException {
        mockConfigFile = File.createTempFile(LoggerConfiguratorTest.class.getSimpleName(),
                LoggerConfigurator.CONFIG_XML);
        FileWriter fileWriter = new FileWriter(mockConfigFile);
        fileWriter.write(content);
        fileWriter.close();
    }

    private String getFactoryDefaultConfig() {
        InputStream in = getClass().getResourceAsStream("/" + LoggerConfigurator.CONFIG_XML);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
