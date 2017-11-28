/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.message;

import java.util.ResourceBundle;

public final class GUIMessages {

    private static final ResourceBundle RESOURCES = ResourceBundles.getDefaultBundle(ResourceBundles.GUI_MESSAGES);

    private GUIMessages() {
    }

    public static String get(String key, Object... args) {
        return MessageFormatter.get(RESOURCES, key, args);
    }
}