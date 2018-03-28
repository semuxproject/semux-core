/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_0_1.response;

import static org.semux.core.TransactionType.DELEGATE;

import org.semux.Kernel;
import org.semux.api.v1_0_1.ApiHandlerResponse;
import org.semux.core.TransactionType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetTransactionLimitsResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Types.TransactionLimitsType transactionLimits;

    public GetTransactionLimitsResponse(Kernel kernel, TransactionType transactionType) {
        super(true, null);
        this.transactionLimits = new Types.TransactionLimitsType(
                kernel.getConfig().maxTransactionDataSize(transactionType),
                kernel.getConfig().minTransactionFee(),
                transactionType.equals(DELEGATE) ? kernel.getConfig().minDelegateBurnAmount() : null);
    }

    @JsonCreator
    public GetTransactionLimitsResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") Types.TransactionLimitsType result) {
        super(success, null);
        this.transactionLimits = result;
    }
}
