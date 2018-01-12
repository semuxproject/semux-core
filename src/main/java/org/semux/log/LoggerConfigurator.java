/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.semux.config.Constants;

/**
 * The configurator will try to load log4j2.xml from config directory at
 * runtime. If log4j2.xml doesn't exist in user's config directory, the factory
 * default of src/main/resources/log4j2.xml will be used.
 */
public class LoggerConfigurator {

    public static final String LOGBACK_XML = "log4j2.xml";
    public static final String DEBUG_LOG = "debug.log";

    private LoggerConfigurator() {
    }

    public static void configure(File dataDir) {
        File file = getConfigurationFile(dataDir);

        if (file.exists()) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(file.toURI());
            if (System.getProperty("log.file") == null) {
                System.setProperty("log.file", new File(dataDir, DEBUG_LOG).getAbsolutePath());
            }
            context.reconfigure();
            context.updateLoggers();
        } else {
            StatusLogger.getLogger().error("Logger config file {} doesn't exist, using the factory default",
                    file.getAbsolutePath());
        }
    }

    protected static File getConfigurationFile(File dataDir) {
        return new File(dataDir, Constants.CONFIG_DIR + File.separator + LOGBACK_XML);
    }
}
