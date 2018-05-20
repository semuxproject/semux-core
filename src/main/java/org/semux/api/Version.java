/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

public enum Version {

    v1_0_1("v1.0.1"),

    v2_0_0("v2.0.0"),

    v2_1_0("v2.1.0");

    public final String prefix;

    Version(String prefix) {
        this.prefix = prefix;
    }

    public static Version fromPrefix(String prefix) {
        switch (prefix) {
        case "v1.0.1":
            return v1_0_1;
        case "v2.0.0":
            return v2_0_0;
        case "v2.1.0":
            return v2_1_0;
        default:
            return null;
        }
    }

}
