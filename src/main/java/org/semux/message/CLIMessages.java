/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import java.util.ResourceBundle;

public final class CLIMessages {

    private static final ResourceBundle RESOURCES = ResourceBundles.getDefaultBundle(ResourceBundles.CLI_MESSAGES);

    private CLIMessages() {
    }

    public static String get(String key, Object... args) {
        return MessageFormatter.get(RESOURCES, key, args);
    }
}
