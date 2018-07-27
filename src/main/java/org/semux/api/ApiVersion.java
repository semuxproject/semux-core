/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an API version, in the format of MAJOR.MINOR.PATCH.
 *
 * <ul>
 * <li>MAJOR version increases when backward compatibility is lost.</li>
 * <li>MINOR version increases when incremental improvements is introduced.</li>
 * <li>PATCH version is reserved for security patches</li>
 * </ul>
 */
public enum ApiVersion {

    // no longer supported
    v1_0_0("v1.0.0"), v1_0_1("v1.0.1"),

    v2_0_0("v2.0.0"), v2_1_0("v2.1.0");

    public final static ApiVersion DEFAULT = v2_1_0;

    public final String prefix;

    private static Map<String, ApiVersion> versions = new HashMap<>();

    static {
        for (ApiVersion version : values()) {
            versions.put(version.prefix, version);
        }
    }

    ApiVersion(String prefix) {
        this.prefix = prefix;
    }

    public static ApiVersion of(String prefix) {
        return versions.get(prefix);
    }

}
