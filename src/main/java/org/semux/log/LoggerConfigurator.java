/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import java.io.File;

import org.semux.config.Constants;
import org.semux.util.SystemUtil;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * The configurator will try to load logback.xml from config directory at
 * runtime. If logback.xml doesn't exist in user's config directory, the factory
 * default of src/main/resources/logback.xml will be used.
 */
public class LoggerConfigurator {

    public static final String LOGBACK_XML = "logback.xml";

    private LoggerConfigurator() {
    }

    public static void configure(File dataDir) {
        File file = getConfigurationFile(dataDir);
        if (file != null && file.exists()) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset(); // Call context.reset() to clear default configuration
                configurator.doConfigure(file);
            } catch (JoranException je) {
                System.err.format(
                        "Failed to load %s. The xml file is either corrupted or invalid. Try to fix the xml file or replace it with the factory default.%n",
                        file.getAbsolutePath());
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
                SystemUtil.exit(-1);
            }
        }
    }

    protected static File getConfigurationFile(File dataDir) {
        return new File(dataDir, Constants.CONFIG_DIR + File.separator + LOGBACK_XML);
    }
}
