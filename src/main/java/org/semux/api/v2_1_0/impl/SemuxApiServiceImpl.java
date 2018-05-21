/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.api.v2_1_0.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.File;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.semux.Kernel;
import org.semux.api.FailableApiService;
import org.semux.api.util.TransactionBuilder;
import org.semux.api.v2_1_0.TypeFactory;
import org.semux.api.v2_1_0.api.SemuxApi;
import org.semux.api.v2_1_0.model.AddNodeResponse;
import org.semux.api.v2_1_0.model.ApiHandlerResponse;
import org.semux.api.v2_1_0.model.ComposeRawTransactionResponse;
import org.semux.api.v2_1_0.model.CreateAccountResponse;
import org.semux.api.v2_1_0.model.DoTransactionResponse;
import org.semux.api.v2_1_0.model.GetAccountPendingTransactionsResponse;
import org.semux.api.v2_1_0.model.GetAccountResponse;
import org.semux.api.v2_1_0.model.GetAccountTransactionsResponse;
import org.semux.api.v2_1_0.model.GetAccountVotesResponse;
import org.semux.api.v2_1_0.model.GetBlockResponse;
import org.semux.api.v2_1_0.model.GetDelegateResponse;
import org.semux.api.v2_1_0.model.GetDelegatesResponse;
import org.semux.api.v2_1_0.model.GetInfoResponse;
import org.semux.api.v2_1_0.model.GetLatestBlockNumberResponse;
import org.semux.api.v2_1_0.model.GetLatestBlockResponse;
import org.semux.api.v2_1_0.model.GetPeersResponse;
import org.semux.api.v2_1_0.model.GetPendingTransactionsResponse;
import org.semux.api.v2_1_0.model.GetRootResponse;
import org.semux.api.v2_1_0.model.GetSyncingProgressResponse;
import org.semux.api.v2_1_0.model.GetTransactionLimitsResponse;
import org.semux.api.v2_1_0.model.GetTransactionResponse;
import org.semux.api.v2_1_0.model.GetValidatorsResponse;
import org.semux.api.v2_1_0.model.GetVoteResponse;
import org.semux.api.v2_1_0.model.GetVotesResponse;
import org.semux.api.v2_1_0.model.ListAccountsResponse;
import org.semux.api.v2_1_0.model.SignMessageResponse;
import org.semux.api.v2_1_0.model.SignRawTransactionResponse;
import org.semux.api.v2_1_0.model.SyncingProgressType;
import org.semux.api.v2_1_0.model.VerifyMessageResponse;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
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

@SuppressWarnings("Duplicates")
public final class SemuxApiServiceImpl implements SemuxApi, FailableApiService {

    private static final Charset CHARSET = UTF_8;

    private final Kernel kernel;

