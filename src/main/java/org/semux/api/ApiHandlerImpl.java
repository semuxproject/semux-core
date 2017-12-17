/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.api.response.AddNodeResponse;
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
import org.semux.api.util.TransactionBuilder;
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
import org.semux.util.exception.UnreachableException;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * Semux RESTful API handler implementation.
 * 
 * TODO: Auto-generate API docs
 */
public class ApiHandlerImpl implements ApiHandler {

    private final Kernel kernel;

    /**
     * Creates an API handler.
     *
     * @param kernel
     */
    public ApiHandlerImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public ApiHandlerResponse service(String uri, Map<String, String> params, HttpHeaders headers) {
        if ("/".equals(uri)) {
            return new GetRootResponse(true, "Semux API works");
        }

        Command cmd = Command.of(uri.substring(1));
        if (cmd == null) {
            return failure("Invalid request: uri = " + uri);
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
            case DELEGATE:
            case VOTE:
            case UNVOTE:
                return doTransaction(cmd, params);
            default:
                // should never reach here, otherwise it's the programmer's fault
                throw new UnreachableException();
            }
        } catch (Exception e) {
            return failure("Failed to process your request: " + e.getMessage());
        }
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
        return new GetPeersResponse(true, //
                kernel.getChannelManager().getActivePeers().parallelStream() //
                        .map(GetPeersResponse.Result::new) //
                        .collect(Collectors.toList()));
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
            return failure(e.getMessage());
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
            throw new IllegalArgumentException("Parameter `node` can't be empty");
        }

        Matcher matcher = Pattern.compile("^(?<host>.+?):(?<port>\\d+)$").matcher(node.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Parameter `node` must in format of `host:port`");
        }

        try {
            return new InetSocketAddress(InetAddress.getByName(matcher.group("host")),
                    Integer.parseInt(matcher.group("port")));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * GET /get_block?number|hash
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
            block = kernel.getBlockchain().getBlock(Hex.decode0x(hash));
        } else {
            return failure("Either parameter `number` or `hash` has to be provided");
        }

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new GetBlockResponse.Result(block));
    }

    /**
     * GET /get_pending_transactions
     *
     * @return
     */
    private ApiHandlerResponse getPendingTransactions() {
        return new GetPendingTransactionsResponse(true, //
                kernel.getPendingManager().getTransactions().parallelStream() //
                        .map(GetTransactionResponse.Result::new) //
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
            return failure("Parameter `address` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure("Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return failure("Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return failure("Parameter `to` is not a valid integer");
        }

        return new GetAccountTransactionsResponse(true,
                kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream() //
                        .map(GetTransactionResponse.Result::new) //
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
            return failure("Parameter `hash` can't be null");
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.decode0x(hash);
        } catch (CryptoException ex) {
            return failure("Parameter `hash` is not a valid hexadecimal string");
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return failure("The request transaction was not found");
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
                return failure("Parameter `raw` can't be null");
            }

            kernel.getPendingManager().addTransaction(Transaction.fromBytes(Hex.decode0x(raw)));
            return new SendTransactionResponse(true);
        } catch (CryptoException e) {
            return failure("Parameter `raw` is not a valid hexadecimal string");
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
            return failure("Parameter `address` can't be null");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure("Parameter `address` is not a valid hexadecimal string");
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
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Parameter `ip` can't be empty");
            }

            kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());

            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
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
            String ip = params.get("ip");
            if (ip == null || ip.trim().length() == 0) {
                return failure("Parameter `ip` can't be empty");
            }

            kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());

            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
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
        return new GetLatestBlockResponse(true, new GetBlockResponse.Result(kernel.getBlockchain().getLatestBlock()));
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
            return failure("Parameter `address` can't be null");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException e) {
            return failure(e.getMessage());
        }

        Delegate delegate = kernel.getBlockchain().getDelegateState().getDelegateByAddress(addressBytes);
        if (delegate == null) {
            return failure("The provided address is not a delegate");
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
        return new GetValidatorsResponse(true, kernel.getBlockchain().getValidators().parallelStream()
                .map(v -> Hex.PREF + v).collect(Collectors.toList()));
    }

    /**
     * GET /get_delegates
     *
     * @return
     */
    private ApiHandlerResponse getDelegates() {
        return new GetDelegatesResponse(true,
                kernel.getBlockchain().getDelegateState().getDelegates().parallelStream()
                        .map(delegate -> new GetDelegateResponse.Result(
                                kernel.getBlockchain().getValidatorStats(delegate.getAddress()), delegate))
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
            return failure("Parameter `voter` is required");
        }

        try {
            voterBytes = Hex.decode0x(voter);
        } catch (CryptoException ex) {
            return failure("Parameter `voter` is not a valid hexadecimal string");
        }

        if (delegate == null) {
            return failure("Parameter `delegate` is required");
        }

        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return failure("Parameter `delegate` is not a valid hexadecimal string");
        }

        return new GetVoteResponse(true, kernel.getBlockchain().getDelegateState().getVote(voterBytes, delegateBytes));
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
            return failure("Parameter `delegate` can't be null");
        }

        byte[] delegateBytes;
        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return failure("Parameter `delegate` is not a valid hexadecimal string");
        }

        return new GetVotesResponse(true,
                kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                        .collect(Collectors.toMap(entry -> Hex.PREF + entry.getKey().toString(), Map.Entry::getValue)));
    }

    /**
     * GET /list_accounts
     *
     * @return
     */
    private ApiHandlerResponse listAccounts() {
        return new ListAccountsResponse(true, kernel.getWallet().getAccounts().parallelStream() //
                .map(acc -> Hex.PREF + acc.toAddressString()) //
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
            return failure(e.getMessage());
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
    private ApiHandlerResponse doTransaction(Command cmd, Map<String, String> params) {
        // [1] check if kernel.getWallet().is unlocked
        if (!kernel.getWallet().isUnlocked()) {
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
            return failure("Unsupported transaction type: " + cmd.toString());
        }

        // [3] build and send the transaction to PendingManager
        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder(kernel, type) //
                    .withFrom(params.get("from")) //
                    .withTo(params.get("to")) //
                    .withValue(params.get("value")) //
                    .withFee(params.get("fee")) //
                    .withData(params.get("data"));

            Transaction tx = transactionBuilder.build();

            if (kernel.getPendingManager().addTransactionSync(tx)) {
                return new DoTransactionResponse(true, null, Hex.encode0x(tx.getHash()));
            } else {
                // TODO: report the actual reason of rejection
                return failure("Transaction rejected by pending manager");
            }
        } catch (IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    /**
     * Construct a failure response.
     *
     * @param message
     * @return
     */
    private ApiHandlerResponse failure(String message) {
        return new ApiHandlerResponse(false, message);
    }
}
