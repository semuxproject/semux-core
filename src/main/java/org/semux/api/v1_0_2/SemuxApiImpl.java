/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.api.v1_0_2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.semux.Kernel;
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
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.cache.PublicKeyCache;
import org.semux.net.NodeManager;
import org.semux.net.filter.SemuxIpFilter;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

public final class SemuxApiImpl implements SemuxApi {
    private static final Charset CHARSET = UTF_8;

    private Kernel kernel;

    public SemuxApiImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    public ApiHandlerResponse failure(String message) {
        return new ApiHandlerResponse()
                .success(false)
                .message(message);
    }

    @Override
    public GetRootResponse getRoot() {
        return null;
    }

    @Override
    public GetInfoResponse getInfo() {
        return (GetInfoResponse) new GetInfoResponse()
                .result(TypeFactory.infoType(kernel))
                .success(true);
    }

    @Override
    public GetPeersResponse getPeers() {
        return (GetPeersResponse) new GetPeersResponse()
                .result(kernel.getChannelManager().getActivePeers().parallelStream()
                        .map(TypeFactory::peerType)
                        .collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public AddNodeResponse addNode(String node) {
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(node));
            return (AddNodeResponse) new AddNodeResponse().success(true);
        } catch (IllegalArgumentException e) {
            return (AddNodeResponse) failure(e.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse addToBlacklist(String ip) {
        try {
            if (!isSet(ip)) {
                return failure("Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.blacklistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());
            kernel.getChannelManager().removeBlacklistedChannels();

            return new ApiHandlerResponse().success(true);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse addToWhitelist(String ip) {
        try {
            if (!isSet(ip)) {
                return failure("Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.whitelistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());

            return new ApiHandlerResponse().success(true);
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    @Override
    public GetLatestBlockNumberResponse getLatestBlockNumber() {
        return (GetLatestBlockNumberResponse) new GetLatestBlockNumberResponse()
                .result(String.valueOf(kernel.getBlockchain().getLatestBlockNumber()))
                .success(true);
    }

    @Override
    public GetLatestBlockResponse getLatestBlock() {
        return (GetLatestBlockResponse) new GetLatestBlockResponse()
                .result(TypeFactory.blockType(kernel.getBlockchain().getLatestBlock()))
                .success(true);
    }

    @Override
    public GetBlockResponse getBlockByNumber(String blockNum) {
        if (blockNum == null) {
            return (GetBlockResponse) failure("Parameter `number` is required");
        }

        Long blockNumLong;
        try {
            blockNumLong = Long.parseLong(blockNum);
        } catch (NumberFormatException e) {
            return (GetBlockResponse) failure("Parameter `number` is not a valid number");
        }

        Block block = kernel.getBlockchain().getBlock(blockNumLong);

        if (block == null) {
            return (GetBlockResponse) failure("The requested block was not found");
        }

        return (GetBlockResponse) new GetBlockResponse()
                .result(TypeFactory.blockType(block))
                .success(true);
    }

    @Override
    public GetBlockResponse getBlockByHash(String hashString) {
        if (!isSet(hashString)) {
            return (GetBlockResponse) failure("Parameter `hash` is required");
        }

        byte[] hash;
        try {
            hash = Hex.decode0x(hashString);
        } catch (CryptoException ex) {
            return (GetBlockResponse) failure("Parameter `hash` is not a valid hexadecimal string");
        }

        Block block = kernel.getBlockchain().getBlock(hash);

        if (block == null) {
            return (GetBlockResponse) failure("The requested block was not found");
        }

        return (GetBlockResponse) new GetBlockResponse()
                .result(TypeFactory.blockType(block))
                .success(true);
    }

    @Override
    public GetPendingTransactionsResponse getPendingTransactions() {
        return (GetPendingTransactionsResponse) new GetPendingTransactionsResponse()
                .result(kernel.getPendingManager().getPendingTransactions().parallelStream()
                        .map(pendingTransaction -> pendingTransaction.transaction)
                        .map(tx -> TypeFactory.transactionType(null, tx))
                        .collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public GetAccountTransactionsResponse getAccountTransactions(String address, String from, String to) {
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (!isSet(address)) {
            return (GetAccountTransactionsResponse) failure("Parameter `address` is required");
        }
        if (!isSet(from)) {
            return (GetAccountTransactionsResponse) failure("Parameter `from` is required");
        }
        if (!isSet(to)) {
            return (GetAccountTransactionsResponse) failure("Parameter `to` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return (GetAccountTransactionsResponse) failure("Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return (GetAccountTransactionsResponse) failure("Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return (GetAccountTransactionsResponse) failure("Parameter `to` is not a valid integer");
        }

        return (GetAccountTransactionsResponse) new GetAccountTransactionsResponse()
                .result(kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream()
                        .map(tx -> TypeFactory.transactionType(
                                kernel.getBlockchain().getTransactionBlockNumber(tx.getHash()), tx))
                        .collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public GetTransactionResponse getTransaction(String hash) {
        if (!isSet(hash)) {
            return (GetTransactionResponse) failure("Parameter `hash` is required");
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.decode0x(hash);
        } catch (CryptoException ex) {
            return (GetTransactionResponse) failure("Parameter `hash` is not a valid hexadecimal string");
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return (GetTransactionResponse) failure("The request transaction was not found");
        }

        return (GetTransactionResponse) new GetTransactionResponse()
                .result(TypeFactory.transactionType(
                        kernel.getBlockchain().getTransactionBlockNumber(transaction.getHash()),
                        transaction))
                .success(true);
    }

    @Override
    public SendTransactionResponse sendTransaction(String raw) {
        if (!isSet(raw)) {
            return (SendTransactionResponse) failure("Parameter `raw` is required");
        }

        try {
            kernel.getPendingManager().addTransaction(Transaction.fromBytes(Hex.decode0x(raw)));
            return (SendTransactionResponse) new SendTransactionResponse().success(true);
        } catch (CryptoException e) {
            return (SendTransactionResponse) failure("Parameter `raw` is not a valid hexadecimal string");
        }
    }

    @Override
    public GetAccountResponse getAccount(String address) {
        if (!isSet(address)) {
            return (GetAccountResponse) failure("Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return (GetAccountResponse) failure("Parameter `address` is not a valid hexadecimal string");
        }

        Account account = kernel.getBlockchain().getAccountState().getAccount(addressBytes);
        int transactionCount = kernel.getBlockchain().getTransactionCount(account.getAddress());
        return (GetAccountResponse) new GetAccountResponse()
                .result(TypeFactory.accountType(account, transactionCount))
                .success(true);
    }

    @Override
    public GetDelegateResponse getDelegate(String address) {
        if (!isSet(address)) {
            return (GetDelegateResponse) failure("Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException e) {
            return (GetDelegateResponse) failure(e.getMessage());
        }

        Delegate delegate = kernel.getBlockchain().getDelegateState().getDelegateByAddress(addressBytes);
        if (delegate == null) {
            return (GetDelegateResponse) failure("The provided address is not a delegate");
        }

        BlockchainImpl.ValidatorStats validatorStats = kernel.getBlockchain().getValidatorStats(addressBytes);

        return (GetDelegateResponse) new GetDelegateResponse()
                .result(TypeFactory.delegateType(validatorStats, delegate))
                .success(true);
    }

    @Override
    public GetDelegatesResponse getDelegates() {
        return (GetDelegatesResponse) new GetDelegatesResponse()
                .result(kernel.getBlockchain().getDelegateState().getDelegates().parallelStream()
                        .map(delegate -> TypeFactory.delegateType(
                                kernel.getBlockchain().getValidatorStats(delegate.getAddress()), delegate))
                        .collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public GetValidatorsResponse getValidators() {
        return (GetValidatorsResponse) new GetValidatorsResponse()
                .result(kernel.getBlockchain().getValidators().parallelStream()
                        .map(v -> Hex.PREF + v).collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public GetVoteResponse getVote(String delegate, String voter) {
        byte[] voterBytes;
        byte[] delegateBytes;

        if (!isSet(voter)) {
            return (GetVoteResponse) failure("Parameter `voter` is required");
        }

        if (!isSet(delegate)) {
            return (GetVoteResponse) failure("Parameter `delegate` is required");
        }

        try {
            voterBytes = Hex.decode0x(voter);
        } catch (CryptoException ex) {
            return (GetVoteResponse) failure("Parameter `voter` is not a valid hexadecimal string");
        }

        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return (GetVoteResponse) failure("Parameter `delegate` is not a valid hexadecimal string");
        }

        return (GetVoteResponse) new GetVoteResponse()
                .result(TypeFactory
                        .encodeAmount(kernel.getBlockchain().getDelegateState().getVote(voterBytes, delegateBytes)))
                .success(true);
    }

    @Override
    public GetVotesResponse getVotes(String delegate) {
        if (!isSet(delegate)) {
            return (GetVotesResponse) failure("Parameter `delegate` is required");
        }

        byte[] delegateBytes;
        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return (GetVotesResponse) failure("Parameter `delegate` is not a valid hexadecimal string");
        }

        return (GetVotesResponse) new GetVotesResponse()
                .result(
                        kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                                .collect(Collectors.toMap(
                                        entry -> Hex.PREF + entry.getKey().toString(),
                                        entry -> TypeFactory.encodeAmount(entry.getValue()))))
                .success(true);
    }

    @Override
    public ListAccountsResponse listAccounts() {
        return (ListAccountsResponse) new ListAccountsResponse()
                .result(kernel.getWallet().getAccounts().parallelStream()
                        .map(acc -> Hex.PREF + acc.toAddressString())
                        .collect(Collectors.toList()))
                .success(true);
    }

    @Override
    public CreateAccountResponse createAccount(String name) {
        try {
            // create an account
            Key key = new Key();
            kernel.getWallet().addAccount(key);

            // set alias of the address
            if (isSet(name)) {
                kernel.getWallet().setAddressAlias(key.toAddress(), name);
            }

            // save the account
            kernel.getWallet().flush();
            return (CreateAccountResponse) new CreateAccountResponse().result(Hex.PREF + key.toAddressString())
                    .success(true);
        } catch (WalletLockedException e) {
            return (CreateAccountResponse) failure(e.getMessage());
        }
    }

    @Override
    public DoTransactionResponse transfer(String from, String to, String value, String fee, String data) {
        return (DoTransactionResponse) doTransaction(TransactionType.TRANSFER, from, to, value, fee, data);
    }

    @Override
    public DoTransactionResponse registerDelegate(String from, String fee, String delegateName) {
        return (DoTransactionResponse) doTransaction(TransactionType.DELEGATE, from, null, null, fee, delegateName);
    }

    @Override
    public DoTransactionResponse vote(String from, String to, String value, String fee) {
        return (DoTransactionResponse) doTransaction(TransactionType.VOTE, from, to, value, fee, null);
    }

    @Override
    public DoTransactionResponse unvote(String from, String to, String value, String fee) {
        return (DoTransactionResponse) doTransaction(TransactionType.UNVOTE, from, to, value, fee, null);
    }

    @Override
    public GetTransactionLimitsResponse getTransactionLimits(String type) {
        try {
            return (GetTransactionLimitsResponse) new GetTransactionLimitsResponse()
                    .result(TypeFactory.transactionLimitsType(kernel, TransactionType.valueOf(type)))
                    .success(true);
        } catch (NullPointerException | IllegalArgumentException e) {
            return (GetTransactionLimitsResponse) failure(String.format("Invalid transaction type. (must be one of %s)",
                    Arrays.stream(TransactionType.values())
                            .map(TransactionType::toString)
                            .collect(Collectors.joining(","))));
        }
    }

    @Override
    public SignMessageResponse signMessage(String address, String message) {
        if (address == null) {
            return (SignMessageResponse) failure("Parameter `address` is required");
        }

        if (message == null) {
            return (SignMessageResponse) failure("Parameter `message` is required");
        }

        try {
            byte[] addressBytes;
            try {
                addressBytes = Hex.decode0x(address);
            } catch (CryptoException ex) {
                return (SignMessageResponse) failure("Parameter `address` is not a valid hexadecimal string");
            }

            Key account = kernel.getWallet().getAccount(addressBytes);

            if (account == null) {
                return (SignMessageResponse) failure(
                        String.format("The provided address %s doesn't belong to the wallet", address));
            }

            Key.Signature signedMessage = account.sign(message.getBytes(CHARSET));
            return (SignMessageResponse) new SignMessageResponse()
                    .result(Hex.encode0x(signedMessage.toBytes()))
                    .success(true);
        } catch (NullPointerException | IllegalArgumentException e) {
            return (SignMessageResponse) failure("Invalid message");
        }
    }

    @Override
    public VerifyMessageResponse verifyMessage(String address, String message, String signature) {
        if (address == null) {
            return (VerifyMessageResponse) failure("Parameter `address` is required");
        }

        if (message == null) {
            return (VerifyMessageResponse) failure("Parameter `message` is required");
        }

        if (signature == null) {
            return (VerifyMessageResponse) failure("Parameter `signature` is required");
        }

        boolean isValidSignature = true;
        try {
            Key.Signature sig = Key.Signature.fromBytes(Hex.decode0x(signature));
            EdDSAPublicKey pubKey = PublicKeyCache.computeIfAbsent(sig.getPublicKey());
            byte[] signatureAddress = Hash.h160(pubKey.getEncoded());

            byte[] addressBytes;
            addressBytes = Hex.decode0x(address);
            if (!Arrays.equals(signatureAddress, addressBytes)) {
                isValidSignature = false;
            }
            if (!Key.verify(message.getBytes(CHARSET), sig)) {
                isValidSignature = false;
            }

        } catch (NullPointerException | IllegalArgumentException | CryptoException e) {
            isValidSignature = false;
        }

        return (VerifyMessageResponse) new VerifyMessageResponse().validSignature(isValidSignature).success(true);
    }

    /**
     * Validates node parameter of /add_node API
     *
     * @param node
     *            node parameter of /add_node API
     * @return validated hostname and port number
     */
    private NodeManager.Node validateAddNodeParameter(String node) {
        if (!isSet(node)) {
            throw new IllegalArgumentException("Parameter `node` is required");
        }

        Matcher matcher = Pattern.compile("^(?<host>.+?):(?<port>\\d+)$").matcher(node.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Parameter `node` must in format of `host:port`");
        }

        return new NodeManager.Node(matcher.group("host"), Integer.parseInt(matcher.group("port")));
    }

    /**
     * Processes a transaction.
     *
     * @param type
     * @param from
     * @param to
     * @param value
     * @param fee
     * @param data
     * @return
     */
    private ApiHandlerResponse doTransaction(TransactionType type, String from, String to, String value, String fee,
            String data) {
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

            return new DoTransactionResponse().result(Hex.encode0x(tx.getHash())).success(true);
        } catch (IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }

    /**
     * Whether a value is supplied
     *
     * @param value
     * @return
     */
    private boolean isSet(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
