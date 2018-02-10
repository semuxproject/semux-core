/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.log;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.semux.config.Constants;
import org.semux.util.SystemUtil;
import org.xml.sax.SAXException;

/**
 * The configurator will try to load log4j2.xml from config directory at
 * runtime. If log4j2.xml doesn't exist in user's config directory, the factory
 * default of src/main/resources/log4j2.xml will be used.
 */
public class LoggerConfigurator {

    public static final String CONFIG_XML = "log4j2.xml";
    public static final String DEBUG_LOG = "debug.log";

    private LoggerConfigurator() {
    }

    public static void configure(File dataDir) {
        File file = getConfigurationFile(dataDir);

        if (file.exists()) {
            if (System.getProperty("log.file") == null) {
                System.setProperty("log.file", new File(dataDir, DEBUG_LOG).getAbsolutePath());
            }

            // register configuration error listener
            StatusListener errorStatusListener = new ConfigurationErrorStatusListener();
            StatusLogger.getLogger().registerListener(errorStatusListener);

            // load configuration
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(file.toURI());
            context.reconfigure();
            context.updateLoggers();

            // remove configuration error listener
            StatusLogger.getLogger().removeListener(errorStatusListener);
        }
    }

    protected static File getConfigurationFile(File dataDir) {
        return new File(dataDir, Constants.CONFIG_DIR + File.separator + CONFIG_XML);
    }

    /**
     * Error listener to configuration error. The listener exits the process when it
     * receives a configuration error from
     * {@link org.apache.logging.log4j.core.config.xml.XmlConfiguration}.
     */
    private static class ConfigurationErrorStatusListener implements StatusListener {

        @Override
        public void log(StatusData data) {
            Throwable throwable = data.getThrowable();
            if (throwable instanceof SAXException
                    || throwable instanceof IOException
                    || throwable instanceof ParserConfigurationException) {
                SystemUtil.exit(1);
            }
        }

        @Override
        public Level getStatusLevel() {
            return Level.ERROR;
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }
    }
}
