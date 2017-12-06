/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.Kernel;
import org.semux.core.BlockchainImpl;
import org.semux.core.state.Delegate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetDelegateResponse {

    @JsonProperty("address")
    public String address;

    @JsonProperty("name")
    public String name;

    @JsonProperty("registeredAt")
    public Long registeredAt;

    @JsonProperty("votes")
    public Long votes;

    @JsonProperty("blocksForged")
    public Long blocksForged;

    @JsonProperty("turnsHit")
    public Long turnsHit;

    @JsonProperty("turnsMissed")
    public Long turnsMissed;

    public GetDelegateResponse(Kernel kernel, Delegate delegate) {
        BlockchainImpl.ValidatorStats validatorStats = kernel.getBlockchain().getValidatorStats(delegate.getAddress());

        address = delegate.getAddressString();
        name = delegate.getNameString();
        registeredAt = delegate.getRegisteredAt();
        votes = delegate.getVotes();
        blocksForged = validatorStats.getBlocksForged();
        turnsHit = validatorStats.getTurnsHit();
        turnsMissed = validatorStats.getTurnsMissed();
    }

}
