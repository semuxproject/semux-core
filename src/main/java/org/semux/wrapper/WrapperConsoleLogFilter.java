/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * The log filter prevents outputs of wrapper classes from being printed to
 * console
 */
public class WrapperConsoleLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        if (iLoggingEvent.getLevel().levelInt < Level.INFO.levelInt
                && iLoggingEvent.getLoggerName().startsWith("org.semux.wrapper")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
