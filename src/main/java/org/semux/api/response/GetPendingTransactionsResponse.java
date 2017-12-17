/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.util.List;

import org.semux.api.ApiHandlerResponse;
import org.semux.api.response.GetTransactionResponse.Result;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetPendingTransactionsResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final List<Result> pendingTransactions;

    public GetPendingTransactionsResponse( //
            @JsonProperty("success") Boolean success, //
            @JsonProperty("result") List<Result> pendingTransactions //
    ) {
        super(success, null);
        this.pendingTransactions = pendingTransactions;
    }
}
