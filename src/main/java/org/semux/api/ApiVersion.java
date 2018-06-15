/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

public enum ApiVersion {

    v2_0_0("v2.0.0"),

    v2_1_0("v2.1.0");

    public final String prefix;

    ApiVersion(String prefix) {
        this.prefix = prefix;
    }

    public static ApiVersion fromPrefix(String prefix) {
        switch (prefix) {
        case "v2.0.0":
            return v2_0_0;
        case "v2.1.0":
            return v2_1_0;
        default:
            return null;
        }
    }

}
