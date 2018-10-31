/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.api.v2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.File;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.ethereum.vm.chainspec.Spec;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.TransactionReceipt;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.semux.Kernel;
import org.semux.api.util.TransactionBuilder;
import org.semux.api.v2.model.AddNodeResponse;
import org.semux.api.v2.model.ApiHandlerResponse;
import org.semux.api.v2.model.ComposeRawTransactionResponse;
import org.semux.api.v2.model.CreateAccountResponse;
import org.semux.api.v2.model.DeleteAccountResponse;
import org.semux.api.v2.model.DoTransactionResponse;
import org.semux.api.v2.model.GetAccountPendingTransactionsResponse;
import org.semux.api.v2.model.GetAccountResponse;
import org.semux.api.v2.model.GetAccountTransactionsResponse;
import org.semux.api.v2.model.GetAccountVotesResponse;
import org.semux.api.v2.model.GetBlockResponse;
import org.semux.api.v2.model.GetDelegateResponse;
import org.semux.api.v2.model.GetDelegatesResponse;
import org.semux.api.v2.model.GetInfoResponse;
import org.semux.api.v2.model.GetLatestBlockNumberResponse;
import org.semux.api.v2.model.GetLatestBlockResponse;
import org.semux.api.v2.model.GetPeersResponse;
import org.semux.api.v2.model.GetPendingTransactionsResponse;
import org.semux.api.v2.model.GetSyncingProgressResponse;
import org.semux.api.v2.model.GetTransactionLimitsResponse;
import org.semux.api.v2.model.GetTransactionResponse;
import org.semux.api.v2.model.GetValidatorsResponse;
import org.semux.api.v2.model.GetVoteResponse;
import org.semux.api.v2.model.GetVotesResponse;
import org.semux.api.v2.model.ListAccountsResponse;
import org.semux.api.v2.model.SignMessageResponse;
import org.semux.api.v2.model.SignRawTransactionResponse;
import org.semux.api.v2.model.SyncingProgressType;
import org.semux.api.v2.model.VerifyMessageResponse;
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
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;
import org.semux.vm.client.SemuxRepository;
import org.semux.vm.client.SemuxTransaction;

public final class SemuxApiImpl implements SemuxApi {

    private static final Charset CHARSET = UTF_8;

    private final Kernel kernel;

