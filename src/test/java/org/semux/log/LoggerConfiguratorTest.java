/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.config.Constants;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
public class LoggerConfiguratorTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

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

        ((LoggerContext) LoggerFactory.getILoggerFactory()).reset();
    }

    @Test
    @PrepareForTest(LoggerConfigurator.class)
    public void testConfigureFactoryDefault() {
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
    @PrepareForTest(LoggerConfigurator.class)
    public void testConfigureUserDefined() throws IOException {
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
    @PrepareForTest(LoggerConfigurator.class)
    public void testConfigureUserDefinedError() throws IOException {
        // mock invalid config file
        mockConfigFile("I am not a XML file");
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile(dataDir)).thenReturn(mockConfigFile);

        // expect system exit
        exit.expectSystemExitWithStatus(-1);
        systemErrRule.enableLog();

        // execution
        LoggerConfigurator.configure(dataDir);

        // there should be error messages
        assertTrue(systemErrRule.getLog().length() > 0);
    }

    private void assertRootLogLevel(Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        assertTrue(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel() == level);
    }

    private void mockConfigFile(String content) throws IOException {
        mockConfigFile = File.createTempFile(LoggerConfiguratorTest.class.getSimpleName(), "logback.xml");
        FileWriter fileWriter = new FileWriter(mockConfigFile);
        fileWriter.write(content);
        fileWriter.close();
    }

    private String getFactoryDefaultConfig() throws FileNotFoundException {
        InputStream in = getClass().getResourceAsStream("/" + LoggerConfigurator.LOGBACK_XML);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
