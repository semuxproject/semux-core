/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiHandlerSuccessResponse extends ApiHandlerResponse {

    @JsonProperty("success")
    public Boolean success = true;

    @JsonProperty("result")
    public Object result;

    public ApiHandlerSuccessResponse(Object result) {
        this.result = result;
    }
}
