/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.util.List;
import java.util.stream.Collectors;

import org.semux.api.ApiHandlerResponse;
import org.semux.core.Block;
import org.semux.crypto.Hex;
import org.semux.util.TimeUtil;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetBlockResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final BlockResult block;

    public GetBlockResponse(
            @JsonProperty("success") Boolean success,
            @JsonProperty("result") BlockResult block) {
        super(success, null);
        this.block = block;
    }

    public static class BlockResult {

        @JsonProperty("hash")
        public final String hash;

        @JsonProperty("number")
        public final Long number;

        @JsonProperty("view")
        public final Integer view;

        @JsonProperty("coinbase")
        public final String coinbase;

        @JsonProperty("parentHash")
        public final String parentHash;

        @JsonProperty("timestamp")
        public final Long timestamp;

        @JsonProperty("date")
        public final String date;

        @JsonProperty("transactionsRoot")
        public final String transactionsRoot;

        @JsonProperty("resultsRoot")
        public final String resultsRoot;

        @JsonProperty("stateRoot")
        public final String stateRoot;

        @JsonProperty("data")
        public final String data;

        @JsonProperty("transactions")
        public final List<GetTransactionResponse.TransactionResult> transactions;

        public BlockResult(
                @JsonProperty("hash") String hash,
                @JsonProperty("number") Long number,
                @JsonProperty("view") Integer view,
                @JsonProperty("coinbase") String coinbase,
                @JsonProperty("parentHash") String parentHash,
                @JsonProperty("timestamp") Long timestamp,
                @JsonProperty("date") String date,
                @JsonProperty("transactionsRoot") String transactionsRoot,
                @JsonProperty("resultsRoot") String resultsRoot,
                @JsonProperty("stateRoot") String stateRoot,
                @JsonProperty("data") String data,
                @JsonProperty("transactions") List<GetTransactionResponse.TransactionResult> transactions) {
            this.hash = hash;
            this.number = number;
            this.view = view;
            this.coinbase = coinbase;
            this.parentHash = parentHash;
            this.timestamp = timestamp;
            this.date = date;
            this.transactionsRoot = transactionsRoot;
            this.resultsRoot = resultsRoot;
            this.stateRoot = stateRoot;
            this.data = data;
            this.transactions = transactions;
        }

        public BlockResult(Block block) {
            this(Hex.encode0x(block.getHash()),
                    block.getNumber(),
                    block.getView(),
                    Hex.encode0x(block.getCoinbase()),
                    Hex.encode0x(block.getParentHash()),
                    block.getTimestamp(),
                    TimeUtil.formatTimestamp(block.getTimestamp()),
                    Hex.encode0x(block.getTransactionsRoot()),
                    Hex.encode0x(block.getResultsRoot()),
                    Hex.encode0x(block.getStateRoot()),
                    Hex.encode0x(block.getData()),
                    block.getTransactions().stream()
                            .map(GetTransactionResponse.TransactionResult::new)
                            .collect(Collectors.toList()));
        }
    }
}
