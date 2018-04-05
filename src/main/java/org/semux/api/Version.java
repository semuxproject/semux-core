/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import org.semux.util.exception.UnreachableException;

public enum Version {

    v1_0_1,

    v1_1_0;

    public static String prefixOf(Version version) {
        switch (version) {
        case v1_0_1:
            return "v1.0.1";
        case v1_1_0:
            return "v1.1.0";
        default:
            throw new UnreachableException();
        }
    }

    public static Version fromPrefix(String prefix) {
        switch (prefix) {
        case "v1.0.1":
            return v1_0_1;
        case "v1.1.0":
            return v1_1_0;
        default:
            return null;
        }
    }

}
