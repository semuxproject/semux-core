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
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpResponseStatus.UNPROCESSABLE_ENTITY;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
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
import org.semux.api.transaction.TransactionBuilder;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.exception.WalletLockedException;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Semux RESTful API handler implementation.
 * 
 * @TODO: Auto-generate API docs
 */
public class ApiHandlerImpl implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandlerImpl.class);

    private final Kernel kernel;

    /**
     * Required parameters of each type of transaction
     */
    private static final EnumMap<TransactionType, List<String>> TRANSACTION_REQUIRED_PARAMS = new EnumMap<>(
            TransactionType.class);
    static {
        TRANSACTION_REQUIRED_PARAMS.put(TransactionType.TRANSFER, Arrays.asList("from", "to", "value", "fee"));
        TRANSACTION_REQUIRED_PARAMS.put(TransactionType.TRANSFER_MANY, Arrays.asList("from", "to[]", "value", "fee"));
        TRANSACTION_REQUIRED_PARAMS.put(TransactionType.DELEGATE, Arrays.asList("from", "fee"));
        TRANSACTION_REQUIRED_PARAMS.put(TransactionType.VOTE, Arrays.asList("from", "to", "value", "fee"));
        TRANSACTION_REQUIRED_PARAMS.put(TransactionType.UNVOTE, Arrays.asList("from", "to", "value", "fee"));
    }

    /**
     * Create an API handler.
     *
     * @param kernel
     */
    public ApiHandlerImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public ApiHandlerResponse service(String uri, Map<String, Object> params, HttpHeaders headers)
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
            case GET_INFO:
                return getInfo();

            case GET_PEERS:
                return getPeers();

            case ADD_NODE:
                return addNode(params);

            case ADD_TO_BLACKLIST:
                return addToBlackList(params);

            case ADD_TO_WHITELIST:
                return addToWhiteList(params);

            case GET_LATEST_BLOCK_NUMBER:
                return getLatestBlockNumber();

            case GET_LATEST_BLOCK:
                return getLatestBlock();

            case GET_BLOCK:
                return getBlock(params);

            case GET_PENDING_TRANSACTIONS:
                return getPendingTransactions();

            case GET_ACCOUNT_TRANSACTIONS:
                return getAccountTransactions(params);

            case GET_TRANSACTION:
                return getTransaction(params);

            case SEND_TRANSACTION:
                return sendTransaction(params);

            case GET_ACCOUNT:
                return getAccount(params);

            case GET_DELEGATE:
                return getDelegate(params);

            case GET_VALIDATORS:
                return getValidators();

            case GET_DELEGATES:
                return getDelegates();

            case GET_VOTE:
                return getVote(params);

            case GET_VOTES:
                return getVotes(params);

            case LIST_ACCOUNTS:
                return listAccounts();

            case CREATE_ACCOUNT:
                return createAccount();

            case TRANSFER:
            case TRANSFER_MANY:
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

    /**
     * GET /get_info
     *
     * @return
     */
    private ApiHandlerResponse getInfo() {
        return new GetInfoResponse(true, new GetInfoResponse.Result(kernel));
    }

    /**
     * GET /get_peers
     *
     * @return
     */
    private ApiHandlerResponse getPeers() {
        return new GetPeersResponse(true, kernel.getChannelManager()
                .getActivePeers()
                .parallelStream()
                .map(GetPeersResponse.Result::new).collect(Collectors.toList()));
    }

    /**
     * GET /add_node?node
     *
     * @param params
     * @return result
     */
    private ApiHandlerResponse addNode(Map<String, String> params) {
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(params.get("node")));
            return new AddNodeResponse(true);
        } catch (IllegalArgumentException e) {
            return failure(e.getMessage(), BAD_REQUEST);
        }
    }

    /**
     * Validate node parameter of /add_node API
     *
     * @param node
     *            node parameter of /add_node API
     * @return validated hostname and port number
     */
    private InetSocketAddress validateAddNodeParameter(String node) {
        if (node == null || node.length() == 0) {
            throw new IllegalArgumentException("Invalid parameter: node can't be empty");
        }

        Matcher matcher = Pattern.compile("^(?<host>.+?):(?<port>\\d+)$").matcher(node.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("node parameter must in format of 'host:port'");
        }

        try {
            return new InetSocketAddress(
                    InetAddress.getByName(matcher.group("host")),
                    Integer.parseInt(matcher.group("port")));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * GET /get_block?number&hash
     *
     * @param params
     * @return
     */
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

    /**
     * GET /get_pending_transactions
     *
     * @return
     */
    private ApiHandlerResponse getPendingTransactions() {
        return new GetPendingTransactionsResponse(true, kernel.getPendingManager()
                .getTransactions()
                .parallelStream()
                .map(GetTransactionResponse.Result::new)
                .collect(Collectors.toList()));
    }

    /**
     * GET /get_account_transactions?address&from&to
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse getAccountTransactions(Map<String, String> params) {
        String address = params.get("address");
        String from = params.get("from");
        String to = params.get("to");
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (address == null) {
            return failure("address is required", BAD_REQUEST);
        }

        try {
            addressBytes = Hex.parse(address);
        } catch (CryptoException ex) {
            return failure("address is not a valid hexadecimal string", BAD_REQUEST);
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return failure("from is not a valid integer", BAD_REQUEST);
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return failure("to is not a valid integer", BAD_REQUEST);
        }

        return new GetAccountTransactionsResponse(true, kernel.getBlockchain()
                .getTransactions(addressBytes, fromInt, toInt)
                .parallelStream()
                .map(GetTransactionResponse.Result::new)
                .collect(Collectors.toList()));
    }

    /**
     * GET /get_transaction?hash
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse getTransaction(Map<String, String> params) {
        String hash = params.get("hash");
        if (hash == null) {
            return failure("Invalid parameter: hash can't be null", BAD_REQUEST);
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.parse(hash);
        } catch (CryptoException ex) {
            return failure("hash parameter is not a valid hexadecimal string", BAD_REQUEST);
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return failure("transaction not found", NOT_FOUND);
        }

        return new GetTransactionResponse(true, new GetTransactionResponse.Result(transaction));
    }

    /**
     * GET /send_transaction?raw
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse sendTransaction(Map<String, String> params) {
        try {
            String raw = params.get("raw");
            if (raw == null) {
                return failure("Invalid parameter: raw can't be null", BAD_REQUEST);
            }

            kernel.getPendingManager().addTransaction(Transaction.fromBytes(Hex.parse(raw)));
            return new SendTransactionResponse(true);
        } catch (CryptoException e) {
            return failure("parameter 'raw' is not a valid hexadecimal string", BAD_REQUEST);
        }
    }

    /**
     * GET /get_account?address
     *
     * @param params
     * @return
     */
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
        return new GetAccountResponse(true, new GetAccountResponse.Result(account));
    }

    /**
     * GET /add_to_blacklist?ip
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse addToBlackList(Map<String, String> params) {
        try {
            String ip = (String) params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty", BAD_REQUEST);
            }

            kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());
            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }
    }

    /**
     * GET /add_to_whitelist?ip
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse addToWhiteList(Map<String, String> params) {
        try {
            String ip = (String) params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Invalid parameter: ip can't be empty", BAD_REQUEST);
            }

            kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());
            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }
    }

    /**
     * GET /get_latest_block_number
     *
     * @return
     */
    private ApiHandlerResponse getLatestBlockNumber() {
        return new GetLatestBlockNumberResponse(true, kernel.getBlockchain().getLatestBlockNumber());
    }

    /**
     * GET /get_latest_block
     *
     * @return
     */
    private ApiHandlerResponse getLatestBlock() {
        return new GetLatestBlockResponse(true,
                new GetBlockResponse.Result(kernel.getBlockchain().getLatestBlock()));
    }

    /**
     * GET /get_delegate?address
     *
     * @param params
     * @return
     */
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

    /**
     * GET /get_validators
     *
     * @return
     */
    private ApiHandlerResponse getValidators() {
        return new GetValidatorsResponse(
                true,
                kernel.getBlockchain().getValidators().parallelStream()
                        .map(v -> Hex.PREF + v)
                        .collect(Collectors.toList()));
    }

    /**
     * GET /get_delegates
     *
     * @return
     */
    private ApiHandlerResponse getDelegates() {
        return new GetDelegatesResponse(
                true,
                kernel.getBlockchain()
                        .getDelegateState().getDelegates().parallelStream()
                        .map(delegate -> new GetDelegateResponse.Result(
                                kernel.getBlockchain().getValidatorStats(delegate.getAddress()),
                                delegate))
                        .collect(Collectors.toList()));
    }

    /**
     * GET /get_vote?voter&delegate
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse getVote(Map<String, String> params) {
        String voter = params.get("voter");
        String delegate = params.get("delegate");
        byte[] voterBytes;
        byte[] delegateBytes;

        if (voter == null) {
            return failure("parameter 'voter' is required", BAD_REQUEST);
        }

        try {
            voterBytes = Hex.parse(voter);
        } catch (CryptoException ex) {
            return failure("parameter 'voter' is not a valid hexadecimal string", BAD_REQUEST);
        }

        if (delegate == null) {
            return failure("parameter 'delegate' is required", BAD_REQUEST);
        }

        try {
            delegateBytes = Hex.parse(delegate);
        } catch (CryptoException ex) {
            return failure("parameter 'delegate' is not a valid hexadecimal string", BAD_REQUEST);
        }

        return new GetVoteResponse(
                true,
                kernel.getBlockchain().getDelegateState()
                        .getVote(voterBytes, delegateBytes));
    }

    /**
     * GET /get_votes?delegate
     *
     * @param params
     * @return
     */
    private ApiHandlerResponse getVotes(Map<String, String> params) {
        String delegate = params.get("delegate");
        if (delegate == null) {
            return failure("Invalid parameter: delegate can't be null", BAD_REQUEST);
        }

        byte[] delegateBytes;
        try {
            delegateBytes = Hex.parse(delegate);
        } catch (CryptoException ex) {
            return failure("delegate is not a valid hexadecimal string", BAD_REQUEST);
        }

        return new GetVotesResponse(
                true,
                kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                        .collect(Collectors.toMap(entry -> Hex.PREF + entry.getKey().toString(), Map.Entry::getValue)));
    }

    /**
     * GET /list_accounts
     *
     * @return
     */
    private ApiHandlerResponse listAccounts() {
        return new ListAccountsResponse(
                true,
                kernel.getWallet().getAccounts().parallelStream()
                        .map(acc -> Hex.PREF + acc.toAddressString())
                        .collect(Collectors.toList()));
    }

    /**
     * GET /create_account
     *
     * @return
     */
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

    /**
     * This method processes the following transaction-related endpoints:
     *
     * <ul>
     * <li>GET /transfer?from&to&value&fee&data</li>
     * <li>GET /delegate?from&fee&data</li>
     * <li>GET /vote?from&to&value&fee&data</li>
     * <li>GET /unvote?from&to&value&fee&data</li>
     * </ul>
     *
     * @param cmd
     *            type of transaction
     * @param params
     * @return
     */
    private ApiHandlerResponse doTransaction(Command cmd, Map<String, Object> params) {
        // [1] check if kernel.getWallet().is unlocked
        if (!kernel.getWallet().isUnlocked()) {
            return failure("Wallet is locked", INTERNAL_SERVER_ERROR);
        }

        // [2] parse transaction type
        TransactionType type;
        switch (cmd) {
        case TRANSFER:
            type = TransactionType.TRANSFER;
            break;
        case TRANSFER_MANY:
            type = TransactionType.TRANSFER_MANY;
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
            return failure("Unsupported transaction type: " + cmd.toString(), NOT_IMPLEMENTED);
        }

        // [3] build and send the transaction to PendingManager
        try {
            Transaction tx = new TransactionBuilder(kernel)
                    .withType(type)
                    .withFrom((String) params.get("from"))
                    .withTo((String) params.get("to"))
                    .withValue((String) params.get("value"))
                    .withFee((String) params.get("fee"))
                    .withData((String) params.get("data"))
                    .build();

            if (kernel.getPendingManager().addTransactionSync(tx)) {
                return new DoTransactionResponse(true, null, Hex.encode0x(tx.getHash()));
            } else {
                // TODO: report the actual reason of rejection
                return failure("Transaction rejected by pending manager", UNPROCESSABLE_ENTITY);
            }
        } catch (IllegalArgumentException ex) {
            return failure(ex.getMessage(), BAD_REQUEST);
        }
    }

    /**
     * Construct a failure response.
     *
     * @param message
     * @return
     */
    private ApiHandlerResponse failure(String message, HttpResponseStatus status) {
        return new ApiHandlerResponse(false, message, status);
    }
}