    public SemuxApiServiceImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public Response addNode(String node) {
        AddNodeResponse resp = new AddNodeResponse();
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(node));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (IllegalArgumentException e) {
            return failure(resp, e.getMessage());
        }
    }

    @Override
    public Response addToBlacklist(String ip) {
        ApiHandlerResponse resp = new ApiHandlerResponse();
        try {
            if (!isSet(ip)) {
                return failure(resp, "Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.blacklistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());
            kernel.getChannelManager().removeBlacklistedChannels();

            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(resp, ex.getMessage());
        }
    }

    @Override
    public Response addToWhitelist(String ip) {
        ApiHandlerResponse resp = new ApiHandlerResponse();
        try {
            if (!isSet(ip)) {
                return failure(resp, "Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.whitelistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());

            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return failure(resp, ex.getMessage());
        }
    }

    @Override
    public Response composeRawTransaction(String network, String type, String fee, String nonce, String to,
            String value,
            String timestamp, String data) {
        ComposeRawTransactionResponse resp = new ComposeRawTransactionResponse();

        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                    .withNetwork(network)
                    .withType(type)
                    .withTo(to)
                    .withValue(value)
                    .withFee(fee, true)
                    .withNonce(nonce)
                    .withTimestamp(timestamp)
                    .withData(data);

            Transaction transaction = transactionBuilder.buildUnsigned();
            resp.setResult(Hex.encode0x(transaction.getEncoded()));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (IllegalArgumentException e) {
            return failure(resp, e.getMessage());
        }
    }

    @Override
    public Response createAccount(String name) {
        CreateAccountResponse resp = new CreateAccountResponse();
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
            resp.setResult(Hex.PREF + key.toAddressString());
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (WalletLockedException e) {
            return failure(resp, e.getMessage());
        }
    }

    @Override
    public Response getAccount(String address) {
        GetAccountResponse resp = new GetAccountResponse();

        if (!isSet(address)) {
            return failure(resp, "Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        Account account = kernel.getBlockchain().getAccountState().getAccount(addressBytes);
        int transactionCount = kernel.getBlockchain().getTransactionCount(account.getAddress());
        int pendingTransactionCount = (int) kernel.getPendingManager()
                .getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                .count();
        resp.setResult(TypeFactory.accountType(account, transactionCount, pendingTransactionCount));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getAccountTransactions(String address, String from, String to) {
        GetAccountTransactionsResponse resp = new GetAccountTransactionsResponse();
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (!isSet(address)) {
            return failure(resp, "Parameter `address` is required");
        }
        if (!isSet(from)) {
            return failure(resp, "Parameter `from` is required");
        }
        if (!isSet(to)) {
            return failure(resp, "Parameter `to` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return failure(resp, "Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return failure(resp, "Parameter `to` is not a valid integer");
        }

        resp.setResult(kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream()
                .map(tx -> TypeFactory.transactionType(
                        kernel.getBlockchain().getTransactionBlockNumber(tx.getHash()), tx))
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getAccountPendingTransactions(String address, String from, String to) {
        GetAccountPendingTransactionsResponse resp = new GetAccountPendingTransactionsResponse();
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (!isSet(address)) {
            return failure(resp, "Parameter `address` is required");
        }
        if (!isSet(from)) {
            return failure(resp, "Parameter `from` is required");
        }
        if (!isSet(to)) {
            return failure(resp, "Parameter `to` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return failure(resp, "Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return failure(resp, "Parameter `to` is not a valid integer");
        }

        if (toInt <= fromInt) {
            return failure(resp, "Parameter `to` must be greater than `from`");
        }

        resp.setResult(kernel.getPendingManager()
                .getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                .skip(fromInt)
                .limit(toInt - fromInt)
                .map(TypeFactory::pendingTransactionType)
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok(resp).build();
    }

    @Override
    public Response getAccountVotes(String address) {
        GetAccountVotesResponse resp = new GetAccountVotesResponse();
        byte[] addressBytes;

        if (!isSet(address)) {
            return failure(resp, "Parameter `address` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        resp.setResult(TypeFactory.accountVotes(kernel.getBlockchain(), addressBytes));
        resp.setSuccess(true);
        return Response.ok(resp).build();
    }

    @Override
    public Response getBlockByHash(String hashString) {
        GetBlockResponse resp = new GetBlockResponse();
        if (!isSet(hashString)) {
            return failure(resp, "Parameter `hash` is required");
        }

        byte[] hash;
        try {
            hash = Hex.decode0x(hashString);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `hash` is not a valid hexadecimal string");
        }

        Block block = kernel.getBlockchain().getBlock(hash);
        if (block == null) {
            return failure(resp, "The requested block was not found");
        }

        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getBlockByNumber(String blockNum) {
        GetBlockResponse resp = new GetBlockResponse();

        if (blockNum == null) {
            return failure(resp, "Parameter `number` is required");
        }

        Long blockNumLong;
        try {
            blockNumLong = Long.parseLong(blockNum);
        } catch (NumberFormatException e) {
            return failure(resp, "Parameter `number` is not a valid number");
        }

        Block block = kernel.getBlockchain().getBlock(blockNumLong);
        if (block == null) {
            return failure(resp, "The requested block was not found");
        }

        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getDelegate(String address) {
        GetDelegateResponse resp = new GetDelegateResponse();
        if (!isSet(address)) {
            return failure(resp, "Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException e) {
            return failure(resp, e.getMessage());
        }
        Blockchain chain = kernel.getBlockchain();

        Delegate delegate = chain.getDelegateState().getDelegateByAddress(addressBytes);
        if (delegate == null) {
            return failure(resp, "The provided address is not a delegate");
        }

        BlockchainImpl.ValidatorStats validatorStats = chain.getValidatorStats(addressBytes);
        boolean isValidator = chain.getValidators().contains(address.replace("0x", ""));

        resp.setResult(TypeFactory.delegateType(validatorStats, delegate, isValidator));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getDelegates() {
        GetDelegatesResponse resp = new GetDelegatesResponse();
        Blockchain chain = kernel.getBlockchain();
        Set<String> validators = new HashSet<>(chain.getValidators());

        resp.setResult(chain.getDelegateState().getDelegates().parallelStream()
                .map(delegate -> TypeFactory.delegateType(
                        chain.getValidatorStats(delegate.getAddress()),
                        delegate,
                        validators.contains(delegate.getAddressString())))
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getInfo() {
        GetInfoResponse resp = new GetInfoResponse();
        resp.setResult(TypeFactory.infoType(kernel));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getLatestBlock() {
        GetLatestBlockResponse resp = new GetLatestBlockResponse();
        Block block = kernel.getBlockchain().getLatestBlock();
        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getLatestBlockNumber() {
        GetLatestBlockNumberResponse resp = new GetLatestBlockNumberResponse();
        resp.result(String.valueOf(kernel.getBlockchain().getLatestBlockNumber()));
        resp.success(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getPeers() {
        GetPeersResponse resp = new GetPeersResponse();
        resp.setResult(kernel.getChannelManager().getActivePeers().parallelStream()
                .map(TypeFactory::peerType)
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getPendingTransactions() {
        GetPendingTransactionsResponse resp = new GetPendingTransactionsResponse();
        resp.result(kernel.getPendingManager().getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .map(TypeFactory::pendingTransactionType)
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getRoot() {
        GetRootResponse resp = new GetRootResponse();
        resp.setMessage("Semux API Works!");
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getTransaction(String hash) {
        GetTransactionResponse resp = new GetTransactionResponse();

        if (!isSet(hash)) {
            return failure(resp, "Parameter `hash` is required");
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.decode0x(hash);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `hash` is not a valid hexadecimal string");
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return failure(resp, "The request transaction was not found");
        }

        resp.setResult(TypeFactory.transactionType(
                kernel.getBlockchain().getTransactionBlockNumber(transaction.getHash()),
                transaction));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getTransactionLimits(String type) {
        GetTransactionLimitsResponse resp = new GetTransactionLimitsResponse();
        try {
            resp.setResult(TypeFactory.transactionLimitsType(kernel, TransactionType.valueOf(type)));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure(resp, String.format("Invalid transaction type. (must be one of %s)",
                    Arrays.stream(TransactionType.values())
                            .map(TransactionType::toString)
                            .collect(Collectors.joining(","))));
        }
    }

    @Override
    public Response getValidators() {
        GetValidatorsResponse resp = new GetValidatorsResponse();
        resp.setResult(kernel.getBlockchain().getValidators().parallelStream()
                .map(v -> Hex.PREF + v).collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getVote(String delegate, String voter) {
        GetVoteResponse resp = new GetVoteResponse();
        byte[] voterBytes;
        byte[] delegateBytes;

        if (!isSet(voter)) {
            return failure(resp, "Parameter `voter` is required");
        }

        if (!isSet(delegate)) {
            return failure(resp, "Parameter `delegate` is required");
        }

        try {
            voterBytes = Hex.decode0x(voter);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `voter` is not a valid hexadecimal string");
        }

        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `delegate` is not a valid hexadecimal string");
        }

        resp.setResult(
                TypeFactory.encodeAmount(kernel.getBlockchain().getDelegateState().getVote(voterBytes, delegateBytes)));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response getVotes(String delegate) {
        GetVotesResponse resp = new GetVotesResponse();
        if (!isSet(delegate)) {
            return failure(resp, "Parameter `delegate` is required");
        }

        byte[] delegateBytes;
        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `delegate` is not a valid hexadecimal string");
        }

        resp.setResult(kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                .collect(Collectors.toMap(
                        entry -> Hex.PREF + entry.getKey().toString(),
                        entry -> TypeFactory.encodeAmount(entry.getValue()))));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response listAccounts() {
        ListAccountsResponse resp = new ListAccountsResponse();
        resp.setResult(kernel.getWallet().getAccounts().parallelStream()
                .map(acc -> Hex.PREF + acc.toAddressString())
                .collect(Collectors.toList()));
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response registerDelegate(String from, String data, String fee, String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.DELEGATE, from, null, null, fee, nonce, validateNonce, data);
    }

    @Override
    public Response broadcastRawTransaction(String raw, Boolean validateNonce) {
        DoTransactionResponse resp = new DoTransactionResponse();

        if (!isSet(raw)) {
            return failure(resp, "Parameter `raw` is required");
        }

        try {
            Transaction tx = Transaction.fromBytes(Hex.decode0x(raw));

            // tx nonce is validated in advance to avoid silently pushing the tx into
            // delayed queue of pending manager
            if ((validateNonce == null || validateNonce)
                    && tx.getNonce() != kernel.getPendingManager().getNonce(tx.getFrom())) {
                return failure(resp, "Invalid transaction nonce.");
            }

            PendingManager.ProcessTransactionResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return failure(resp, "Transaction rejected by pending manager: " + result.error.toString());
            }
            resp.setResult(Hex.encode0x(tx.getHash()));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (CryptoException e) {
            return failure(resp, "Parameter `raw` is not a valid hexadecimal string");
        } catch (IndexOutOfBoundsException e) {
            return failure(resp, "Parameter `raw` is not a valid hexadecimal raw transaction");
        }
    }

    @Override
    public Response signMessage(String address, String message) {
        SignMessageResponse resp = new SignMessageResponse();

        if (address == null) {
            return failure(resp, "Parameter `address` is required");
        }

        if (message == null) {
            return failure(resp, "Parameter `message` is required");
        }

        try {
            byte[] addressBytes;
            try {
                addressBytes = Hex.decode0x(address);
            } catch (CryptoException ex) {
                return failure(resp, "Parameter `address` is not a valid hexadecimal string");
            }

            Key account = kernel.getWallet().getAccount(addressBytes);

            if (account == null) {
                return failure(resp,
                        String.format("The provided address %s doesn't belong to the wallet", address));
            }

            Key.Signature signedMessage = account.sign(message.getBytes(CHARSET));
            resp.setResult(Hex.encode0x(signedMessage.toBytes()));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return failure(resp, "Invalid message");
        }
    }

    @Override
    public Response signRawTransaction(String raw, String address) {
        SignRawTransactionResponse resp = new SignRawTransactionResponse();

        byte[] txBytes;
        try {
            txBytes = Hex.decode0x(raw);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `raw` is not a hexadecimal string.");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return failure(resp, "Parameter `address` is not a hexadecimal string.");
        }

        Key signerKey = kernel.getWallet().getAccount(addressBytes);
        if (signerKey == null) {
            return failure(resp, "Parameter `address` doesn't exist in the wallet.");
        }

        try {
            Transaction tx = Transaction.fromEncoded(txBytes).sign(signerKey);
            resp.setResult(Hex.encode0x(tx.toBytes()));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (IndexOutOfBoundsException ex) {
            return failure(resp, "Parameter `raw` is not a valid raw transaction.");
        }
    }

    @Override
    public Response transfer(String from, String to, String value, String fee, String nonce, Boolean validateNonce,
            String data) {
        return doTransaction(TransactionType.TRANSFER, from, to, value, fee, nonce, validateNonce, data);
    }

    @Override
    public Response unvote(String from, String to, String value, String fee, String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.UNVOTE, from, to, value, fee, nonce, validateNonce, null);
    }

    @Override
    public Response verifyMessage(String address, String message, String signature) {
        VerifyMessageResponse resp = new VerifyMessageResponse();

        if (address == null) {
            return failure(resp, "Parameter `address` is required");
        }

        if (message == null) {
            return failure(resp, "Parameter `message` is required");
        }

        if (signature == null) {
            return failure(resp, "Parameter `signature` is required");
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

        resp.setValidSignature(isValidSignature);
        resp.setSuccess(true);
        return Response.ok().entity(resp).build();
    }

    @Override
    public Response vote(String from, String to, String value, String fee, String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.VOTE, from, to, value, fee, nonce, validateNonce, null);
    }

    @Override
    public Response getSyncingProgress() {
        GetSyncingProgressResponse resp = new GetSyncingProgressResponse();
        SyncingProgressType result = new SyncingProgressType();

        if (kernel.getSyncManager().isRunning()) {
            SyncManager.Progress progress = kernel.getSyncManager().getProgress();
            result.setSyncing(true);
            result.setStartingHeight(String.valueOf(progress.getStartingHeight()));
            result.setCurrentHeight(String.valueOf(progress.getCurrentHeight()));
            result.setTargetHeight(String.valueOf(progress.getTargetHeight()));
        } else {
            result.setSyncing(false);
        }

        resp.setSuccess(true);
        resp.setResult(result);
        return Response.ok(resp).build();
    }

    public Response failure(ApiHandlerResponse resp, Response.Status status, String message) {
        resp.setSuccess(false);
        resp.setMessage(message);
        return Response.status(status).entity(resp).build();
    }

    public Response failure(ApiHandlerResponse resp, String message) {
        return failure(resp, BAD_REQUEST, message);
    }

    @Override
    public Response failure(Response.Status status, String message) {
        return failure(new ApiHandlerResponse(), status, message);
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

        String host = matcher.group("host");
        if (!DomainValidator.getInstance().isValid(host) && !InetAddressValidator.getInstance().isValid(host)) {
            throw new IllegalArgumentException("Parameter `host` must be a hostname or ip address`");
        }

        Integer port = Integer.parseInt(matcher.group("port"));
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Parameter `node` is invalid`");
        }

        return new NodeManager.Node(host, port);
    }

    private Response doTransaction(TransactionType type, String from, String to, String value, String fee, String nonce,
            Boolean validateNonce, String data) {
        DoTransactionResponse resp = new DoTransactionResponse();
        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                    .withType(type)
                    .withFrom(from)
                    .withTo(to)
                    .withValue(value)
                    .withFee(fee, true)
                    .withData(data);

            if (nonce != null) {
                transactionBuilder.withNonce(nonce);
            }

            Transaction tx = transactionBuilder.buildSigned();

            // tx nonce is validated in advance to avoid silently pushing the tx into
            // delayed queue of pending manager
            if ((validateNonce == null || validateNonce)
                    && tx.getNonce() != kernel.getPendingManager().getNonce(tx.getFrom())) {
                return failure(resp, "Invalid transaction nonce.");
            }

            PendingManager.ProcessTransactionResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return failure(resp, "Transaction rejected by pending manager: " + result.error.toString());
            }

            resp.setResult(Hex.encode0x(tx.getHash()));
            resp.setSuccess(true);
            return Response.ok().entity(resp).build();
        } catch (IllegalArgumentException ex) {
            return failure(resp, ex.getMessage());
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
