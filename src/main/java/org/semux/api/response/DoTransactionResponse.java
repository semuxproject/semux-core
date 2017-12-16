/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DoTransactionResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final String txId;

    public DoTransactionResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("message") String message,
            @JsonProperty("result") String txId) {
        super(success, message);
        this.txId = txId;
    }
}
