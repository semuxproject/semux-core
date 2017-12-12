/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * ApiHandlerResponse is the base class of Semux API responses
 */
public class ApiHandlerResponse {

    @JsonProperty(value = "success", required = true)
    public final Boolean success;

    @JsonProperty("message")
    @JsonInclude(NON_NULL)
    public String message;

    @JsonIgnore
    public final HttpResponseStatus status;

    @JsonCreator
    public ApiHandlerResponse(
            @JsonProperty(value = "success", required = true) Boolean success,
            @JsonProperty("message") String message) {
        this.success = success;
        this.message = message;
        this.status = HttpResponseStatus.OK;
    }

    public ApiHandlerResponse(Boolean success, String message, HttpResponseStatus status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
