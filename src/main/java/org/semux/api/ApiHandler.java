/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.Map;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

/**
 * Semux RESTful API handler.
 */
public interface ApiHandler {

    /**
     * Service object.
     *
     * @param method
     *            the method
     * @param uri
     *            the uri
     * @param params
     *            the params
     * @param headers
     *            the headers
     * @return the response object
     */
    Object service(HttpMethod method, String uri, Map<String, String> params, HttpHeaders headers);
}
