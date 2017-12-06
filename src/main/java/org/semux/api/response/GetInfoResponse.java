/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import org.semux.Kernel;
import org.semux.crypto.Hex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetInfoResponse {

    @JsonProperty("clientId")
    public String clientId;

    @JsonProperty("coinbase")
    public String coinbase;

    @JsonProperty("latestBlockNumber")
    public Number latestBlockNumber;

    @JsonProperty("latestBlockHash")
    public String latestBlockHash;

    @JsonProperty("activePeers")
    public Number activePeers;

    @JsonProperty("pendingTransactions")
    public Number pendingTransactions;

    public GetInfoResponse(Kernel kernel) {
        clientId = kernel.getConfig().getClientId();
        coinbase = Hex.PREF + kernel.getCoinbase();
        latestBlockNumber = kernel.getBlockchain().getLatestBlockNumber();
        latestBlockHash = Hex.encodeWithPrefix(kernel.getBlockchain().getLatestBlockHash());
        activePeers = kernel.getChannelManager().getActivePeers().size();
        pendingTransactions = kernel.getPendingManager().getTransactions().size();
    }
}
