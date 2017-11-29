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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;


@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerConfigurator.class)
public class LoggerConfiguratorTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    File mockConfigFile;

    @After
    public void tearDown() throws IOException {
        if (mockConfigFile != null && mockConfigFile.exists()) {
            Files.delete(mockConfigFile.toPath());
            mockConfigFile = null;
        }
    }

    @Test
    public void testConfigureFactoryDefault() throws IOException {
        // when the user-defined config file doesn't exit
        String usedDefinedConfig = getFactoryDefaultConfig();
        mockConfigFile(usedDefinedConfig);
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile()).thenReturn(null);

        // execution
        LoggerConfigurator.configure();

        // the log level should be the same as the factory default
        // logback-test.xml is used in this case
        assertRootLogLevel(Level.DEBUG);
    }

    @Test
    public void testConfigureUserDefined() throws IOException {
        // mock user-defined config file configured as DEBUG
        String usedDefinedConfig = getFactoryDefaultConfig().replace("INFO", "ERROR");
        mockConfigFile(usedDefinedConfig);
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile()).thenReturn(mockConfigFile);

        // execution
        LoggerConfigurator.configure();

        // the log level should be changed to ERROR
        assertRootLogLevel(Level.ERROR);
    }

    @Test
    public void testConfigureUserDefinedError() throws IOException {
        // mock invalid config file
        mockConfigFile("I am not a XML file");
        spy(LoggerConfigurator.class);
        when(LoggerConfigurator.getConfigurationFile()).thenReturn(mockConfigFile);

        // expect system exit
        exit.expectSystemExitWithStatus(-1);
        systemErrRule.enableLog();

        // execution
        LoggerConfigurator.configure();

        // there should be error messages
        assertTrue(systemErrRule.getLog().length() > 0);
    }

    private void assertRootLogLevel(Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        assertTrue((context).getLogger(Logger.ROOT_LOGGER_NAME).getLevel() == level);
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
