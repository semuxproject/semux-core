/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.semux.api.ApiHandlerResponse;
import org.semux.core.Transaction;
import org.semux.crypto.Hex;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetTransactionResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final Result transaction;

    public GetTransactionResponse( //
            @JsonProperty("success") Boolean success, //
            @JsonProperty("result") Result transaction //
    ) {
        super(success, null);
        this.transaction = transaction;
    }

    public static class Result {

        @JsonProperty("hash")
        public final String hash;

        @JsonProperty("type")
        public final String type;

        @JsonProperty("from")
        public final String from;

        @JsonProperty("to")
        public final String to;

        @JsonProperty("value")
        public final Long value;

        @JsonProperty("fee")
        public final Long fee;

        @JsonProperty("nonce")
        public final Long nonce;

        @JsonProperty("timestamp")
        public final Long timestamp;

        @JsonProperty("date")
        public final String date;

        @JsonProperty("data")
        public final String data;

        public Result( //
                @JsonProperty("hash") String hash, //
                @JsonProperty("type") String type, //
                @JsonProperty("from") String from, //
                @JsonProperty("to") String to, //
                @JsonProperty("value") Long value, //
                @JsonProperty("fee") Long fee, //
                @JsonProperty("nonce") Long nonce, //
                @JsonProperty("timestamp") Long timestamp, //
                @JsonProperty("date") String date, //
                @JsonProperty("data") String data //
        ) {
            this.hash = hash;
            this.type = type;
            this.from = from;
            this.to = to;
            this.value = value;
            this.fee = fee;
            this.nonce = nonce;
            this.timestamp = timestamp;
            this.date = date;
            this.data = data;
        }

        public Result(Transaction tx) {
            this(Hex.encode0x(tx.getHash()), //
                    tx.getType().toString(), //
                    Hex.encode0x(tx.getFrom()), //
                    Hex.encode0x(tx.getTo()), //
                    tx.getValue(), //
                    tx.getFee(), //
                    tx.getNonce(), //
                    tx.getTimestamp(), //
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(tx.getTimestamp())), //
                    Hex.encode0x(tx.getData()));
        }
    }
}
