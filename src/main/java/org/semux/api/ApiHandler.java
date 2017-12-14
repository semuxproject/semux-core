/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.Map;

import org.semux.api.exception.ApiHandlerException;
import org.semux.api.response.ApiHandlerResponse;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * Semux RESTful API handler.
 *
 */
public interface ApiHandler {

    /**
     * Processes API request.
     * 
     * @param uri
     * @param params
     * @param headers
     * @return
     */
    ApiHandlerResponse service(String uri, Map<String, String> params, HttpHeaders headers) throws ApiHandlerException;
}
