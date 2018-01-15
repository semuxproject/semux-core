/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;
import org.semux.net.Peer;
import org.semux.util.TimeUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Types {

    public static class AccountType {

        public final String address;
        public final long available;
        public final long locked;
        public final long nonce;

        @JsonCreator
        public AccountType(
                @JsonProperty("address") String address,
                @JsonProperty("available") long available,
                @JsonProperty("locked") long locked,
                @JsonProperty("nonce") long nonce) {
            this.address = address;
            this.available = available;
            this.locked = locked;
            this.nonce = nonce;
        }

        public AccountType(Account account) {
            this(Hex.encode0x(account.getAddress()),
                    account.getAvailable(),
                    account.getLocked(),
                    account.getNonce());
        }
    }

    public static class BlockType {

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
        public final List<Types.TransactionType> transactions;

        public BlockType(
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
                @JsonProperty("transactions") List<Types.TransactionType> transactions) {
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

        public BlockType(Block block) {
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
                            .map(Types.TransactionType::new)
                            .collect(Collectors.toList()));
        }
    }

    public static class DelegateType {

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

        public DelegateType(BlockchainImpl.ValidatorStats validatorStats, Delegate delegate) {
            this(Hex.PREF + delegate.getAddressString(),
                    delegate.getNameString(),
                    delegate.getRegisteredAt(),
                    delegate.getVotes(),
                    validatorStats.getBlocksForged(),
                    validatorStats.getTurnsHit(),
                    validatorStats.getTurnsMissed());
        }

        public DelegateType(
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

    public static class InfoType {
        @JsonProperty("clientId")
        public final String clientId;

        @JsonProperty("coinbase")
        public final String coinbase;

        @JsonProperty("latestBlockNumber")
        public final Long latestBlockNumber;

        @JsonProperty("latestBlockHash")
        public final String latestBlockHash;

        @JsonProperty("activePeers")
        public final Integer activePeers;

        @JsonProperty("pendingTransactions")
        public final Integer pendingTransactions;

        public InfoType(
                @JsonProperty("clientId") String clientId,
                @JsonProperty("coinbase") String coinbase,
                @JsonProperty("latestBlockNumber") Long latestBlockNumber,
                @JsonProperty("latestBlockHash") String latestBlockHash,
                @JsonProperty("activePeers") Integer activePeers,
                @JsonProperty("pendingTransactions") Integer pendingTransactions) {
            this.clientId = clientId;
            this.coinbase = coinbase;
            this.latestBlockNumber = latestBlockNumber;
            this.latestBlockHash = latestBlockHash;
            this.activePeers = activePeers;
            this.pendingTransactions = pendingTransactions;
        }

        public InfoType(Kernel kernel) {
            this(kernel.getConfig().getClientId(),
                    Hex.PREF + kernel.getCoinbase(),
                    kernel.getBlockchain().getLatestBlockNumber(),
                    Hex.encode0x(kernel.getBlockchain().getLatestBlockHash()),
                    kernel.getChannelManager().getActivePeers().size(),
                    kernel.getPendingManager().getTransactions().size());
        }
    }

    public static class PeerType {

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

        @JsonProperty("capabilities")
        public final List<String> capabilities;

        public PeerType(
                @JsonProperty("ip") String ip,
                @JsonProperty("port") int port,
                @JsonProperty("networkVersion") short networkVersion,
                @JsonProperty("clientId") String clientId,
                @JsonProperty("peerId") String peerId,
                @JsonProperty("latestBlockNumber") long latestBlockNumber,
                @JsonProperty("latency") long latency,
                @JsonProperty("capabilities") List<String> capabilities) {
            this.ip = ip;
            this.port = port;
            this.networkVersion = networkVersion;
            this.clientId = clientId;
            this.peerId = peerId;
            this.latestBlockNumber = latestBlockNumber;
            this.latency = latency;
            this.capabilities = capabilities;
        }

        public PeerType(Peer peer) {
            this(peer.getIp(),
                    peer.getPort(),
                    peer.getNetworkVersion(),
                    peer.getClientId(),
                    Hex.PREF + peer.getPeerId(),
                    peer.getLatestBlockNumber(),
                    peer.getLatency(),
                    peer.getCapabilities().toList());
        }
    }

    public static class TransactionLimitsType {

        @JsonProperty("maxTransactionDataSize")
        public final Integer maxTransactionDataSize;

        @JsonProperty("minTransactionFee")
        public final Long minTransactionFee;

        @JsonProperty("minDelegateBurnAmount")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public final Long minDelegateBurnAmount;

        @JsonCreator
        public TransactionLimitsType(
                @JsonProperty("maxTransactionDataSize") Integer maxTransactionDataSize,
                @JsonProperty("minTransactionFee") Long minTransactionFee,
                @JsonProperty("minDelegateBurnAmount") Long minDelegateBurnAmount) {
            this.maxTransactionDataSize = maxTransactionDataSize;
            this.minTransactionFee = minTransactionFee;
            this.minDelegateBurnAmount = minDelegateBurnAmount;
        }
    }

    public static class TransactionType {

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

        public TransactionType(
                @JsonProperty("hash") String hash,
                @JsonProperty("type") String type,
                @JsonProperty("from") String from,
                @JsonProperty("to") String to,
                @JsonProperty("value") Long value,
                @JsonProperty("fee") Long fee,
                @JsonProperty("nonce") Long nonce,
                @JsonProperty("timestamp") Long timestamp,
                @JsonProperty("date") String date,
                @JsonProperty("data") String data) {
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

        public TransactionType(Transaction tx) {
            this(Hex.encode0x(tx.getHash()),
                    tx.getType().toString(),
                    Hex.encode0x(tx.getFrom()),
                    Hex.encode0x(tx.getTo()),
                    tx.getValue(),
                    tx.getFee(),
                    tx.getNonce(),
                    tx.getTimestamp(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(tx.getTimestamp())),
                    Hex.encode0x(tx.getData()));
        }
    }

}
