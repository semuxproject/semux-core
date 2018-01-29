/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.util.Map;

import org.semux.api.ApiHandlerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetVotesResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Map<String, Long> votes;

    public GetVotesResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") Map<String, Long> votes) {
        super(success, null);
        this.votes = votes;
    }
}
