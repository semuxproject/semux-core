/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.api.ApiHandlerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetBlockResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Types.BlockType block;

    public GetBlockResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") Types.BlockType block) {
        super(success, null);
        this.block = block;
    }
}
