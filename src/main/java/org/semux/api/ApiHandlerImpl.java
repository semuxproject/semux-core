/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl.ValidatorStats;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Wallet;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.Peer;
import org.semux.net.PeerClient;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * Semux RESTful API handler implementation.
 *
 */
public class ApiHandlerImpl implements ApiHandler {

    private Wallet wallet;
    private Blockchain chain;
    private ChannelManager channelMgr;
    private PendingManager pendingMgr;
    private NodeManager nodeMgr;
    private PeerClient client;

    private Config config;

    /**
     * Create an API handler.
     * 
     * @param kernel
     */
    public ApiHandlerImpl(Kernel kernel) {
        this.wallet = kernel.getWallet();
        this.chain = kernel.getBlockchain();
        this.channelMgr = kernel.getChannelManager();
        this.pendingMgr = kernel.getPendingManager();
        this.nodeMgr = kernel.getNodeManager();
        this.client = kernel.getPeerClient();

        this.config = kernel.getConfig();
    }

    @Override
    public String service(String uri, Map<String, String> params, HttpHeaders headers) {
        if ("/".equals(uri)) {
            return success("Semux API works");
        }

        Command cmd = Command.of(uri.substring(1));
        if (cmd == null) {
            return failure("Invalid request: uri = " + uri);
        }

        try {
            switch (cmd) {
            case GET_INFO: {
                JSONObject obj = new JSONObject();
                obj.put("clientId", config.getClientId());
                obj.put("coinbase", Hex.PREF + client.getCoinbase());
                obj.put("latestBlockNumber", chain.getLatestBlockNumber());
                obj.put("latestBlockHash", Hex.encodeWithPrefix(chain.getLatestBlockHash()));
                obj.put("activePeers", channelMgr.getActivePeers().size());
                obj.put("pendingTransactions", pendingMgr.getTransactions().size());

                return success(obj);
            }

            case GET_PEERS: {
                JSONArray arr = new JSONArray();
                for (Peer peer : channelMgr.getActivePeers()) {
                    arr.put(peerToJson(peer));
                }

                return success(arr);
            }
            case ADD_NODE: {
                String node = params.get("node");
                if (node != null) {
                    String[] tokens = node.trim().split(":");
                    nodeMgr.addNode(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                    return success(null);
                } else {
                    return failure("Invalid parameter: node can't be null");
                }
            }
            case ADD_TO_BLACKLIST: {
                return failure("Not implmemented yet!");
            }
            case ADD_TO_WHITELIST: {
                return failure("Not implmemented yet!");
            }

            case GET_LATEST_BLOCK_NUMBER: {
                long num = chain.getLatestBlockNumber();
                return success(num);
            }
            case GET_LATEST_BLOCK: {
                Block block = chain.getLatestBlock();
                return success(blockToJson(block));
            }
            case GET_BLOCK: {
                String number = params.get("number");
                String hash = params.get("hash");

                if (number != null) {
                    return success(blockToJson(chain.getBlock(Long.parseLong(number))));
                } else if (hash != null) {
                    return success(blockToJson(chain.getBlock(Hex.parse(hash))));
                } else {
                    return failure("Invalid parameter: number or hash can't be null");
                }
            }
            case GET_PENDING_TRANSACTIONS: {
                List<Transaction> txs = pendingMgr.getTransactions();
                JSONArray arr = new JSONArray();
                for (Transaction tx : txs) {
                    arr.put(transactionToJson(tx));
                }
                return success(arr);
            }
            case GET_ACCOUNT_TRANSACTIONS: {
                String addr = params.get("address");
                String from = params.get("from");
                String to = params.get("to");
                if (addr != null && from != null && to != null) {
                    List<Transaction> txs = chain.getTransactions(Hex.parse(addr), Integer.parseInt(from),
                            Integer.parseInt(to));
                    JSONArray arr = new JSONArray();
                    for (Transaction tx : txs) {
                        arr.put(transactionToJson(tx));
                    }
                    return success(arr);
                } else {
                    return failure("Invalid parameter: address = " + addr + ", from = " + from + ", to = " + to);
                }
            }
            case GET_TRANSACTION: {
                String hash = params.get("hash");
                if (hash != null) {
                    return success(transactionToJson(chain.getTransaction(Hex.parse(hash))));
                } else {
                    return failure("Invalid parameter: hash can't be null");
                }
            }
            case SEND_TRANSACTION: {
                String raw = params.get("raw");
                if (raw != null) {
                    byte[] bytes = Hex.parse(raw);
                    pendingMgr.addTransaction(Transaction.fromBytes(bytes));
                    return success(null);
                } else {
                    return failure("Invalid parameter: raw can't be null");
                }
            }

            case GET_ACCOUNT: {
                String addr = params.get("address");
                if (addr != null) {
                    Account acc = chain.getAccountState().getAccount(Hex.parse(addr));
                    return success(accountToJson(acc));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }
            case GET_DELEGATE: {
                String address = params.get("address");

                if (address != null) {
                    Delegate d = chain.getDelegateState().getDelegateByAddress(Hex.parse(address));
                    return success(delegateToJson(d));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }
            case GET_VALIDATORS: {
                List<String> validators = chain.getValidators();
                JSONArray arr = new JSONArray();
                for (String v : validators) {
                    arr.put(Hex.PREF + v);
                }
                return success(arr);
            }
            case GET_DELEGATES: {
                List<Delegate> delegates = chain.getDelegateState().getDelegates();
                JSONArray arr = new JSONArray();
                for (Delegate d : delegates) {
                    arr.put(delegateToJson(d));
                }
                return success(arr);
            }
            case GET_VOTE: {
                String voter = params.get("voter");
                String delegate = params.get("delegate");

                if (voter != null && delegate != null) {
                    long vote = chain.getDelegateState().getVote(Hex.parse(voter), Hex.parse(delegate));
                    return success(vote);
                } else {
                    return failure("Invalid parameter: voter = " + voter + ", delegate = " + delegate);
                }
            }
            case GET_VOTES: {
                String delegate = params.get("delegate");

                if (delegate != null) {
                    Map<ByteArray, Long> votes = chain.getDelegateState().getVotes(Hex.parse(delegate));
                    JSONObject obj = new JSONObject();
                    for (Map.Entry<ByteArray, Long> entry : votes.entrySet()) {
                        obj.put(Hex.PREF + entry.getKey().toString(), entry.getValue());
                    }
                    return success(obj);
                } else {
                    return failure("Invalid parameter: delegate can't be null");
                }
            }

            case LIST_ACCOUNTS: {
                List<EdDSA> accounts = wallet.getAccounts();
                JSONArray arr = new JSONArray();
                for (EdDSA acc : accounts) {
                    arr.put(Hex.PREF + acc.toAddressString());
                }
                return success(arr);
            }
            case CREATE_ACCOUNT: {
                EdDSA key = new EdDSA();
                wallet.addAccount(key);
                wallet.flush();
                return success(Hex.PREF + key.toAddressString());
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

        // [1] check if wallet is unlocked
        if (!wallet.unlocked()) {
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
            EdDSA from = wallet.getAccount(Hex.parse(pFrom));
            if (from == null) {
                return failure("Invalid parameter: from = " + pFrom);
            }

            // to address
            byte[] to = (type == TransactionType.DELEGATE) ? from.toAddress() : Hex.parse(pTo);
            if (to == null) {
                return failure("Invalid parameter: to = " + pTo);
            }

            // value and fee
            long value = (type == TransactionType.DELEGATE) ? config.minDelegateFee() : Long.parseLong(pValue);
            long fee = Long.parseLong(pFee);

            // nonce, timestamp and data
            long nonce = pendingMgr.getNonce(from.toAddress());
            long timestamp = System.currentTimeMillis();
            byte[] data = (pData == null) ? Bytes.EMPTY_BYTES : Hex.parse(pData);

            // sign
            Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
            tx.sign(from);

            if (pendingMgr.addTransactionSync(tx)) {
                return success(Hex.encodeWithPrefix(tx.getHash()));
            } else {
                return failure("Transaciton rejected by pending manager");
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
    protected Object blockToJson(Block block) {
        if (block == null) {
            return JSONObject.NULL;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONObject obj = new JSONObject();
        obj.put("hash", Hex.encodeWithPrefix(block.getHash()));
        obj.put("number", block.getNumber());
        obj.put("view", block.getView());
        obj.put("coinbase", Hex.encodeWithPrefix(block.getCoinbase()));
        obj.put("prevHash", Hex.encodeWithPrefix(block.getPrevHash()));
        obj.put("timestamp", block.getTimestamp());
        obj.put("date", df.format(new Date(block.getTimestamp())));
        obj.put("transactionsRoot", Hex.encodeWithPrefix(block.getTransactionsRoot()));
        obj.put("resultsRoot", Hex.encodeWithPrefix(block.getResultsRoot()));
        obj.put("stateRoot", Hex.encodeWithPrefix(block.getStateRoot()));
        obj.put("data", Hex.encodeWithPrefix(block.getData()));
        JSONArray arr = new JSONArray();
        for (Transaction tx : block.getTransactions()) {
            arr.put(transactionToJson(tx));
        }
        obj.put("transactions", arr);

        return obj;
    }

    /**
     * Convert a transaction to JSON object.
     * 
     * @param tx
     * @return
     */
    protected Object transactionToJson(Transaction tx) {
        if (tx == null) {
            return JSONObject.NULL;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONObject obj = new JSONObject();
        obj.put("hash", Hex.encodeWithPrefix(tx.getHash()));
        obj.put("type", tx.getType().toString());
        obj.put("from", Hex.encodeWithPrefix(tx.getFrom()));
        obj.put("to", Hex.encodeWithPrefix(tx.getTo()));
        obj.put("value", tx.getValue());
        obj.put("fee", tx.getFee());
        obj.put("nonce", tx.getNonce());
        obj.put("timestamp", tx.getTimestamp());
        obj.put("date", df.format(new Date(tx.getTimestamp())));
        obj.put("data", Hex.encodeWithPrefix(tx.getData()));

        return obj;
    }

    /**
     * Convert an account to JSON object.
     * 
     * @param acc
     * @return
     */
    protected Object accountToJson(Account acc) {
        if (acc == null) {
            return JSONObject.NULL;
        }

        JSONObject obj = new JSONObject();
        obj.put("address", Hex.encodeWithPrefix(acc.getAddress()));
        obj.put("available", acc.getAvailable());
        obj.put("locked", acc.getLocked());
        obj.put("nonce", acc.getNonce());

        return obj;
    }

    /**
     * Convert a delegate to JSON object.
     * 
     * @param delegate
     * @return
     */
    protected Object delegateToJson(Delegate delegate) {
        if (delegate == null) {
            return JSONObject.NULL;
        }

        JSONObject obj = new JSONObject();
        obj.put("address", Hex.encodeWithPrefix(delegate.getAddress()));
        obj.put("name", new String(delegate.getName()));
        obj.put("votes", delegate.getVotes());
        obj.put("registeredAt", delegate.getRegisteredAt());
        ValidatorStats s = chain.getValidatorStats(delegate.getAddress());
        obj.put("blocksForged", s.getBlocksForged());
        obj.put("turnsHit", s.getTurnsHit());
        obj.put("turnsMissed", s.getTurnsMissed());

        return obj;
    }

    protected Object peerToJson(Peer peer) {
        if (peer == null) {
            return JSONObject.NULL;
        }

        JSONObject obj = new JSONObject();
        obj.put("ip", peer.getIp());
        obj.put("port", peer.getPort());
        obj.put("p2pVersion", peer.getNetworkVersion());
        obj.put("clientId", peer.getClientId());
        obj.put("peerId", Hex.PREF + peer.getPeerId());
        obj.put("latestBlockNumber", peer.getLatestBlockNumber());
        obj.put("latency", peer.getLatency());

        return obj;
    }

    /**
     * Construct a success response.
     * 
     * @param result
     * @return
     */
    protected String success(Object result) {
        JSONObject obj = new JSONObject();
        obj.put("success", true);
        if (result != null) {
            obj.put("result", result);
        }
        return obj.toString();
    }

    /**
     * Construct a failure response.
     * 
     * @param msg
     * @return
     */
    protected String failure(String msg) {
        JSONObject obj = new JSONObject();
        obj.put("success", false);
        if (msg != null) {
            obj.put("message", msg);
        }
        return obj.toString();
    }
}