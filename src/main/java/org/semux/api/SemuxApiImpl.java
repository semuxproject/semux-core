package org.semux.api;

import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.semux.api.response.GetTransactionLimitsResponse;
import org.semux.api.response.GetTransactionResponse;
import org.semux.api.response.GetValidatorsResponse;
import org.semux.api.response.GetVoteResponse;
import org.semux.api.response.GetVotesResponse;
import org.semux.api.response.ListAccountsResponse;
import org.semux.api.response.SendTransactionResponse;
import org.semux.api.util.TransactionBuilder;
import org.semux.core.Block;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.exception.WalletLockedException;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.net.NodeManager;

/**
 */
public class SemuxApiImpl implements SemuxAPI {
    private Kernel kernel;

    public SemuxApiImpl(Kernel kernel) {

        this.kernel = kernel;
    }

    @Override
    public ApiHandlerResponse getInfo() {
        return new GetInfoResponse(true, new GetInfoResponse.Result(kernel));
    }

    @Override
    public ApiHandlerResponse getPeers() {
        return new GetPeersResponse(true,
                kernel.getChannelManager().getActivePeers().parallelStream()
                        .map(GetPeersResponse.Result::new)
                        .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse addNode(String node) {
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(node));
            return new AddNodeResponse(true);
        } catch (IllegalArgumentException e) {
            return failure(e.getMessage());
        }
    }

    public ApiHandlerResponse failure(String message) {
        return new ApiHandlerResponse(false, message);
    }

    @Override
    public ApiHandlerResponse addToBlacklist(String ip) {
        try {
            if (ip == null || ip.trim().length() == 0) {
                return failure("Parameter `ip` can't be empty");
            }

            kernel.getChannelManager().getIpFilter().blacklistIp(ip.trim());

            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse addToWhitelist(String ip) {
        try {
            if (ip == null || ip.trim().length() == 0) {
                return failure("Parameter `ip` can't be empty");
            }

            kernel.getChannelManager().getIpFilter().whitelistIp(ip.trim());

            return new ApiHandlerResponse(true, null);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse getLatestBlockNumber() {
        return new GetLatestBlockNumberResponse(true, kernel.getBlockchain().getLatestBlockNumber());

    }

    @Override
    public ApiHandlerResponse getLatestBlock() {
        return new GetLatestBlockResponse(true, new GetBlockResponse.Result(kernel.getBlockchain().getLatestBlock()));
    }

    @Override
    public ApiHandlerResponse getBlock(long blockNum) {
        Block block = kernel.getBlockchain().getBlock(blockNum);

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new GetBlockResponse.Result(block));
    }

    @Override
    public ApiHandlerResponse getBlock(String hashString) {

        byte[] hash = Hex.decode0x(hashString);
        Block block = kernel.getBlockchain().getBlock(hash);

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new GetBlockResponse.Result(block));
    }

    @Override
    public ApiHandlerResponse getPendingTransactions() {
        return new GetPendingTransactionsResponse(true,
                kernel.getPendingManager().getTransactions().parallelStream()
                        .map(pendingTransaction -> pendingTransaction.transaction)
                        .map(GetTransactionResponse.Result::new)
                        .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getAccountTransactions(String address, String from, String to) {
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
                kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream()
                        .map(GetTransactionResponse.Result::new)
                        .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getTransaction(String hash) {
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

    @Override
    public ApiHandlerResponse sendTransaction(String raw) {
        if (raw == null) {
            return failure("Parameter `raw` can't be null");
        }

        try {
            kernel.getPendingManager().addTransaction(Transaction.fromBytes(Hex.decode0x(raw)));
            return new SendTransactionResponse(true);
        } catch (CryptoException e) {
            return failure("Parameter `raw` is not a valid hexadecimal string");
        }
    }

    @Override
    public ApiHandlerResponse getAccount(String address) {
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

    @Override
    public ApiHandlerResponse getDelegate(String address) {
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

    @Override
    public ApiHandlerResponse getDelegates() {
        return new GetDelegatesResponse(true,
                kernel.getBlockchain().getDelegateState().getDelegates().parallelStream()
                        .map(delegate -> new GetDelegateResponse.Result(
                                kernel.getBlockchain().getValidatorStats(delegate.getAddress()), delegate))
                        .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getValidators() {
        return new GetValidatorsResponse(true, kernel.getBlockchain().getValidators().parallelStream()
                .map(v -> Hex.PREF + v).collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getVotes(String delegate, String voter) {
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

    @Override
    public ApiHandlerResponse getVotes(String delegate) {
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

    @Override
    public ApiHandlerResponse listAccounts() {
        return new ListAccountsResponse(true, kernel.getWallet().getAccounts().parallelStream()
                .map(acc -> Hex.PREF + acc.toAddressString())
                .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse createAccount() {
        try {
            EdDSA key = new EdDSA();
            kernel.getWallet().addAccount(key);
            kernel.getWallet().flush();
            return new CreateAccountResponse(true, Hex.PREF + key.toAddressString());
        } catch (WalletLockedException e) {
            return failure(e.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse transfer(String amountToSend, String from, String to, String fee, String data) {
        TransactionType type = TransactionType.TRANSFER;
        return doTransaction(type, amountToSend, from, to, fee, data);
    }

    private ApiHandlerResponse doTransaction(TransactionType type, String value, String from, String to, String fee, String data) {
        // [3] build and send the transaction to PendingManager
        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder(kernel, type)
                    .withFrom(from)
                    .withTo(to)
                    .withValue(value)
                    .withFee(fee)
                    .withData(data);

            Transaction tx = transactionBuilder.build();

            PendingManager.ProcessTransactionResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return failure("Transaction rejected by pending manager: " + result.error.toString());
            }

            return new DoTransactionResponse(true, null, Hex.encode0x(tx.getHash()));
        } catch (IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse registerDelegate(String fromAddress, String fee, String delegateName) {
        TransactionType type = TransactionType.DELEGATE;
        return doTransaction(type, null, fromAddress, null, fee, delegateName);
    }

    @Override
    public ApiHandlerResponse vote(String from, String to, String value, String fee) {
        TransactionType type = TransactionType.VOTE;
        return doTransaction(type, value, from, to, fee, null);
    }

    @Override
    public ApiHandlerResponse unvote(String from, String to, String value, String fee) {
        TransactionType type = TransactionType.VOTE;
        return doTransaction(type, value, from, to, fee, null);
    }

    public ApiHandlerResponse getTransactionLimits(String type) {
        try {
            return new GetTransactionLimitsResponse(kernel, TransactionType.valueOf(type));
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure(String.format(
                    "Invalid transaction type. (must be one of %s)",
                    Arrays.stream(TransactionType.values())
                            .map(TransactionType::toString)
                            .collect(Collectors.joining(","))));
        }
    }

    /**
     * Validate node parameter of /add_node API
     *
     * @param node node parameter of /add_node API
     * @return validated hostname and port number
     */
    private NodeManager.Node validateAddNodeParameter(String node) {
        if (node == null || node.length() == 0) {
            throw new IllegalArgumentException("Parameter `node` can't be empty");
        }

        Matcher matcher = Pattern.compile("^(?<host>.+?):(?<port>\\d+)$").matcher(node.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Parameter `node` must in format of `host:port`");
        }

        return new NodeManager.Node(matcher.group("host"), Integer.parseInt(matcher.group("port")));
    }

}
