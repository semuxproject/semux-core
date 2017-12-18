/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.util.List;

import org.semux.api.ApiHandlerResponse;
import org.semux.crypto.Hex;
import org.semux.net.Peer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetPeersResponse extends ApiHandlerResponse {

    @JsonProperty("result")
    public final List<Result> peers;

    public GetPeersResponse( //
            @JsonProperty("success") Boolean success, //
            @JsonProperty("result") List<Result> peers //
    ) {
        super(success, null);
        this.peers = peers;
    }

    public static class Result {

        @JsonProperty("ip")
        public final String ip;

        @JsonProperty("port")
        public final Integer port;

        @JsonProperty("networkVersion")
        public final Short networkVersion;

        @JsonProperty("clientId")
        public final String clientId;

        @JsonProperty("peerId")
        public final String peerId;

        @JsonProperty("latestBlockNumber")
        public final Long latestBlockNumber;

        @JsonProperty("latency")
        public final Long latency;

        public Result( //
                @JsonProperty("ip") String ip, //
                @JsonProperty("port") int port, //
                @JsonProperty("networkVersion") short networkVersion, //
                @JsonProperty("clientId") String clientId, //
                @JsonProperty("peerId") String peerId, //
                @JsonProperty("latestBlockNumber") long latestBlockNumber, //
                @JsonProperty("latency") long latency //
        ) {
            this.ip = ip;
            this.port = port;
            this.networkVersion = networkVersion;
            this.clientId = clientId;
            this.peerId = peerId;
            this.latestBlockNumber = latestBlockNumber;
            this.latency = latency;
        }

        public Result(Peer peer) {
            this(peer.getIp(), peer.getPort(), peer.getNetworkVersion(), peer.getClientId(),
                    Hex.PREF + peer.getPeerId(), peer.getLatestBlockNumber(), peer.getLatency());
        }
    }
}
