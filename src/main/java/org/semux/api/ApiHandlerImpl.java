/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNPROCESSABLE_ENTITY;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.WalletLockedException;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public ApiHandlerResponse service(String uri, Map<String, String> params, HttpHeaders headers)
            throws ApiHandlerException {
        if ("/".equals(uri)) {
            return new GetRootResponse(true, "Semux API works");
        }

        Command cmd = Command.of(uri.substring(1));
        if (cmd == null) {
            return failure("Invalid request: uri = " + uri, BAD_REQUEST);
        }

        try {
            switch (cmd) {
            case GET_INFO: return new GetInfoResponse(true, new GetInfoResponse.Result(kernel));

            case GET_PEERS: return new GetPeersResponse(true, kernel.getChannelManager()
                    .getActivePeers()
                    .parallelStream()
                    .map(GetPeersResponse.Result::new).collect(Collectors.toList()));

            case ADD_NODE: return addNode(params);

            case ADD_TO_BLACKLIST: return addToBlackList(params);

            case ADD_TO_WHITELIST: return addToWhiteList(params);

            case GET_LATEST_BLOCK_NUMBER: return new GetLatestBlockNumberResponse(true, kernel.getBlockchain().getLatestBlockNumber());

            case GET_LATEST_BLOCK: return new GetLatestBlockResponse(true,
                    new GetBlockResponse.Result(kernel.getBlockchain().getLatestBlock()));

            case GET_BLOCK: return getBlock(params);

            case GET_PENDING_TRANSACTIONS: return new GetPendingTransactionsResponse(true, kernel.getPendingManager()
                    .getTransactions()
                    .parallelStream()
                    .map(GetTransactionResponse.Result::new)
                    .collect(Collectors.toList()));

            case GET_ACCOUNT_TRANSACTIONS: return getAccountTransactions(params);

            case GET_TRANSACTION: return getTransaction(params);

            case SEND_TRANSACTION: return sendTransaction(params);

            case GET_ACCOUNT: return getAccount(params);

            case GET_DELEGATE: return getDelegate(params);

            case GET_VALIDATORS: return new GetValidatorsResponse(
                    true,
                    kernel.getBlockchain().getValidators().parallelStream()
                            .map(v -> Hex.PREF + v)
                            .collect(Collectors.toList()));

            case GET_DELEGATES: return new GetDelegatesResponse(
                    true,
                    kernel.getBlockchain()
                            .getDelegateState().getDelegates().parallelStream()
                            .map(delegate -> new GetDelegateResponse.Result(
                                    kernel.getBlockchain().getValidatorStats(delegate.getAddress()),
                                    delegate))
                            .collect(Collectors.toList()));

            case GET_VOTE: return getVote(params);

            case GET_VOTES: return getVotes(params);

            case LIST_ACCOUNTS: return new ListAccountsResponse(
                    true,
                    kernel.getWallet().getAccounts().parallelStream()
                            .map(acc -> Hex.PREF + acc.toAddressString())
                            .collect(Collectors.toList()));

            case CREATE_ACCOUNT: return createAccount();

            case TRANSFER:
            case DELEGATE:
            case VOTE:
            case UNVOTE:
                return doTransaction(cmd, params);
            }
        } catch (Exception e) {
            logger.error("Unhandled exception", e);
            throw new ApiHandlerException("Internal error: " + e.getMessage(), INTERNAL_SERVER_ERROR, e);
        }

        throw new ApiHandlerException("Not implemented: command = " + cmd, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    private ApiHandlerResponse addNode(Map<String, String> params) {
        String node = params.get("node");
        if (node == null) {
            return failure("Invalid parameter: node can't be null", BAD_REQUEST);
        }

        // validate node parameter
        Matcher matcher = Pattern.compile("^(?<host>.+?):(?<port>\\d+)$").matcher(node.trim());
        if (!matcher.matches()) {
            return failure("node parameter must in format of 'host:port'", BAD_REQUEST);
        }

        // validate host
        String host = matcher.group("host");
        if (host == null) {
            return failure("host is required", BAD_REQUEST);
        }

        InetAddress hostInetAddress;
        try {
            hostInetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return failure(e.getMessage(), BAD_REQUEST);
        }

        // validate port
        String port = matcher.group("port");
        if (port == null) {
            return failure("port number is required", BAD_REQUEST);
        }
        int portNumber = Integer.parseInt(port);

        // combine host and port into an InetSocketAddress object
        InetSocketAddress nodeInetSocketAddress;
        try {
            nodeInetSocketAddress = new InetSocketAddress(hostInetAddress, portNumber);
        } catch (IllegalArgumentException e) {
            return failure(e.getMessage(), BAD_REQUEST);
        }

        kernel.getNodeManager().addNode(nodeInetSocketAddress);
        return new AddNodeResponse(true);
    }

    private ApiHandlerResponse getBlock(Map<String, String> params) {
        String number = params.get("number");
        String hash = params.get("hash");
        Block block;

        if (number != null) {
            block = kernel.getBlockchain().getBlock(Long.parseLong(number));
        } else if (hash != null) {
            block = kernel.getBlockchain().getBlock(Hex.parse(hash));
        } else {
            return failure("Invalid parameter: either number or hash has to be provided", BAD_REQUEST);
        }

        if (block == null) {
            return failure("block is not found", NOT_FOUND);
        }

        return new GetBlockResponse(true, new GetBlockResponse.Result(block));
    }

    private ApiHandlerResponse getAccountTransactions(Map<String, String> params) {
        String addr = params.get("address");
        String from = params.get("from");
        String to = params.get("to");
        if (addr != null && from != null && to != null) {
            return new GetAccountTransactionsResponse(true, kernel.getBlockchain()
                    .getTransactions(Hex.parse(addr), Integer.parseInt(from), Integer.parseInt(to))
                    .parallelStream()
                    .map(GetTransactionResponse.Result::new)
                    .collect(Collectors.toList()));
        } else {
            return failure(
                    "Invalid parameter: address = " + addr + ", from = " + from + ", to = " + to,
                    BAD_REQUEST
            );
        }
    }

    private ApiHandlerResponse getTransaction(Map<String, String> params) {
        String hash = params.get("hash");
        if (hash == null) {
            return failure("Invalid parameter: hash can't be null", BAD_REQUEST);
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.parse(hash);
        } catch (CryptoException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return failure("transaction not found", NOT_FOUND);
        }

        return new GetTransactionResponse(true, new GetTransactionResponse.Result(transaction));
    }

    private ApiHandlerResponse sendTransaction(Map<String, String> params) {
        try {
            String raw = params.get("raw");
            if (raw == null) {
                return failure("Invalid parameter: raw can't be null", BAD_REQUEST);
            }

            kernel.getPendingManager().addTransaction(Transaction.fromBytes(Hex.parse(raw)));
            return new SendTransactionResponse(true);
        } catch (CryptoException e) {
            return failure(e.getMessage(), BAD_REQUEST);
        }
    }

    private ApiHandlerResponse getAccount(Map<String, String> params) {
        String address = params.get("address");
        if (address == null) {
            return failure("Invalid parameter: address can't be null", BAD_REQUEST);
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.parse(address);
        } catch (CryptoException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }

        Account account = kernel.getBlockchain().getAccountState().getAccount(addressBytes);
        if (account == null) {
            return failure("provided address doesn't exist in the wallet", BAD_REQUEST);
        }

        return new GetAccountResponse(true, new GetAccountResponse.Result(account));
    }

    private ApiHandlerResponse addToBlackList(Map<String, String> params) {
        try {
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty", BAD_REQUEST);
            }

            kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());
            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }
    }

    private ApiHandlerResponse addToWhiteList(Map<String, String> params) {
        try {
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty", BAD_REQUEST);
            }

            kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());
            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }
    }

    private ApiHandlerResponse getDelegate(Map<String, String> params) {
        String address = params.get("address");
        if (address == null) {
            return failure("Invalid parameter: address can't be null", BAD_REQUEST);
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.parse(address);
        } catch (CryptoException e) {
            return failure(e.getMessage(), BAD_REQUEST);
        }

        Delegate delegate = kernel.getBlockchain().getDelegateState().getDelegateByAddress(addressBytes);
        if (delegate == null) {
            return failure("Invalid parameter: provided address is not a delegate", NOT_FOUND);
        }

        BlockchainImpl.ValidatorStats validatorStats = kernel.getBlockchain().getValidatorStats(addressBytes);

        return new GetDelegateResponse(true, new GetDelegateResponse.Result(validatorStats, delegate));
    }

    private ApiHandlerResponse getVote(Map<String, String> params) {
        String voter = params.get("voter");
        String delegate = params.get("delegate");

        if (voter == null) {
            return failure("parameter 'voter' is required", BAD_REQUEST);
        }


        if (delegate == null) {
            return failure("parameter 'delegate' is required", BAD_REQUEST);
        }

        return new GetVoteResponse(
                true,
                kernel.getBlockchain().getDelegateState()
                        .getVote(Hex.parse(voter), Hex.parse(delegate)));
    }

    private ApiHandlerResponse getVotes(Map<String, String> params) {
        String delegate = params.get("delegate");
        if (delegate == null) {
            return failure("Invalid parameter: delegate can't be null", BAD_REQUEST);
        }

        return new GetVotesResponse(
                true,
                kernel.getBlockchain().getDelegateState().getVotes(Hex.parse(delegate)).entrySet()
                        .parallelStream()
                        .collect(Collectors.toMap(
                                entry -> Hex.PREF + entry.getKey().toString(),
                                Map.Entry::getValue)));
    }

    private ApiHandlerResponse createAccount() {
        try {
            EdDSA key = new EdDSA();
            kernel.getWallet().addAccount(key);
            kernel.getWallet().flush();
            return new CreateAccountResponse(true, Hex.PREF + key.toAddressString());
        } catch (WalletLockedException e) {
            return failure(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private ApiHandlerResponse doTransaction(Command cmd, Map<String, String> params) {
        String pFrom = params.get("from");
        String pTo = params.get("to");
        String pValue = params.get("value");
        String pFee = params.get("fee");
        String pData = params.get("data");

        // [1] check if kernel.getWallet().is unlocked
        if (!kernel.getWallet().unlocked()) {
            return failure("Wallet is locked", INTERNAL_SERVER_ERROR);
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
            return failure("Unsupported transaction type: " + cmd, BAD_REQUEST);
        }

        // [3] parse parameters
        if (pFrom == null) {
            return failure("parameter 'from' is required", BAD_REQUEST);
        }

        if (pFee == null) {
            return failure("parameter 'fee' is required", BAD_REQUEST);
        }

        if (type != TransactionType.DELEGATE) {
            if (pTo == null) {
                return failure("parameter 'pTo' is required", BAD_REQUEST);
            }

            if (pValue == null) {
                return failure("parameter 'pValue' is required", BAD_REQUEST);
            }
        }

        // from address
        EdDSA from = kernel.getWallet().getAccount(Hex.parse(pFrom));
        if (from == null) {
            return failure("Invalid parameter: from = " + pFrom, BAD_REQUEST);
        }

        // to address
        byte[] to = (type == TransactionType.DELEGATE) ? from.toAddress() : Hex.parse(pTo);
        if (to == null) {
            return failure("Invalid parameter: to = " + pTo, BAD_REQUEST);
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
            return new DoTransactionResponse(true, Hex.encode0x(tx.getHash()));
        } else {
            return failure("Transaction rejected by pending manager", UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Construct a failure response.
     *
     * @param message
     * @return
     */
    protected ApiHandlerResponse failure(String message, HttpResponseStatus status) {
        return new ApiHandlerResponse(false, message, status);
    }
}
