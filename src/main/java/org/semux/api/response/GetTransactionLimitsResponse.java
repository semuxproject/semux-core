/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.Kernel;
import org.semux.api.ApiHandlerResponse;
import org.semux.core.TransactionType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetTransactionLimitsResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Result result;

    public GetTransactionLimitsResponse(Kernel kernel, TransactionType transactionType) {
        super(true, null);
        this.result = new Result(
                kernel.getConfig().maxTransactionDataSize(transactionType),
                kernel.getConfig().minTransactionFee(),
                kernel.getConfig().minDelegateBurnAmount());
    }

    @JsonCreator
    public GetTransactionLimitsResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") Result result) {
        super(success, null);
        this.result = result;
    }

    public static class Result {

        @JsonProperty("maxTransactionDataSize")
        public final int maxTransactionDataSize;

        @JsonProperty("minTransactionFee")
        public final long minTransactionFee;

        @JsonProperty("minDelegateBurnAmount")
        public final long minDelegateBurnAmount;

        @JsonCreator
        public Result(
                @JsonProperty("maxTransactionDataSize") int maxTransactionDataSize,
                @JsonProperty("minTransactionFee") long minTransactionFee,
                @JsonProperty("minDelegateBurnAmount") long minDelegateBurnAmount) {
            this.maxTransactionDataSize = maxTransactionDataSize;
            this.minTransactionFee = minTransactionFee;
            this.minDelegateBurnAmount = minDelegateBurnAmount;
        }
    }
}
