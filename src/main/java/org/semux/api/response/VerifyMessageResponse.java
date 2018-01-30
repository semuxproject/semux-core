/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.api.ApiHandlerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
public class VerifyMessageResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Boolean validSignature;

    public VerifyMessageResponse(
            @JsonProperty("success") Boolean success, @JsonProperty("result") Boolean validSignature) {
        super(success, null);
        this.validSignature = validSignature;
    }
}