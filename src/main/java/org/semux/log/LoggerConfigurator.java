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

        if (file.exists()) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                context.reset(); // Call context.reset() to clear default configuration
                context.putProperty("logger.file", new File(dataDir, "debug.log").getAbsolutePath());

                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(file);
            } catch (JoranException je) {
                System.err.format("The logback configure file is invalid: %s", file.getAbsolutePath());
                SystemUtil.exit(-1);
            }
        }
    }

    protected static File getConfigurationFile(File dataDir) {
        return new File(dataDir, Constants.CONFIG_DIR + File.separator + LOGBACK_XML);
    }
}
