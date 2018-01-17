/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Dummy log4j2 appender that collects all logging events into a list, for
 * testing purpose only.
 *
 */
@Plugin(name = "TestLoggingAppender", category = "Core", elementType = "appender", printObject = true)
public final class TestLoggingAppender extends AbstractAppender {

    private static List<LogEvent> events = new ArrayList<>();

    private static Level testLevel = Level.OFF;

    protected TestLoggingAppender(String name, Filter filter,
            Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    protected TestLoggingAppender(String name, Filter filter,
            Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    public static void prepare(Level level) {
        testLevel = level;
    }

    /**
     * Returns all collected events.
     *
     * @return
     */
    public static List<LogEvent> events() {
        return events;
    }

    /**
     * Clears all events.
     */
    public static void clear() {
        events.clear();
    }

    @Override
    public void append(LogEvent e) {
        if (e.getLevel().intLevel() >= testLevel.intLevel()) {
            events.add(new LogEventMini(e));
        }
    }

    /**
     * Construct an INFO {@link LogEvent}.
     *
     * @param msg
     * @return
     */
    public static LogEvent info(String msg) {
        return log(Level.INFO, msg);
    }

    protected static LogEvent log(Level lvl, String msg) {
        return new LogEventMini(lvl, msg);
    }

    /**
     * A {@link LogEventMini} presents the level and formatted message of an
     * {@link LogEvent}. It overrides the {@link #equals(Object)} but not the
     * {@link #hashCode()} methods, and should never be used for production.
     *
     */
    public static class LogEventMini extends AbstractLogEvent {
        private static final long serialVersionUID = 1L;

        private Level level;
        private String formattedMessage;

        public LogEventMini(Level level, String formattedMessage) {
            this.level = level;
            this.formattedMessage = formattedMessage;
        }

        public LogEventMini(LogEvent e) {
            this.level = e.getLevel();
            this.formattedMessage = e.getMessage().getFormattedMessage();
        }

        public String getFormattedMessage() {
            return formattedMessage;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LogEventMini) {
                LogEventMini ev = (LogEventMini) obj;
                return level.equals(ev.getLevel()) && formattedMessage.equals(ev.getFormattedMessage());
            } else {
                return false;
            }
        }
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    // the configured attributes.
    @PluginFactory
    public static TestLoggingAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<?> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for TestLog4j2Appender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TestLoggingAppender(name, filter, layout, true);
    }
}