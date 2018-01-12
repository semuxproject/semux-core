/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.api.ApiHandlerResponse;
import org.semux.core.BlockchainImpl;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetDelegateResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final DelegateResult delegate;

    public GetDelegateResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") DelegateResult delegate) {
        super(success, null);
        this.delegate = delegate;
    }

    public static class DelegateResult {

        @JsonProperty("address")
        public final String address;

        @JsonProperty("name")
        public final String name;

        @JsonProperty("registeredAt")
        public final Long registeredAt;

        @JsonProperty("votes")
        public final Long votes;

        @JsonProperty("blocksForged")
        public final Long blocksForged;

        @JsonProperty("turnsHit")
        public final Long turnsHit;

        @JsonProperty("turnsMissed")
        public final Long turnsMissed;

        public DelegateResult(BlockchainImpl.ValidatorStats validatorStats, Delegate delegate) {
            this(Hex.PREF + delegate.getAddressString(),
                    delegate.getNameString(),
                    delegate.getRegisteredAt(),
                    delegate.getVotes(),
                    validatorStats.getBlocksForged(),
                    validatorStats.getTurnsHit(),
                    validatorStats.getTurnsMissed());
        }

        public DelegateResult(
                @JsonProperty("address") String address,
                @JsonProperty("name") String name,
                @JsonProperty("registeredAt") Long registeredAt,
                @JsonProperty("votes") Long votes,
                @JsonProperty("blocksForged") Long blocksForged,
                @JsonProperty("turnsHit") Long turnsHit,
                @JsonProperty("turnsMissed") Long turnsMissed) {
            this.address = address;
            this.name = name;
            this.registeredAt = registeredAt;
            this.votes = votes;
            this.blocksForged = blocksForged;
            this.turnsHit = turnsHit;
            this.turnsMissed = turnsMissed;
        }
    }
}
