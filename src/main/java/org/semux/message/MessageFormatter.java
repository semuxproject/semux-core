/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class MessageFormatter {

    private MessageFormatter() {
    }

    /**
     * Gets a value from this bundle for the given {@code key}. Any second arguments
     * will be used to format the value.
     *
     * @param resourceBundle
     *            the resource bundle of messages
     * @param key
     *            the bundle key
     * @param args
     *            objects used to format the value.
     * @return the formatted value for the given key.
     */
    public static String get(ResourceBundle resourceBundle, String key, Object... args) {
        String value = resourceBundle.getString(key);
        return MessageFormat.format(value, args);
    }
}
