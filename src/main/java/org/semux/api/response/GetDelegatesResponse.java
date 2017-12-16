/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.util.List;

import org.semux.api.ApiHandlerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetDelegatesResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final List<GetDelegateResponse.Result> delegateResults;

    public GetDelegatesResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") List<GetDelegateResponse.Result> delegateResults) {
        super(success, null);
        this.delegateResults = delegateResults;
    }
}
