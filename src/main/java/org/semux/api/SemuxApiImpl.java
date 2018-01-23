/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
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
import org.semux.api.response.SignMessageResponse;
import org.semux.api.response.Types;
import org.semux.api.response.VerifyMessageResponse;
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
import org.semux.util.Bytes;

public class SemuxApiImpl implements SemuxApi {
    private Kernel kernel;

    public SemuxApiImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public ApiHandlerResponse failure(String message) {
        return new ApiHandlerResponse(false, message);
    }

    @Override
    public ApiHandlerResponse getInfo() {
        return new GetInfoResponse(true, new Types.InfoType(kernel));
    }

    @Override
    public ApiHandlerResponse getPeers() {
        return new GetPeersResponse(true,
                kernel.getChannelManager().getActivePeers().parallelStream()
                        .map(Types.PeerType::new)
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

    @Override
    public ApiHandlerResponse addToBlacklist(String ip) {
        try {
            if (ip == null || ip.trim().length() == 0) {
                return failure("Parameter `ip` can't be empty");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.blacklistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());
            kernel.getChannelManager().removeBlacklistedChannels();

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

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.whitelistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());

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
        return new GetLatestBlockResponse(true,
                new Types.BlockType(kernel.getBlockchain().getLatestBlock()));
    }

    @Override
    public ApiHandlerResponse getBlock(Long blockNum) {
        Block block = kernel.getBlockchain().getBlock(blockNum);

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new Types.BlockType(block));
    }

    @Override
    public ApiHandlerResponse getBlock(String hashString) {

        byte[] hash = Hex.decode0x(hashString);
        Block block = kernel.getBlockchain().getBlock(hash);

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new Types.BlockType(block));
    }

    @Override
    public ApiHandlerResponse getPendingTransactions() {
        return new GetPendingTransactionsResponse(true,
                kernel.getPendingManager().getTransactions().parallelStream()
                        .map(pendingTransaction -> pendingTransaction.transaction)
                        .map(tx -> new Types.TransactionType(
                                kernel.getBlockchain().getTransactionBlockNumber(tx.getHash()), tx))
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
                        .map(tx -> new Types.TransactionType(
                                kernel.getBlockchain().getTransactionBlockNumber(tx.getHash()), tx))
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

        return new GetTransactionResponse(true, new Types.TransactionType(
                kernel.getBlockchain().getTransactionBlockNumber(transaction.getHash()),
                transaction));
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
        return new GetAccountResponse(true, new Types.AccountType(account));
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

        return new GetDelegateResponse(true, new Types.DelegateType(validatorStats, delegate));
    }

    @Override
    public ApiHandlerResponse getDelegates() {
        return new GetDelegatesResponse(true,
                kernel.getBlockchain().getDelegateState().getDelegates().parallelStream()
                        .map(delegate -> new Types.DelegateType(
                                kernel.getBlockchain().getValidatorStats(delegate.getAddress()), delegate))
                        .collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getValidators() {
        return new GetValidatorsResponse(true, kernel.getBlockchain().getValidators().parallelStream()
                .map(v -> Hex.PREF + v).collect(Collectors.toList()));
    }

    @Override
    public ApiHandlerResponse getVote(String delegate, String voter) {
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
            Key key = new Key();
            kernel.getWallet().addAccount(key);
            kernel.getWallet().flush();
            return new CreateAccountResponse(true, Hex.PREF + key.toAddressString());
        } catch (WalletLockedException e) {
            return failure(e.getMessage());
        }
    }

    @Override
    public ApiHandlerResponse transfer(String from, String to, String value, String fee, String data) {
        TransactionType type = TransactionType.TRANSFER;
        return doTransaction(type, from, to, value, fee, data);
    }

    @Override
    public ApiHandlerResponse registerDelegate(String from, String fee, String delegateName) {
        TransactionType type = TransactionType.DELEGATE;
        return doTransaction(type, from, null, null, fee, delegateName);
    }

    @Override
    public ApiHandlerResponse vote(String from, String to, String value, String fee) {
        TransactionType type = TransactionType.VOTE;
        return doTransaction(type, from, to, value, fee, null);
    }

    @Override
    public ApiHandlerResponse unvote(String from, String to, String value, String fee) {
        TransactionType type = TransactionType.UNVOTE;
        return doTransaction(type, from, to, value, fee, null);
    }

    @Override
    public ApiHandlerResponse getTransactionLimits(String type) {
        try {
            return new GetTransactionLimitsResponse(kernel, TransactionType.valueOf(type));
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure(String.format("Invalid transaction type. (must be one of %s)",
                    Arrays.stream(TransactionType.values())
                            .map(TransactionType::toString)
                            .collect(Collectors.joining(","))));
        }
    }

    @Override
    public ApiHandlerResponse signMessage(String address, String message) {
        if (address == null) {
            return failure("Parameter `address` can't be null");
        }
        if (message == null) {
            return failure("Parameter `message` can't be null");
        }
        try {
            byte[] addressBytes = Hex.decode0x(address);

            Key account = kernel.getWallet().getAccount(addressBytes);
            Key.Signature signedMessage = account.sign(message.getBytes());
            return new SignMessageResponse(true, Hex.encode0x(signedMessage.toBytes()));
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure("Invalid message");
        }
    }

    @Override
    public ApiHandlerResponse verifyMessage(String address, String message, String signature) {
        if (address == null) {
            return failure("Parameter `address` can't be null");
        }
        if (message == null) {
            return failure("Parameter `message` can't be null");
        }
        if (signature == null) {
            return failure("Parameter `signature` can't be null");
        }
        try {
            Key.Signature sig = Key.Signature.fromBytes(Hex.decode0x(signature));
            EdDSAPublicKey pubKey = PublicKeyCache.computeIfAbsent(sig.getPublicKey());
            byte[] signatureAddress = Hash.h160(pubKey.getEncoded());
            byte[] addressBytes = Hex.decode0x(address);

            if (!Arrays.equals(signatureAddress, addressBytes)) {
                return failure("Signature does not match provided address.");
            }
            if (!Key.verify(message.getBytes(), sig)) {
                return failure("Signature does not match message.");
            }

            return new VerifyMessageResponse(true);
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure(String.format("Invalid Signature"));
        }
    }

    /**
     * Validates node parameter of /add_node API
     *
     * @param node
     *            node parameter of /add_node API
     * @return validated hostname and port number
     */
    protected NodeManager.Node validateAddNodeParameter(String node) {
        if (node == null || node.length() == 0) {
            throw new IllegalArgumentException("Parameter `node` can't be empty");
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
    protected ApiHandlerResponse doTransaction(TransactionType type, String from, String to, String value, String fee,
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

            return new DoTransactionResponse(true, null, Hex.encode0x(tx.getHash()));
        } catch (IllegalArgumentException ex) {
            return failure(ex.getMessage());
        }
    }
}
