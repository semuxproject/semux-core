/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import java.util.ResourceBundle;

public final class CliMessages {

    private static final ResourceBundle RESOURCES = ResourceBundles.getDefaultBundle(ResourceBundles.CLI_MESSAGES);

    private CliMessages() {
    }

    public static String get(String key, Object... args) {
        return MessageFormatter.get(RESOURCES, key, args);
    }
}
