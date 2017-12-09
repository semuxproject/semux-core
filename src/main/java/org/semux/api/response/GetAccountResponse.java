/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.core.state.Account;
import org.semux.crypto.Hex;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetAccountResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Result account;

    public GetAccountResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") Result account) {
        super(success, null);
        this.account = account;
    }

    public static class Result {

        public final String address;
        public final long available;
        public final long locked;
        public final long nonce;

        @JsonCreator
        public Result(
                @JsonProperty("address") String address,
                @JsonProperty("available") long available,
                @JsonProperty("locked") long locked,
                @JsonProperty("nonce") long nonce) {
            this.address = address;
            this.available = available;
            this.locked = locked;
            this.nonce = nonce;
        }

        public Result(Account account) {
            this(
                    Hex.encode0x(account.getAddress()),
                    account.getAvailable(),
                    account.getLocked(),
                    account.getNonce());
        }
    }
}
