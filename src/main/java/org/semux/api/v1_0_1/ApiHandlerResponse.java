/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_0_1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ApiHandlerResponse is the base class of Semux API responses
 * @deprecated
 */
public class ApiHandlerResponse {

    @JsonProperty(value = "success", required = true)
    public final Boolean success;

    @JsonProperty("message")
    @JsonInclude(NON_NULL)
    public String message;

    @JsonCreator
    public ApiHandlerResponse(
            @JsonProperty(value = "success", required = true) Boolean success,
            @JsonProperty("message") String message) {
        this.success = success;
        this.message = message;
    }
}
