/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class provides utility functions for retrieving translations from a
 * resource bundle.<br>
 * By default it loads translations from {@value #RESOURCES_PATH}.
 * 
 * <p>
 * The locale used is the current value of the default locale for this instance
 * of the Java Virtual Machine.
 * </p>
 */
public final class MessagesUtil {

    private final static String RESOURCES_PATH = "org/semux/gui/messages";
    private final static ResourceBundle RESOURCES;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES_PATH, Locale.getDefault());
        RESOURCES = bundle == null ? ResourceBundle.getBundle(RESOURCES_PATH, Locale.ENGLISH) : bundle;
    }

    /**
     * Gets a value from this bundle for the given {@code key}. Any second arguments
     * will be used to format the value.
     * 
     * @param key
     *            the bundle key
     * @param args
     *            objects used to format the value.
     * @return the formatted value for the given key.
     */
    public static String get(String key, Object... args) {
        String value = RESOURCES.getString(key);
        return MessageFormat.format(value, args);
    }
}