    public SemuxApiImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public Response addNode(String node) {
        AddNodeResponse resp = new AddNodeResponse();
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(node));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(resp, e.getMessage());
        }
    }

    @Override
    public Response addToBlacklist(String ip) {
        ApiHandlerResponse resp = new ApiHandlerResponse();
        try {
            if (!isSet(ip)) {
                return badRequest(resp, "Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.blacklistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());
            kernel.getChannelManager().closeBlacklistedChannels();

            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return badRequest(resp, ex.getMessage());
        }
    }

    @Override
    public Response addToWhitelist(String ip) {
        ApiHandlerResponse resp = new ApiHandlerResponse();
        try {
            if (!isSet(ip)) {
                return badRequest(resp, "Parameter `ip` is required");
            }

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.whitelistIp(ip.trim());
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());

            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return badRequest(resp, ex.getMessage());
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

            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(resp, e.getMessage());
        }
    }

    @Override
    public Response createAccount(String name, String privateKey) {
        CreateAccountResponse resp = new CreateAccountResponse();
        try {
            Key key;
            if (privateKey != null) { // import
                byte[] privateKeyBytes = Hex.decode0x(privateKey);
                key = new Key(privateKeyBytes);
            } else { // generate
                key = new Key();
            }

            if (!kernel.getWallet().addAccount(key)) {
                return badRequest(resp, "The key already exists in this wallet.");
            }

            // set alias of the address
            if (isSet(name)) {
                kernel.getWallet().setAddressAlias(key.toAddress(), name);
            }

            // save the account
            kernel.getWallet().flush();
            resp.setResult(Hex.PREF + key.toAddressString());

            return success(resp);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `privateKey` is not a valid hexadecimal string");
        } catch (InvalidKeySpecException e) {
            return badRequest(resp,
                    "Parameter `privateKey` is not a valid ED25519 private key encoded in PKCS#8 format");
        } catch (WalletLockedException e) {
            return badRequest(resp, e.getMessage());
        }
    }

    @Override
    public Response getAccount(String address) {
        GetAccountResponse resp = new GetAccountResponse();

        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        Account account = kernel.getBlockchain().getAccountState().getAccount(addressBytes);
        int transactionCount = kernel.getBlockchain().getTransactionCount(account.getAddress());
        int pendingTransactionCount = (int) kernel.getPendingManager()
                .getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                .count();
        resp.setResult(TypeFactory.accountType(account, transactionCount, pendingTransactionCount));

        return success(resp);
    }

    @Override
    public Response deleteAccount(String address) {
        DeleteAccountResponse resp = new DeleteAccountResponse();

        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }

        try {
            byte[] addressBytes = Hex.decode0x(address);

            if (!kernel.getWallet().removeAccount(addressBytes)) {
                return badRequest(resp, "The provided address doesn't exist in this wallet.");
            }

            if (!kernel.getWallet().flush()) {
                return badRequest(resp, "Failed to write the wallet.");
            }

            return success(resp);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
        } catch (WalletLockedException e) {
            return badRequest(resp, e.getMessage());
        }
    }

    @Override
    public Response getAccountTransactions(String address, String from, String to) {
        GetAccountTransactionsResponse resp = new GetAccountTransactionsResponse();
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }
        if (!isSet(from)) {
            return badRequest(resp, "Parameter `from` is required");
        }
        if (!isSet(to)) {
            return badRequest(resp, "Parameter `to` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return badRequest(resp, "Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return badRequest(resp, "Parameter `to` is not a valid integer");
        }

        resp.setResult(kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream()
                .map(tx -> TypeFactory.transactionType(
                        kernel.getBlockchain().getTransactionBlockNumber(tx.getHash()), tx))
                .collect(Collectors.toList()));

        return success(resp);
    }

    @Override
    public Response getAccountPendingTransactions(String address, String from, String to) {
        GetAccountPendingTransactionsResponse resp = new GetAccountPendingTransactionsResponse();
        byte[] addressBytes;
        int fromInt;
        int toInt;

        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }
        if (!isSet(from)) {
            return badRequest(resp, "Parameter `from` is required");
        }
        if (!isSet(to)) {
            return badRequest(resp, "Parameter `to` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        try {
            fromInt = Integer.parseInt(from);
        } catch (NumberFormatException ex) {
            return badRequest(resp, "Parameter `from` is not a valid integer");
        }

        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            return badRequest(resp, "Parameter `to` is not a valid integer");
        }

        if (toInt <= fromInt) {
            return badRequest(resp, "Parameter `to` must be greater than `from`");
        }

        resp.setResult(kernel.getPendingManager()
                .getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                .skip(fromInt)
                .limit(toInt - fromInt)
                .map(TypeFactory::pendingTransactionType)
                .collect(Collectors.toList()));

        return success(resp);
    }

    @Override
    public Response getAccountVotes(String address) {
        GetAccountVotesResponse resp = new GetAccountVotesResponse();
        byte[] addressBytes;

        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }

        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
        }

        resp.setResult(TypeFactory.accountVotes(kernel.getBlockchain(), addressBytes));

        return success(resp);
    }

    @Override
    public Response getBlockByHash(String hashString) {
        GetBlockResponse resp = new GetBlockResponse();
        if (!isSet(hashString)) {
            return badRequest(resp, "Parameter `hash` is required");
        }

        byte[] hash;
        try {
            hash = Hex.decode0x(hashString);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `hash` is not a valid hexadecimal string");
        }

        Block block = kernel.getBlockchain().getBlock(hash);
        if (block == null) {
            return badRequest(resp, "The requested block was not found");
        }

        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));

        return success(resp);
    }

    @Override
    public Response getBlockByNumber(String blockNum) {
        GetBlockResponse resp = new GetBlockResponse();

        if (blockNum == null) {
            return badRequest(resp, "Parameter `number` is required");
        }

        long blockNumLong;
        try {
            blockNumLong = Long.parseLong(blockNum);
        } catch (NumberFormatException e) {
            return badRequest(resp, "Parameter `number` is not a valid number");
        }

        Block block = kernel.getBlockchain().getBlock(blockNumLong);
        if (block == null) {
            return badRequest(resp, "The requested block was not found");
        }

        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));

        return success(resp);
    }

    @Override
    public Response getDelegate(String address) {
        GetDelegateResponse resp = new GetDelegateResponse();
        if (!isSet(address)) {
            return badRequest(resp, "Parameter `address` is required");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException e) {
            return badRequest(resp, e.getMessage());
        }
        Blockchain chain = kernel.getBlockchain();

        Delegate delegate = chain.getDelegateState().getDelegateByAddress(addressBytes);
        if (delegate == null) {
            return badRequest(resp, "The provided address is not a delegate");
        }

        BlockchainImpl.ValidatorStats validatorStats = chain.getValidatorStats(addressBytes);
        boolean isValidator = chain.getValidators().contains(address.replace("0x", ""));

        resp.setResult(TypeFactory.delegateType(validatorStats, delegate, isValidator));

        return success(resp);
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

        return success(resp);
    }

    @Override
    public Response getInfo() {
        GetInfoResponse resp = new GetInfoResponse();
        resp.setResult(TypeFactory.infoType(kernel));

        return success(resp);
    }

    @Override
    public Response getLatestBlock() {
        GetLatestBlockResponse resp = new GetLatestBlockResponse();
        Block block = kernel.getBlockchain().getLatestBlock();
        resp.setResult(TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));

        return success(resp);
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

        return success(resp);
    }

    @Override
    public Response getPendingTransactions() {
        GetPendingTransactionsResponse resp = new GetPendingTransactionsResponse();
        resp.result(kernel.getPendingManager().getPendingTransactions().parallelStream()
                .map(pendingTransaction -> pendingTransaction.transaction)
                .map(TypeFactory::pendingTransactionType)
                .collect(Collectors.toList()));

        return success(resp);
    }

    @Override
    public Response getTransaction(String hash) {
        GetTransactionResponse resp = new GetTransactionResponse();

        if (!isSet(hash)) {
            return badRequest(resp, "Parameter `hash` is required");
        }

        byte[] hashBytes;
        try {
            hashBytes = Hex.decode0x(hash);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `hash` is not a valid hexadecimal string");
        }

        Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
        if (transaction == null) {
            return badRequest(resp, "The request transaction was not found");
        }

        resp.setResult(TypeFactory.transactionType(
                kernel.getBlockchain().getTransactionBlockNumber(transaction.getHash()),
                transaction));

        return success(resp);
    }

    @Override
    public Response getTransactionLimits(String type) {
        GetTransactionLimitsResponse resp = new GetTransactionLimitsResponse();
        try {
            resp.setResult(TypeFactory.transactionLimitsType(kernel, TransactionType.valueOf(type)));

            return success(resp);
        } catch (NullPointerException | IllegalArgumentException e) {
            return badRequest(resp, String.format("Invalid transaction type. (must be one of %s)",
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

        return success(resp);
    }

    @Override
    public Response getVote(String delegate, String voter) {
        GetVoteResponse resp = new GetVoteResponse();
        byte[] voterBytes;
        byte[] delegateBytes;

        if (!isSet(voter)) {
            return badRequest(resp, "Parameter `voter` is required");
        }

        if (!isSet(delegate)) {
            return badRequest(resp, "Parameter `delegate` is required");
        }

        try {
            voterBytes = Hex.decode0x(voter);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `voter` is not a valid hexadecimal string");
        }

        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `delegate` is not a valid hexadecimal string");
        }

        resp.setResult(
                TypeFactory.encodeAmount(kernel.getBlockchain().getDelegateState().getVote(voterBytes, delegateBytes)));

        return success(resp);
    }

    @Override
    public Response getVotes(String delegate) {
        GetVotesResponse resp = new GetVotesResponse();
        if (!isSet(delegate)) {
            return badRequest(resp, "Parameter `delegate` is required");
        }

        byte[] delegateBytes;
        try {
            delegateBytes = Hex.decode0x(delegate);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `delegate` is not a valid hexadecimal string");
        }

        resp.setResult(kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                .collect(Collectors.toMap(
                        entry -> Hex.PREF + entry.getKey().toString(),
                        entry -> TypeFactory.encodeAmount(entry.getValue()))));

        return success(resp);
    }

    @Override
    public Response listAccounts() {
        ListAccountsResponse resp = new ListAccountsResponse();
        resp.setResult(kernel.getWallet().getAccounts().parallelStream()
                .map(acc -> Hex.PREF + acc.toAddressString())
                .collect(Collectors.toList()));

        return success(resp);
    }

    @Override
    public Response registerDelegate(String from, String data, String fee, String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.DELEGATE, from, null, null, fee, nonce, validateNonce, data);
    }

    @Override
    public Response broadcastRawTransaction(String raw, Boolean validateNonce) {
        DoTransactionResponse resp = new DoTransactionResponse();

        if (!isSet(raw)) {
            return badRequest(resp, "Parameter `raw` is required");
        }

        try {
            Transaction tx = Transaction.fromBytes(Hex.decode0x(raw));

            // tx nonce is validated in advance to avoid silently pushing the tx into
            // delayed queue of pending manager
            if ((validateNonce != null && validateNonce)
                    && tx.getNonce() != kernel.getPendingManager().getNonce(tx.getFrom())) {
                return badRequest(resp, "Invalid transaction nonce.");
            }

            PendingManager.ProcessTransactionResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return badRequest(resp, "Transaction rejected by pending manager: " + result.error.toString());
            }
            resp.setResult(Hex.encode0x(tx.getHash()));

            return success(resp);
        } catch (CryptoException e) {
            return badRequest(resp, "Parameter `raw` is not a valid hexadecimal string");
        } catch (IndexOutOfBoundsException e) {
            return badRequest(resp, "Parameter `raw` is not a valid hexadecimal raw transaction");
        }
    }

    @Override
    public Response signMessage(String address, String message) {
        SignMessageResponse resp = new SignMessageResponse();

        if (address == null) {
            return badRequest(resp, "Parameter `address` is required");
        }

        if (message == null) {
            return badRequest(resp, "Parameter `message` is required");
        }

        try {
            byte[] addressBytes;
            try {
                addressBytes = Hex.decode0x(address);
            } catch (CryptoException ex) {
                return badRequest(resp, "Parameter `address` is not a valid hexadecimal string");
            }

            Key account = kernel.getWallet().getAccount(addressBytes);

            if (account == null) {
                return badRequest(resp,
                        String.format("The provided address %s doesn't belong to the wallet", address));
            }

            Key.Signature signedMessage = account.sign(message.getBytes(CHARSET));
            resp.setResult(Hex.encode0x(signedMessage.toBytes()));

            return success(resp);
        } catch (NullPointerException | IllegalArgumentException e) {
            return badRequest(resp, "Invalid message");
        }
    }

    @Override
    public Response signRawTransaction(String raw, String address) {
        SignRawTransactionResponse resp = new SignRawTransactionResponse();

        byte[] txBytes;
        try {
            txBytes = Hex.decode0x(raw);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `raw` is not a hexadecimal string.");
        }

        byte[] addressBytes;
        try {
            addressBytes = Hex.decode0x(address);
        } catch (CryptoException ex) {
            return badRequest(resp, "Parameter `address` is not a hexadecimal string.");
        }

        Key signerKey = kernel.getWallet().getAccount(addressBytes);
        if (signerKey == null) {
            return badRequest(resp, "Parameter `address` doesn't exist in the wallet.");
        }

        try {
            Transaction tx = Transaction.fromEncoded(txBytes).sign(signerKey);
            resp.setResult(Hex.encode0x(tx.toBytes()));

            return success(resp);
        } catch (IndexOutOfBoundsException ex) {
            return badRequest(resp, "Parameter `raw` is not a valid raw transaction.");
        }
    }

    @Override
    public Response transfer(String from, String to, String value, String fee, String nonce, Boolean validateNonce,
            String data) {
        return doTransaction(TransactionType.TRANSFER, from, to, value, fee, nonce, validateNonce, data);
    }

    @Override
    public Response create(String from, String value, String data, String gasPrice, String gas, String fee,
            String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.CREATE, from, null, value, fee, nonce, validateNonce, data, gasPrice,
                gas);
    }

    @Override
    public Response call(String from, String to, String value, String gasPrice, String gas, String fee,
            String nonce, Boolean validateNonce, String data, Boolean local) {
        if (local) {
            Transaction tx = getTransaction(TransactionType.CALL, from, to, value, fee, nonce, validateNonce, data,
                    gasPrice, gas);

            SemuxTransaction transaction = new SemuxTransaction(tx);
            SemuxBlock block = new SemuxBlock(kernel.getBlockchain().getLatestBlock().getHeader());
            Repository repository = new SemuxRepository(kernel.getBlockchain().getAccountState());
            ProgramInvokeFactory invokeFactory = new ProgramInvokeFactoryImpl();
            BlockStore blockStore = new SemuxBlockStore(kernel.getBlockchain());
            long gasUsedInBlock = 0l;

            org.ethereum.vm.client.TransactionExecutor executor = new org.ethereum.vm.client.TransactionExecutor(
                    transaction, block, repository, blockStore,
                    Spec.DEFAULT, invokeFactory, gasUsedInBlock, true);
            TransactionReceipt results = executor.run();

            DoTransactionResponse resp = new DoTransactionResponse();
            resp.setResult(Hex.encode0x(results.getReturnData()));
            if (!results.isSuccess()) {
                return badRequest(resp, "Error calling method");
            } else {
                return success(resp);
            }
        } else {
            return doTransaction(TransactionType.CALL, from, to, value, fee, nonce, validateNonce, data, gasPrice,
                    gas);
        }
    }

    @Override
    public Response unvote(String from, String to, String value, String fee, String nonce, Boolean validateNonce) {
        return doTransaction(TransactionType.UNVOTE, from, to, value, fee, nonce, validateNonce, null);
    }

    @Override
    public Response verifyMessage(String address, String message, String signature) {
        VerifyMessageResponse resp = new VerifyMessageResponse();

        if (address == null) {
            return badRequest(resp, "Parameter `address` is required");
        }

        if (message == null) {
            return badRequest(resp, "Parameter `message` is required");
        }

        if (signature == null) {
            return badRequest(resp, "Parameter `signature` is required");
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

        return success(resp);
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

        resp.setResult(result);

        return success(resp);
    }

    /**
     * Constructs a success response.
     *
     * @param resp
     * @return
     */
    private Response success(ApiHandlerResponse resp) {
        resp.setSuccess(true);
        resp.setMessage("successful operation");

        return Response.ok().entity(resp).build();
    }

    /**
     * Constructs a failure response out of bad request.
     *
     * @param resp
     * @param message
     * @return
     */
    private Response badRequest(ApiHandlerResponse resp, String message) {
        resp.setSuccess(false);
        resp.setMessage(message);

        return Response.status(BAD_REQUEST).entity(resp).build();
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
        return doTransaction(type, from, to, value, fee, nonce, validateNonce, data, null, null);
    }

    private Transaction getTransaction(TransactionType type, String from, String to, String value, String fee,
            String nonce,
            Boolean validateNonce, String data, String gasPrice, String gas) {
        TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                .withType(type)
                .withFrom(from)
                .withTo(to)
                .withValue(value)
                .withFee(fee, true)
                .withData(data);
        if (type == TransactionType.CREATE || type == TransactionType.CALL) {
            transactionBuilder.withGasPrice(gasPrice).withGas(gas);
        }

        if (nonce != null) {
            transactionBuilder.withNonce(nonce);
        } else {
            // TODO: fix race condition of auto-assigned nonce
        }

        Transaction tx = transactionBuilder.buildSigned();

        // tx nonce is validated in advance to avoid silently pushing the tx into
        // delayed queue of pending manager
        if ((validateNonce != null && validateNonce)
                && tx.getNonce() != kernel.getPendingManager().getNonce(tx.getFrom())) {
            throw new IllegalArgumentException("Invalid transaction nonce.");
        }
        return tx;

    }

    /**
     * Constructs a transaction and adds it to pending manager.
     *
     * @param type
     * @param from
     * @param to
     * @param value
     * @param fee
     * @param nonce
     * @param validateNonce
     * @param data
     * @return
     */
    private Response doTransaction(TransactionType type, String from, String to, String value, String fee, String nonce,
            Boolean validateNonce, String data, String gasPrice, String gas) {
        DoTransactionResponse resp = new DoTransactionResponse();
        try {
            Transaction tx = getTransaction(type, from, to, value, fee, nonce, validateNonce, data, gasPrice, gas);

            PendingManager.ProcessTransactionResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return badRequest(resp, "Transaction rejected by pending manager: " + result.error.toString());
            }

            resp.setResult(Hex.encode0x(tx.getHash()));

            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(resp, ex.getMessage());
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
