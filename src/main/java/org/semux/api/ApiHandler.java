/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.Map;

import javax.ws.rs.core.Response;

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
     * @param path
     *            the path
     * @param params
     *            the params
     * @param headers
     *            the headers
     * @return the response object
     */
    Response service(HttpMethod method, String path, Map<String, String> params, HttpHeaders headers);

    boolean isAuthRequired(HttpMethod method, String path);
}
