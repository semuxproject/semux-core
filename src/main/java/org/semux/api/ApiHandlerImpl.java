/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.api.exception.ApiHandlerException;
import org.semux.api.response.AddNodeResponse;
import org.semux.api.response.ApiHandlerResponse;
import org.semux.api.response.CreateAccountResponse;
import org.semux.api.response.DoTransactionResponse;
import org.semux.api.response.GetAccountResponse;
import org.semux.api.response.GetAccountTransactionsResponse;
import org.semux.api.response.GetBlockResponse;
import org.semux.api.response.GetDelegateResponse;
import org.semux.api.response.GetDelegatesResponse;
import org.semux.api.response.GetInfoResponse;
import org.semux.api.response.GetLatestBlockNumberResponse;
import org.semux.api.response.GetLatestBlockResponse;
import org.semux.api.response.GetPeersResponse;
import org.semux.api.response.GetPendingTransactionsResponse;
import org.semux.api.response.GetRootResponse;
import org.semux.api.response.GetTransactionResponse;
import org.semux.api.response.GetValidatorsResponse;
import org.semux.api.response.GetVoteResponse;
import org.semux.api.response.GetVotesResponse;
import org.semux.api.response.ListAccountsResponse;
import org.semux.api.response.SendTransactionResponse;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Semux RESTful API handler implementation.
 *
 */
