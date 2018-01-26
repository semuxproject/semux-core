/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.semux.api.ApiHandlerResponse;

/**
 */
public class SignMessageResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final String signature;

    public SignMessageResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") String signature) {
        super(success, null);
        this.signature = signature;
    }
}