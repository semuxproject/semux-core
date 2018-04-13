/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.function.Function;

import org.semux.Kernel;
import org.semux.api.v1_0_1.ApiHandlerImpl;

public enum Version {

    v1_0_1("v1.0.1", ApiHandlerImpl::new),

    v2_0_0("v2.0.0", org.semux.api.v2_0_0.impl.ApiHandlerImpl::new);

    public final String prefix;

    public final Function<Kernel, ApiHandler> apiHandlerFactory;

    Version(String prefix, Function<Kernel, ApiHandler> apiHandlerFactory) {
        this.prefix = prefix;
        this.apiHandlerFactory = apiHandlerFactory;
    }

    public static Version fromPrefix(String prefix) {
        switch (prefix) {
        case "v1.0.1":
            return v1_0_1;
        case "v2.0.0":
            return v2_0_0;
        default:
            return null;
        }
    }

}