public class ApiHandlerImpl implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandlerImpl.class);

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
    public String service(String uri, Map<String, String> params, HttpHeaders headers) throws ApiHandlerException {
        if ("/".equals(uri)) {
            return success(new GetRootResponse(true, "Semux API works"));
        }

        Command cmd = Command.of(uri.substring(1));
        if (cmd == null) {
            return failure("Invalid request: uri = " + uri);
        }

        try {
            switch (cmd) {
            case GET_INFO: {
                return success(new GetInfoResponse(true, new GetInfoResponse.Result(kernel)));
            }

            case GET_PEERS: {
                return success(
                        new GetPeersResponse(true, kernel.getChannelManager()
                                .getActivePeers()
                                .parallelStream()
                                .map(GetPeersResponse.Result::new).collect(Collectors.toList())));
            }

            case ADD_NODE: {
                String node = params.get("node");
                if (node != null) {
                    String[] tokens = node.trim().split(":");
                    kernel.getNodeManager().addNode(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                    return success(new AddNodeResponse(true));
                } else {
                    return failure("Invalid parameter: node can't be null");
                }
            }

            case ADD_TO_BLACKLIST: {
                return addToBlackList(params);
            }

            case ADD_TO_WHITELIST: {
                return addToWhiteList(params);
            }

            case GET_LATEST_BLOCK_NUMBER: {
                return success(new GetLatestBlockNumberResponse(true, kernel.getBlockchain().getLatestBlockNumber()));
            }

            case GET_LATEST_BLOCK: {
                return success(new GetLatestBlockResponse(true,
                        new GetBlockResponse.Result(kernel.getBlockchain().getLatestBlock())));
            }

            case GET_BLOCK: {
                String number = params.get("number");
                String hash = params.get("hash");

                if (number != null) {
                    return success(new GetBlockResponse(true,
                            new GetBlockResponse.Result(kernel.getBlockchain().getBlock(Long.parseLong(number)))));
                } else if (hash != null) {
                    return success(new GetBlockResponse(true,
                            new GetBlockResponse.Result(kernel.getBlockchain().getBlock(Hex.parse(hash)))));
                } else {
                    return failure("Invalid parameter: number or hash can't be null");
                }
            }

            case GET_PENDING_TRANSACTIONS: {
                return success(new GetPendingTransactionsResponse(true, kernel.getPendingManager()
                        .getTransactions()
                        .parallelStream()
                        .map(GetTransactionResponse.Result::new)
                        .collect(Collectors.toList())));
            }

            case GET_ACCOUNT_TRANSACTIONS: {
                String addr = params.get("address");
                String from = params.get("from");
                String to = params.get("to");
                if (addr != null && from != null && to != null) {
                    return success(new GetAccountTransactionsResponse(true, kernel.getBlockchain()
                            .getTransactions(Hex.parse(addr), Integer.parseInt(from), Integer.parseInt(to))
                            .parallelStream()
                            .map(GetTransactionResponse.Result::new)
                            .collect(Collectors.toList())));
                } else {
                    return failure("Invalid parameter: address = " + addr + ", from = " + from + ", to = " + to);
                }
            }

            case GET_TRANSACTION: {
                String hash = params.get("hash");
                if (hash != null) {
                    Transaction transaction = kernel.getBlockchain().getTransaction(Hex.parse(hash));
                    return success(
                            new GetTransactionResponse(
                                    true,
                                    new GetTransactionResponse.Result(transaction)));
                } else {
                    return failure("Invalid parameter: hash can't be null");
                }
            }

            case SEND_TRANSACTION: {
                String raw = params.get("raw");
                if (raw != null) {
                    byte[] bytes = Hex.parse(raw);
                    kernel.getPendingManager().addTransaction(Transaction.fromBytes(bytes));
                    return success(new SendTransactionResponse(true));
                } else {
                    return failure("Invalid parameter: raw can't be null");
                }
            }

            case GET_ACCOUNT: {
                String addr = params.get("address");
                if (addr != null) {
                    return success(new GetAccountResponse(
                            true,
                            new GetAccountResponse.Result(
                                    kernel.getBlockchain().getAccountState().getAccount(Hex.parse(addr)))));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }

            case GET_DELEGATE: {
                String address = params.get("address");

                if (address != null) {
                    return success(new GetDelegateResponse(
                            true,
                            new GetDelegateResponse.Result(
                                    kernel.getBlockchain().getValidatorStats(Hex.parse(address)),
                                    kernel.getBlockchain().getDelegateState()
                                            .getDelegateByAddress(Hex.parse(address)))));
                } else {
                    return failure("Invalid parameter: address can't be null");
                }
            }

            case GET_VALIDATORS: {
                return success(new GetValidatorsResponse(
                        true,
                        kernel.getBlockchain().getValidators().parallelStream()
                                .map(v -> Hex.PREF + v)
                                .collect(Collectors.toList())));
            }

            case GET_DELEGATES: {
                return success(new GetDelegatesResponse(
                        true,
                        kernel.getBlockchain()
                                .getDelegateState().getDelegates().parallelStream()
                                .map(delegate -> new GetDelegateResponse.Result(
                                        kernel.getBlockchain().getValidatorStats(delegate.getAddress()),
                                        delegate))
                                .collect(Collectors.toList())));
            }

            case GET_VOTE: {
                String voter = params.get("voter");
                String delegate = params.get("delegate");

                if (voter != null && delegate != null) {
                    return success(new GetVoteResponse(
                            true,
                            kernel.getBlockchain().getDelegateState()
                                    .getVote(Hex.parse(voter), Hex.parse(delegate))));
                } else {
                    return failure("Invalid parameter: voter = " + voter + ", delegate = " + delegate);
                }
            }

            case GET_VOTES: {
                String delegate = params.get("delegate");

                if (delegate != null) {
                    return success(new GetVotesResponse(
                            true,
                            kernel.getBlockchain().getDelegateState().getVotes(Hex.parse(delegate)).entrySet()
                                    .parallelStream()
                                    .collect(Collectors.toMap(
                                            entry -> Hex.PREF + entry.getKey().toString(), Map.Entry::getValue))));
                } else {
                    return failure("Invalid parameter: delegate can't be null");
                }
            }

            case LIST_ACCOUNTS: {
                return success(new ListAccountsResponse(
                        true,
                        kernel.getWallet().getAccounts().parallelStream()
                                .map(acc -> Hex.PREF + acc.toAddressString())
                                .collect(Collectors.toList())));
            }

            case CREATE_ACCOUNT: {
                EdDSA key = new EdDSA();
                kernel.getWallet().addAccount(key);
                kernel.getWallet().flush();
                return success(new CreateAccountResponse(true, Hex.PREF + key.toAddressString()));
            }

            case TRANSFER:
            case DELEGATE:
            case VOTE:
            case UNVOTE:
                return doTransaction(cmd, params);
            }
        } catch (Exception e) {
            throw new ApiHandlerException("Internal error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }

        throw new ApiHandlerException("Not implemented: command = " + cmd, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    protected String addToBlackList(Map<String, String> params) throws ApiHandlerException {
        try {
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty");
            }

            kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());
            return success(new ApiHandlerResponse(true, null));
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    protected String addToWhiteList(Map<String, String> params) throws ApiHandlerException {
        try {
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty");
            }

            kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());
            return success(new ApiHandlerResponse(true, null));
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    protected String doTransaction(Command cmd, Map<String, String> params) throws ApiHandlerException {
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
                return success(new DoTransactionResponse(true, Hex.encode0x(tx.getHash())));
            } else {
                return failure("Transaction rejected by pending manager");
            }
        } else {
            return failure("Invalid parameters");
        }
    }

    protected String success(ApiHandlerResponse response) throws ApiHandlerException {
        try {
            return new ObjectMapper().writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            logger.error("failed to output success message", ex);
            throw new ApiHandlerException(INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Construct a failure response.
     *
     * @param message
     * @return
     */
    protected String failure(String message) throws ApiHandlerException {
        try {
            return new ObjectMapper().writeValueAsString(new ApiHandlerResponse(false, message));
        } catch (JsonProcessingException ex) {
            logger.error("failed to output error message", ex);
            throw new ApiHandlerException(INTERNAL_SERVER_ERROR);
        }
    }
}
