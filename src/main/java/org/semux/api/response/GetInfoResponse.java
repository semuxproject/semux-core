/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.Kernel;
import org.semux.api.ApiHandlerResponse;
import org.semux.crypto.Hex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetInfoResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Result info;

    public GetInfoResponse( //
            @JsonProperty("success") Boolean success, //
            @JsonProperty("result") Result info //
    ) {
        super(success, null);
        this.info = info;
    }

    public static class Result {
        @JsonProperty("clientId")
        public final String clientId;

        @JsonProperty("coinbase")
        public final String coinbase;

        @JsonProperty("latestBlockNumber")
        public final Number latestBlockNumber;

        @JsonProperty("latestBlockHash")
        public final String latestBlockHash;

        @JsonProperty("activePeers")
        public final Number activePeers;

        @JsonProperty("pendingTransactions")
        public final Number pendingTransactions;

        public Result(@JsonProperty("clientId") String clientId, @JsonProperty("coinbase") String coinbase,
                @JsonProperty("latestBlockNumber") Number latestBlockNumber,
                @JsonProperty("latestBlockHash") String latestBlockHash,
                @JsonProperty("activePeers") Number activePeers,
                @JsonProperty("pendingTransactions") Number pendingTransactions) {
            this.clientId = clientId;
            this.coinbase = coinbase;
            this.latestBlockNumber = latestBlockNumber;
            this.latestBlockHash = latestBlockHash;
            this.activePeers = activePeers;
            this.pendingTransactions = pendingTransactions;
        }

        public Result(Kernel kernel) {
            this(kernel.getConfig().getClientId(), Hex.PREF + kernel.getCoinbase(),
                    kernel.getBlockchain().getLatestBlockNumber(),
                    Hex.encode0x(kernel.getBlockchain().getLatestBlockHash()),
                    kernel.getChannelManager().getActivePeers().size(),
                    kernel.getPendingManager().getTransactions().size());
        }
    }
}
