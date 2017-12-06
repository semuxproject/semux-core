/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonCollectors;

import org.semux.Kernel;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.net.Peer;
import org.semux.util.Bytes;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * Semux RESTful API handler implementation.
 *
 */
public class ApiHandlerImpl implements ApiHandler {

    private Kernel kernel;

    /**
     * Create an API handler.
     *
     * @param kernel
     */
    public ApiHandlerImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public String service(String uri, Map<String, String> params, HttpHeaders headers) {
        if ("/".equals(uri)) {
            return success(Json.createValue("Semux API works"));
        }

        Command cmd = Command.of(uri.substring(1));
        if (cmd == null) {
            return failure("Invalid request: uri = " + uri);
        }

        try {
            switch (cmd) {
            case GET_INFO: {
                return success(Json.createObjectBuilder().add("clientId", kernel.getConfig().getClientId())
                        .add("coinbase", Hex.PREF + kernel.getCoinbase())
                        .add("latestBlockNumber", kernel.getBlockchain().getLatestBlockNumber())
                        .add("latestBlockHash", Hex.encodeWithPrefix(kernel.getBlockchain().getLatestBlockHash()))
                        .add("activePeers", kernel.getChannelManager().getActivePeers().size())
                        .add("pendingTransactions", kernel.getPendingManager().getTransactions().size()).build());
            }

            case GET_PEERS: {
                return success(kernel.getChannelManager().getActivePeers().stream().map(this::peerToJson)
                        .collect(JsonCollectors.toJsonArray()));
            }
            case ADD_NODE: {
                String node = params.get("node");
                if (node != null) {
                    String[] tokens = node.trim().split(":");
                    kernel.getNodeManager().addNode(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                    return success(JsonValue.NULL);
                } else {
                    return failure("Invalid parameter: node can't be null");
                }
            }
            case ADD_TO_BLACKLIST: {
                try {
                    String ip = params.get("ip");
                    if (ip == null || ip.trim().length() == 0) {
                        return failure("Invalid parameter: ip can't be empty");
                    }

                    kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());
                    return success(JsonValue.NULL);
                } catch (UnknownHostException | IllegalArgumentException ex) {
                    return failure(ex.getMessage());
                }
            }
            case ADD_TO_WHITELIST: {
                try {
                    String ip = params.get("ip");
                    if (ip == null || ip.trim().length() == 0) {
                        return failure("Invalid parameter: ip can't be empty");
                    }

                    kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());
                    return success(JsonValue.NULL);
                } catch (UnknownHostException | IllegalArgumentException ex) {
                    return failure(ex.getMessage());
                }
            }

            case GET_LATEST_BLOCK_NUMBER: {
                return success(Json.createValue(kernel.getBlockchain().getLatestBlockNumber()));
            }
            case GET_LATEST_BLOCK: {
                Block block = kernel.getBlockchain().getLatestBlock();
                return success(blockToJson(block));
            }
            case GET_BLOCK: {
                String number = params.get("number");
                String hash = params.get("hash");

                if (number != null) {
                    return success(blockToJson(kernel.getBlockchain().getBlock(Long.parseLong(number))));
                } else if (hash != null) {
                    return success(blockToJson(kernel.getBlockchain().getBlock(Hex.parse(hash))));
                } else {
                    return failure("Invalid parameter: number or hash can't be null");
                }
            }
            case GET_PENDING_TRANSACTIONS: {
                return success(kernel.getPendingManager().getTransactions().stream().map(this::transactionToJson)
                        .collect(JsonCollectors.toJsonArray()));
            }
            case GET_ACCOUNT_TRANSACTIONS: {
                String addr = params.get("address");
                String from = params.get("from");
                String to = params.get("to");
                if (addr != null && from != null && to != null) {
                    return success(kernel.getBlockchain()
                            .getTransactions(Hex.parse(addr), Integer.parseInt(from), Integer.parseInt(to)).stream()
                            .map(this::transactionToJson).collect(JsonCollectors.toJsonArray()));
                } else {
                    return failure("Invalid parameter: address = " + addr + ", from = " + from + ", to = " + to);
                }
            }
            case GET_TRANSACTION: {
                String hash = params.get("hash");
                if (hash != null) {
                    return success(transactionToJson(kernel.getBlockchain().getTransaction(Hex.parse(hash))));
                } else {
                    return failure("Invalid parameter: hash can't be null");
                }
            }
            case SEND_TRANSACTION: {
                String raw = params.get("raw");
                if (raw != null) {
                    byte[] bytes = Hex.parse(raw);
                    kernel.getPendingManager().addTransaction(Transaction.fromBytes(bytes));
                    return success(JsonValue.NULL);
                } else {
                    return failure("Invalid parameter: raw can't be null");
                }
            }

            case GET_ACCOUNT: {
                String addr = params.get("address");
                if (addr != null) {
                    Account acc = kernel.getBlockchain().getAccountState().getAccount(Hex.parse(addr));
                    return success(accountToJson(acc));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }
            case GET_DELEGATE: {
                String address = params.get("address");

                if (address != null) {
                    Delegate d = kernel.getBlockchain().getDelegateState().getDelegateByAddress(Hex.parse(address));
                    return success(delegateToJson(d));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }
            case GET_VALIDATORS: {
                return success(kernel.getBlockchain().getValidators().stream().map(v -> Json.createValue(Hex.PREF + v))
                        .collect(JsonCollectors.toJsonArray()));
            }
            case GET_DELEGATES: {
                return success(kernel.getBlockchain().getDelegateState().getDelegates().stream()
                        .map(this::delegateToJson).collect(JsonCollectors.toJsonArray()));
            }
            case GET_VOTE: {
                String voter = params.get("voter");
                String delegate = params.get("delegate");

                if (voter != null && delegate != null) {
                    return success(Json.createValue(
                            kernel.getBlockchain().getDelegateState().getVote(Hex.parse(voter), Hex.parse(delegate))));
                } else {
                    return failure("Invalid parameter: voter = " + voter + ", delegate = " + delegate);
                }
            }
            case GET_VOTES: {
                String delegate = params.get("delegate");

                if (delegate != null) {
                    return success(
                            kernel.getBlockchain().getDelegateState().getVotes(Hex.parse(delegate)).entrySet().stream()
                                    .map(entry -> new SimpleEntry<String, JsonValue>(
                                            Hex.PREF + entry.getKey().toString(), Json.createValue(entry.getValue())))
                                    .collect(JsonCollectors.toJsonObject()));
                } else {
                    return failure("Invalid parameter: delegate can't be null");
                }
            }

            case LIST_ACCOUNTS: {
                return success(kernel.getWallet().getAccounts().stream()
                        .map(acc -> Json.createValue(Hex.PREF + acc.toAddressString()))
                        .collect(JsonCollectors.toJsonArray()));
            }
            case CREATE_ACCOUNT: {
                EdDSA key = new EdDSA();
                kernel.getWallet().addAccount(key);
                kernel.getWallet().flush();
                return success(Json.createValue(Hex.PREF + key.toAddressString()));
            }
            case TRANSFER:
            case DELEGATE:
            case VOTE:
            case UNVOTE:
                return doTransaction(cmd, params);
            }
        } catch (Exception e) {
            return failure("Internal error: " + e.getMessage());
        }

        return failure("Not implemented: command = " + cmd);
    }

    protected String doTransaction(Command cmd, Map<String, String> params) {
        String pFrom = params.get("from");
        String pTo = params.get("to");
        String pValue = params.get("value");
        String pFee = params.get("fee");
        String pData = params.get("data");

        // [1] check if kernel.getWallet().is unlocked
        if (!kernel.getWallet().unlocked()) {
            return failure("Wallet is locked");
        }

        // [2] parse transaction type
        TransactionType type;
        switch (cmd) {
        case TRANSFER:
            type = TransactionType.TRANSFER;
            break;
        case DELEGATE:
            type = TransactionType.DELEGATE;
            break;
        case VOTE:
            type = TransactionType.VOTE;
            break;
        case UNVOTE:
            type = TransactionType.UNVOTE;
            break;
        default:
            return failure("Unsupported transaction type: " + cmd);
        }

        // [3] parse parameters
        if (pFrom != null //
                && (type == TransactionType.DELEGATE || pTo != null) //
                && (type == TransactionType.DELEGATE || pValue != null) //
                && pFee != null) {
            // from address
            EdDSA from = kernel.getWallet().getAccount(Hex.parse(pFrom));
            if (from == null) {
                return failure("Invalid parameter: from = " + pFrom);
            }

            // to address
            byte[] to = (type == TransactionType.DELEGATE) ? from.toAddress() : Hex.parse(pTo);
            if (to == null) {
                return failure("Invalid parameter: to = " + pTo);
            }

            // value and fee
            long value = (type == TransactionType.DELEGATE) ? kernel.getConfig().minDelegateFee()
                    : Long.parseLong(pValue);
            long fee = Long.parseLong(pFee);

            // nonce, timestamp and data
            long nonce = kernel.getPendingManager().getNonce(from.toAddress());
            long timestamp = System.currentTimeMillis();
            byte[] data = (pData == null) ? Bytes.EMPTY_BYTES : Hex.parse(pData);

            // sign
            Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
            tx.sign(from);

            if (kernel.getPendingManager().addTransactionSync(tx)) {
                return success(Json.createValue(Hex.encodeWithPrefix(tx.getHash())));
            } else {
                return failure("Transaction rejected by pending manager");
            }
        } else {
            return failure("Invalid parameters");
        }
    }

    /**
     * Convert a block to JSON object.
     *
     * @param block
     * @return
     */
    protected JsonValue blockToJson(Block block) {
        if (block == null) {
            return JsonObject.NULL;
        }

        return Json.createObjectBuilder().add("hash", Hex.encodeWithPrefix(block.getHash()))
                .add("number", block.getNumber()).add("view", block.getView())
                .add("coinbase", Hex.encodeWithPrefix(block.getCoinbase()))
                .add("prevHash", Hex.encodeWithPrefix(block.getPrevHash())).add("timestamp", block.getTimestamp())
                .add("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(block.getTimestamp())))
                .add("transactionsRoot", Hex.encodeWithPrefix(block.getTransactionsRoot()))
                .add("resultsRoot", Hex.encodeWithPrefix(block.getResultsRoot()))
                .add("stateRoot", Hex.encodeWithPrefix(block.getStateRoot()))
                .add("data", Hex.encodeWithPrefix(block.getData())).add("transactions", Json.createArrayBuilder(block
                        .getTransactions().stream().map(this::transactionToJson).collect(JsonCollectors.toJsonArray())))
                .build();
    }

    /**
     * Convert a transaction to JSON object.
     *
     * @param tx
     * @return
     */
    protected JsonValue transactionToJson(Transaction tx) {
        if (tx == null) {
            return JsonObject.NULL;
        }

        return Json.createObjectBuilder().add("hash", Hex.encodeWithPrefix(tx.getHash()))
                .add("type", tx.getType().toString()).add("from", Hex.encodeWithPrefix(tx.getFrom()))
                .add("to", Hex.encodeWithPrefix(tx.getTo())).add("value", tx.getValue()).add("fee", tx.getFee())
                .add("nonce", tx.getNonce()).add("timestamp", tx.getTimestamp())
                .add("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(tx.getTimestamp())))
                .add("data", Hex.encodeWithPrefix(tx.getData())).build();
    }

    /**
     * Convert an account to JSON object.
     *
     * @param acc
     * @return
     */
    protected JsonValue accountToJson(Account acc) {
        if (acc == null) {
            return JsonObject.NULL;
        }

        return Json.createObjectBuilder().add("address", Hex.encodeWithPrefix(acc.getAddress()))
                .add("available", acc.getAvailable()).add("locked", acc.getLocked()).add("nonce", acc.getNonce())
                .build();
    }

    /**
     * Convert a delegate to JSON object.
     *
     * @param delegate
     * @return
     */
    protected JsonValue delegateToJson(Delegate delegate) {
        if (delegate == null) {
            return JsonObject.NULL;
        }

        BlockchainImpl.ValidatorStats s = kernel.getBlockchain().getValidatorStats(delegate.getAddress());

        return Json.createObjectBuilder().add("address", Hex.encodeWithPrefix(delegate.getAddress()))
                .add("name", new String(delegate.getName())).add("votes", delegate.getVotes())
                .add("registeredAt", delegate.getRegisteredAt()).add("blocksForged", s.getBlocksForged())
                .add("turnsHit", s.getTurnsHit()).add("turnsMissed", s.getTurnsMissed()).build();
    }

    protected JsonValue peerToJson(Peer peer) {
        if (peer == null) {
            return JsonObject.NULL;
        }

        return Json.createObjectBuilder().add("ip", peer.getIp()).add("port", peer.getPort())
                .add("networkVersion", peer.getNetworkVersion()).add("clientId", peer.getClientId())
                .add("peerId", Hex.PREF + peer.getPeerId()).add("latestBlockNumber", peer.getLatestBlockNumber())
                .add("latency", peer.getLatency()).build();
    }

    /**
     * Construct a success response.
     *
     * @param result
     * @return
     */
    protected String success(JsonValue result) {
        return Json.createObjectBuilder().add("success", true).add("result", result).build().toString();
    }

    /**
     * Construct a failure response.
     *
     * @param msg
     * @return
     */
    protected String failure(String msg) {
        return Json.createObjectBuilder().add("success", false).add("message", msg).build().toString();
    }
}
