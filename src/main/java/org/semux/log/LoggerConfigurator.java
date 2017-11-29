/**
 * Copyright (c) 2017 The Semux Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.semux.Config;
import org.semux.util.SystemUtil;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

/**
 * The configurator will try to load logback.xml from config directory at
 * runtime. If logback.xml doesn't exit in user's config directory, the factory
 * default of src/main/resources/logback.xml will be used.
 */
public class LoggerConfigurator {

    public static final String LOGBACK_XML = "logback.xml";

    private LoggerConfigurator() {
    }

    public static void configure() {
        File configurationFile = getConfigurationFile();
        if (configurationFile != null && configurationFile.exists()) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset(); // Call context.reset() to clear default configuration
                configurator.doConfigure(configurationFile);
            } catch (JoranException je) {
                System.err.format(
                        "Failed to load %s. The xml file is either corrupted or invalid. Try to fix the xml file or replace it with the factory default.%n",
                        configurationFile.getAbsolutePath());
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
                SystemUtil.exit(-1);
            }
        }
    }

    protected static File getConfigurationFile() {
        return Paths.get(Config.CONFIG_DIR, LOGBACK_XML).toFile();
    }
}
