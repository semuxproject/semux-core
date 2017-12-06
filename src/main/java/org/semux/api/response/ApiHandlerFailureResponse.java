/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiHandlerFailureResponse {

    @JsonProperty("success")
    public Boolean success = false;

    @JsonProperty("message")
    public String message;

    public ApiHandlerFailureResponse(String message) {
        this.message = message;
    }
}
