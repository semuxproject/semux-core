/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Dummy SLF4J appender that collects all logging events into a list, for
 * testing purpose only.
 *
 */
public class TestAppender extends AppenderBase<LoggingEvent> {

    private static List<LoggingEvent> events = new ArrayList<>();

    private static Level testLevel = Level.OFF;
    private static Class<?> testClass = null;
    private static Logger testLogger = null;

    @Override
    protected void append(LoggingEvent e) {
        if (e.getLevel().isGreaterOrEqual(testLevel)) {
            events.add(new LoggingEventMini(e));
        }
    }

    /**
     * Prepares the log appender.
     * 
     * @param level
     * @param cls
     * @param logger
     */
    public static void prepare(Level level, Class<?> cls, org.slf4j.Logger logger) {
        testLevel = level;
        testClass = cls;
        testLogger = (Logger) logger; // unsafe
    }

    /**
     * Returns all collected events.
     * 
     * @return
     */
    public static List<LoggingEvent> events() {
        return events;
    }

    /**
     * Clears all events.
     */
    public static void clear() {
        events.clear();
    }

    /**
     * Construct an ERROR {@link LoggingEvent}.
     * 
     * @param msg
     * @param args
     * @return
     */
    public static LoggingEvent error(String msg, Object... args) {
        return log(Level.ERROR, msg, args);
    }

    /**
     * Construct an INFO {@link LoggingEvent}.
     * 
     * @param msg
     * @param args
     * @return
     */
    public static LoggingEvent info(String msg, Object... args) {
        return log(Level.INFO, msg, args);
    }

    /**
     * Construct a DEBUG {@link LoggingEvent}.
     * 
     * @param msg
     * @param args
     * @return
     */
    public static LoggingEvent debug(String msg, Object... args) {
        return log(Level.DEBUG, msg, args);
    }

    protected static LoggingEvent log(Level lvl, String msg, Object[] args) {
        if (testClass == null || testLogger == null) {
            throw new RuntimeException("TestAppender is not prepared");
        }

        Throwable throwable = null;
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            throwable = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }

        return new LoggingEvent(testClass.getCanonicalName(), testLogger, lvl, msg, throwable, args);
    }

    /**
     * A {@link LoggingEventMini} presents the level and formatted message of an
     * {@link LoggingEvent}. It overrides the {@link #equals(Object)} but not the
     * {@link #hashCode()} methods, and should never be used for production.
     * 
     */
    public static class LoggingEventMini extends LoggingEvent {
        private Level level;
        private String formattedMessage;

        public LoggingEventMini(LoggingEvent ev) {
            this.level = ev.getLevel();
            this.formattedMessage = ev.getFormattedMessage();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LoggingEvent) {
                LoggingEvent ev = (LoggingEvent) obj;
                return level == ev.getLevel() && formattedMessage.equals(ev.getFormattedMessage());
            } else {
                return false;
            }
        }
    }
}