/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.api.ApiHandlerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRootResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final String result;

    public GetRootResponse( //
            @JsonProperty("success") Boolean success, //
            @JsonProperty("result") String result //
    ) {
        super(success, null);
        this.result = result;
    }
}
