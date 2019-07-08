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
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.TransactionReceipt;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.semux.Kernel;
import org.semux.api.util.TransactionBuilder;
import org.semux.api.v2.model.AddNodeResponse;
import org.semux.api.v2.model.ApiHandlerResponse;
import org.semux.api.v2.model.CallResponse;
import org.semux.api.v2.model.ComposeRawTransactionResponse;
import org.semux.api.v2.model.CreateAccountResponse;
import org.semux.api.v2.model.DeleteAccountResponse;
import org.semux.api.v2.model.DoTransactionResponse;
import org.semux.api.v2.model.EstimateGasResponse;
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
import org.semux.api.v2.model.GetTransactionResultResponse;
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
import org.semux.core.TransactionResult;
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
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;
import org.semux.vm.client.SemuxRepository;
import org.semux.vm.client.SemuxTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

public final class SemuxApiImpl implements SemuxApi {

    private static final Logger logger = LoggerFactory.getLogger(SemuxApiImpl.class);

    private static final Charset CHARSET = UTF_8;

    private final Kernel kernel;

    public SemuxApiImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public Response addNode(String node) {
        try {
            kernel.getNodeManager().addNode(validateAddNodeParameter(node));

            AddNodeResponse resp = new AddNodeResponse();
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response addToBlacklist(String ip) {
        try {
            String blacklistIp = parseIp(ip, true);

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.blacklistIp(blacklistIp);
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());
            kernel.getChannelManager().closeBlacklistedChannels();

            ApiHandlerResponse resp = new ApiHandlerResponse();
            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @Override
    public Response addToWhitelist(String ip) {
        try {
            String whitelistIp = parseIp(ip, true);

            SemuxIpFilter ipFilter = kernel.getChannelManager().getIpFilter();
            ipFilter.whitelistIp(whitelistIp);
            ipFilter.persist(new File(kernel.getConfig().configDir(), SemuxIpFilter.CONFIG_FILE).toPath());

            ApiHandlerResponse resp = new ApiHandlerResponse();
            return Response.ok().entity(resp.success(true)).build();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @Override
    public Response composeRawTransaction(String network, String type, String to, String value, String fee,
            String nonce, String timestamp, String data, String gas, String gasPrice) {
        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                    .withNetwork(network)
                    .withType(type)
                    .withTo(to)
                    .withValue(value)
                    .withFee(fee)
                    .withNonce(nonce)
                    .withTimestamp(timestamp)
                    .withData(data)
                    .withGas(gas)
                    .withGasPrice(gasPrice);
            Transaction transaction = transactionBuilder.buildUnsigned();

            ComposeRawTransactionResponse resp = new ComposeRawTransactionResponse();
            resp.setResult(Hex.encode0x(transaction.getEncoded()));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response createAccount(String name, String privateKey) {
        try {
            Key key;
            if (privateKey != null) { // import
                byte[] privateKeyBytes = Hex.decode0x(privateKey);
                key = new Key(privateKeyBytes);
            } else { // generate
                key = new Key();
            }

            if (!kernel.getWallet().addAccount(key)) {
                return badRequest("The key already exists in this wallet.");
            }

            // set alias of the address
            if (name != null) {
                kernel.getWallet().setAddressAlias(key.toAddress(), name);
            }

            // save the account
            kernel.getWallet().flush();

            CreateAccountResponse resp = new CreateAccountResponse();
            resp.setResult(Hex.PREF + key.toAddressString());
            return success(resp);
        } catch (CryptoException ex) {
            return badRequest("Parameter `privateKey` is not a valid hexadecimal string");
        } catch (InvalidKeySpecException e) {
            return badRequest(
                    "Parameter `privateKey` is not a valid ED25519 private key encoded in PKCS#8 format");
        } catch (WalletLockedException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getAccount(String address) {
        try {
            byte[] addressBytes = parseAddress(address, true);

            Account account = kernel.getBlockchain().getAccountState().getAccount(addressBytes);
            int transactionCount = kernel.getBlockchain().getTransactionCount(account.getAddress());
            int pendingTransactionCount = (int) kernel.getPendingManager()
                    .getPendingTransactions().parallelStream()
                    .map(pendingTransaction -> pendingTransaction.transaction)
                    .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                    .count();

            GetAccountResponse resp = new GetAccountResponse();
            resp.setResult(TypeFactory.accountType(account, transactionCount, pendingTransactionCount));
            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @Override
    public Response deleteAccount(String address) {
        try {
            byte[] addressBytes = parseAddress(address, true);

            if (!kernel.getWallet().removeAccount(addressBytes)) {
                return badRequest("The provided address doesn't exist in this wallet.");
            }

            if (!kernel.getWallet().flush()) {
                return badRequest("Failed to write the wallet.");
            }

            DeleteAccountResponse resp = new DeleteAccountResponse();
            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (WalletLockedException e) {
            return badRequest("Wallet is locked");
        }
    }

    @Override
    public Response getAccountTransactions(String address, String from, String to) {

        try {
            byte[] addressBytes = parseAddress(address, true);
            int fromInt = parseInt(from, true, "from");
            int toInt = parseInt(to, true, "to");

            if (toInt <= fromInt) {
                return badRequest("Parameter `to` must be greater than `from`");
            }

            GetAccountTransactionsResponse resp = new GetAccountTransactionsResponse();
            resp.setResult(kernel.getBlockchain().getTransactions(addressBytes, fromInt, toInt).parallelStream()
                    .map(tx -> TypeFactory.transactionType(tx))
                    .collect(Collectors.toList()));
            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @Override
    public Response getAccountPendingTransactions(String address, String from, String to) {
        try {
            byte[] addressBytes = parseAddress(address, true);
            int fromInt = parseInt(from, true, "from");
            int toInt = parseInt(to, true, "to");

            if (toInt <= fromInt) {
                return badRequest("Parameter `to` must be greater than `from`");
            }

            GetAccountPendingTransactionsResponse resp = new GetAccountPendingTransactionsResponse();
            resp.setResult(kernel.getPendingManager()
                    .getPendingTransactions().parallelStream()
                    .map(pendingTransaction -> pendingTransaction.transaction)
                    .filter(tx -> Arrays.equals(tx.getFrom(), addressBytes) || Arrays.equals(tx.getTo(), addressBytes))
                    .skip(fromInt)
                    .limit(toInt - fromInt)
                    .map(TypeFactory::transactionType)
                    .collect(Collectors.toList()));
            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @Override
    public Response getAccountVotes(String address) {
        try {
            byte[] addressBytes = parseAddress(address, true);

            GetAccountVotesResponse resp = new GetAccountVotesResponse();
            resp.setResult(TypeFactory.accountVotes(kernel.getBlockchain(), addressBytes));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getBlockByHash(String hashString) {
        try {
            byte[] hash = parseHash(hashString, true);

            Block block = kernel.getBlockchain().getBlock(hash);
            if (block == null) {
                return badRequest("The requested block was not found");
            }

            GetBlockResponse resp = new GetBlockResponse();
            resp.setResult(
                    TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getBlockByNumber(String blockNum) {
        try {
            long blockNumLong = parseInt(blockNum, true, "number");

            Block block = kernel.getBlockchain().getBlock(blockNumLong);
            if (block == null) {
                return badRequest("The requested block was not found");
            }

            GetBlockResponse resp = new GetBlockResponse();
            resp.setResult(
                    TypeFactory.blockType(block, kernel.getBlockchain().getCoinbaseTransaction(block.getNumber())));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getDelegate(String address) {
        try {
            byte[] addressBytes = parseAddress(address, true);

            Blockchain chain = kernel.getBlockchain();
            Delegate delegate = chain.getDelegateState().getDelegateByAddress(addressBytes);
            if (delegate == null) {
                return badRequest("The provided address is not a delegate");
            }

            BlockchainImpl.ValidatorStats validatorStats = chain.getValidatorStats(addressBytes);
            boolean isValidator = chain.getValidators().contains(address.replace("0x", ""));

            GetDelegateResponse resp = new GetDelegateResponse();
            resp.setResult(TypeFactory.delegateType(validatorStats, delegate, isValidator));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
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
                .map(TypeFactory::transactionType)
                .collect(Collectors.toList()));

        return success(resp);
    }

    @Override
    public Response getTransaction(String hash) {
        try {
            byte[] hashBytes = parseHash(hash, true);

            Transaction transaction = kernel.getBlockchain().getTransaction(hashBytes);
            if (transaction == null) {
                return badRequest("The request transaction was not found");
            }

            GetTransactionResponse resp = new GetTransactionResponse();
            resp.setResult(TypeFactory.transactionType(transaction));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getTransactionLimits(String type) {
        try {
            GetTransactionLimitsResponse resp = new GetTransactionLimitsResponse();
            resp.setResult(TypeFactory.transactionLimitsType(kernel, TransactionType.valueOf(type)));
            return success(resp);
        } catch (NullPointerException | IllegalArgumentException e) {
            return badRequest(String.format("Invalid transaction type"));
        }
    }

    @Override
    public Response getTransactionResult(String hash) {
        try {
            byte[] hashBytes = parseHash(hash, true);
            long number = kernel.getBlockchain().getTransactionBlockNumber(hashBytes);
            Transaction tx = kernel.getBlockchain().getTransaction(hashBytes);
            TransactionResult result = kernel.getBlockchain().getTransactionResult(hashBytes);
            if (result == null) {
                return badRequest("The request transaction was not found");
            }

            GetTransactionResultResponse resp = new GetTransactionResultResponse();
            resp.setResult(TypeFactory.transactionResultType(tx, result, number));
            return success(resp);
        } catch (NullPointerException | IllegalArgumentException e) {
            return badRequest(String.format("Invalid transaction type"));
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
        try {
            byte[] voterBytes = parseAddress(voter, true, "voter");
            byte[] delegateBytes = parseAddress(delegate, true, "delegate");

            GetVoteResponse resp = new GetVoteResponse();
            resp.setResult(
                    TypeFactory.encodeAmount(
                            kernel.getBlockchain().getDelegateState().getVote(voterBytes, delegateBytes)));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response getVotes(String delegate) {
        try {
            byte[] delegateBytes = parseAddress(delegate, true, "delegate");

            GetVotesResponse resp = new GetVotesResponse();
            resp.setResult(kernel.getBlockchain().getDelegateState().getVotes(delegateBytes).entrySet().parallelStream()
                    .collect(Collectors.toMap(
                            entry -> Hex.PREF + entry.getKey().toString(),
                            entry -> TypeFactory.encodeAmount(entry.getValue()))));
            return success(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
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
    public Response broadcastRawTransaction(String raw) {
        try {
            if (raw == null) {
                return badRequest("Parameter `raw` is required");
            }

            Transaction tx = Transaction.fromBytes(Hex.decode0x(raw));

            PendingManager.ProcessingResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return badRequest("Transaction rejected by pending manager: " + result.error.toString());
            }

            DoTransactionResponse resp = new DoTransactionResponse();
            resp.setResult(Hex.encode0x(tx.getHash()));
            return success(resp);
        } catch (CryptoException e) {
            return badRequest("Parameter `raw` is not a valid hexadecimal string");
        } catch (IndexOutOfBoundsException e) {
            return badRequest("Parameter `raw` is not a valid hexadecimal raw transaction");
        }
    }

    @Override
    public Response signMessage(String address, String message) {
        try {
            byte[] addressBytes = parseAddress(address, true);
            Key account = kernel.getWallet().getAccount(addressBytes);
            if (account == null) {
                return badRequest(
                        String.format("The provided address %s doesn't belong to the wallet", address));
            }

            if (message == null) {
                return badRequest("Parameter `message` is required");
            }

            Key.Signature signedMessage = account.sign(message.getBytes(CHARSET));

            SignMessageResponse resp = new SignMessageResponse();
            resp.setResult(Hex.encode0x(signedMessage.toBytes()));
            return success(resp);
        } catch (NullPointerException | IllegalArgumentException e) {
            return badRequest("Invalid message");
        }
    }

    @Override
    public Response signRawTransaction(String raw, String address) {
        try {
            byte[] txBytes;
            try {
                txBytes = Hex.decode0x(raw);
            } catch (CryptoException ex) {
                return badRequest("Parameter `raw` is not a hexadecimal string.");
            }

            byte[] addressBytes = parseAddress(address, true);
            Key signerKey = kernel.getWallet().getAccount(addressBytes);
            if (signerKey == null) {
                return badRequest("Address doesn't belong to this wallet.");
            }

            Transaction tx = Transaction.fromEncoded(txBytes).sign(signerKey);

            SignRawTransactionResponse resp = new SignRawTransactionResponse();
            resp.setResult(Hex.encode0x(tx.toBytes()));
            return success(resp);
        } catch (IndexOutOfBoundsException ex) {
            return badRequest("Parameter `raw` is not a valid raw transaction.");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Override
    public Response transfer(String from, String to, String value, String fee, String nonce, String data) {
        return doTransaction(TransactionType.TRANSFER, from, to, value, fee, nonce, data, null, null);
    }

    @Override
    public Response registerDelegate(String from, String data, String fee, String nonce) {
        return doTransaction(TransactionType.DELEGATE, from, null, null, fee, nonce, data, null, null);
    }

    @Override
    public Response create(String from, String data, String gas, String gasPrice, String value, String nonce) {
        return doTransaction(TransactionType.CREATE, from, null, value, null, nonce, data, gas, gasPrice);
    }

    @Override
    public Response call(String from, String to, String gas, String gasPrice, String value, String nonce, String data) {
        return doTransaction(TransactionType.CALL, from, to, value, "0", nonce, data, gas, gasPrice);
    }

    @Override
    public Response vote(String from, String to, String value, String fee, String nonce) {
        return doTransaction(TransactionType.VOTE, from, to, value, fee, nonce, null, null, null);
    }

    @Override
    public Response unvote(String from, String to, String value, String fee, String nonce) {
        return doTransaction(TransactionType.UNVOTE, from, to, value, fee, nonce, null, null, null);
    }

    private TransactionReceipt executeTransactionLocally(String to, String from, String value, String nonce,
            String data, String gas, String gasPrice) {
        TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                .withType("CALL")
                .withTo(to)
                .withFrom(from)
                .withValue(value)
                .withNonce(nonce)
                .withData(data);

        if (gas == null) {
            transactionBuilder.withGas(Long.toString(kernel.getConfig().spec().maxBlockGasLimit()));
        }
        if (gasPrice == null) {
            transactionBuilder.withGasPrice("1");
        }

        SemuxTransaction transaction = new SemuxTransaction(transactionBuilder.buildUnsigned());
        SemuxBlock block = new SemuxBlock(kernel.getBlockchain().getLatestBlock().getHeader(),
                kernel.getConfig().spec().maxBlockGasLimit());
        Repository repository = new SemuxRepository(kernel.getBlockchain().getAccountState(),
                kernel.getBlockchain().getDelegateState());
        ProgramInvokeFactory invokeFactory = new ProgramInvokeFactoryImpl();
        BlockStore blockStore = new SemuxBlockStore(kernel.getBlockchain());
        long gasUsedInBlock = 0l;

        org.ethereum.vm.client.TransactionExecutor executor = new org.ethereum.vm.client.TransactionExecutor(
                transaction, block, repository, blockStore,
                kernel.getConfig().spec().vmSpec(), invokeFactory, gasUsedInBlock, true);
        TransactionReceipt receipt = executor.run();

        return receipt;
    }

    @Override
    public Response localCall(String to, String from, String value, String nonce, String data, String gas,
            String gasPrice) {
        TransactionReceipt receipt = executeTransactionLocally(to, from, value, nonce, data, gas, gasPrice);
        if (receipt == null || !receipt.isSuccess()) {
            return badRequest("Failed to call");
        } else {
            CallResponse resp = new CallResponse();
            resp.setResult(Hex.encode0x(receipt.getReturnData()));
            return success(resp);
        }
    }

    @Override
    public Response estimateGas(String to, String from, String value, String nonce, String data, String gas,
            String gasPrice) {
        TransactionReceipt receipt = executeTransactionLocally(to, from, value, nonce, data, gas, gasPrice);
        if (receipt == null || !receipt.isSuccess()) {
            return badRequest("Failed to estimate the gas usage");
        } else {
            EstimateGasResponse resp = new EstimateGasResponse();
            resp.setResult(Long.toString(receipt.getGasUsed()));
            return success(resp);
        }
    }

    @Override
    public Response verifyMessage(String address, String message, String signature) {

        if (address == null) {
            return badRequest("Parameter `address` is required");
        }

        if (message == null) {
            return badRequest("Parameter `message` is required");
        }

        if (signature == null) {
            return badRequest("Parameter `signature` is required");
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

        VerifyMessageResponse resp = new VerifyMessageResponse();
        resp.setValid(isValidSignature);
        return success(resp);
    }

    @Override
    public Response getSyncingProgress() {
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

        GetSyncingProgressResponse resp = new GetSyncingProgressResponse();
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
     * @param message
     * @return
     */
    private Response badRequest(String message) {
        ApiHandlerResponse resp = new ApiHandlerResponse();
        resp.setSuccess(false);
        resp.setMessage(message);

        logger.info("Bad request: {}", message);
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
        if (node == null) {
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

    private Transaction getTransaction(TransactionType type, String from, String to, String value, String fee,
            String nonce, String data, String gas, String gasPrice) {
        TransactionBuilder transactionBuilder = new TransactionBuilder(kernel)
                .withType(type)
                .withFrom(from)
                .withTo(to)
                .withValue(value)
                .withFee(fee)
                .withNonce(nonce)
                .withData(data)
                .withGas(gas)
                .withGasPrice(gasPrice);

        Transaction tx = transactionBuilder.buildSigned();

        return tx;
    }

    private Response doTransaction(TransactionType type, String from, String to, String value, String fee,
            String nonce, String data, String gas, String gasPrice) {
        DoTransactionResponse resp = new DoTransactionResponse();
        try {
            Transaction tx = getTransaction(type, from, to, value, fee, nonce, data, gas, gasPrice);

            PendingManager.ProcessingResult result = kernel.getPendingManager().addTransactionSync(tx);
            if (result.error != null) {
                return badRequest("Transaction rejected by pending manager: " + result.error.toString());
            }

            resp.setResult(Hex.encode0x(tx.getHash()));

            return success(resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    private static final String IP_ADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private String parseIp(String ip, boolean required) {
        if (ip == null) {
            if (required) {
                throw new IllegalArgumentException("Parameter `ip` is required");
            } else {
                return null;
            }
        } else {
            if (ip.matches(IP_ADDRESS_PATTERN)) {
                return ip;
            } else {
                throw new IllegalArgumentException("Parameter `ip` is invalid");
            }
        }
    }

    private byte[] parseAddress(String address, boolean required) {
        return parseAddress(address, required, "address");
    }

    private byte[] parseAddress(String address, boolean required, String name) {
        if (address == null) {
            if (required) {
                throw new IllegalArgumentException("Parameter `" + name + "` is required");
            } else {
                return null;
            }
        }

        try {
            byte[] bytes = Hex.decode0x(address);

            if (bytes.length != Key.ADDRESS_LEN) {
                throw new IllegalArgumentException("Parameter `" + name + "` length is invalid");
            }

            return bytes;
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `" + name + "` is not a valid hexadecimal string");
        }
    }

    private byte[] parseHash(String hash, boolean required) {
        if (hash == null) {
            if (required) {
                throw new IllegalArgumentException("Parameter `hash` is required");
            } else {
                return null;
            }
        }

        try {
            byte[] bytes = Hex.decode0x(hash);

            if (bytes.length != Hash.HASH_LEN) {
                throw new IllegalArgumentException("Parameter `hash` length is invalid");
            }

            return bytes;
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `hash` is not a valid hexadecimal string");
        }
    }

    private Integer parseInt(String num, boolean required, String name) {
        if (num == null) {
            if (required) {
                throw new IllegalArgumentException("Parameter `" + name + "` is required");
            } else {
                return null;
            }
        } else {
            try {
                return Integer.parseInt(num);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter `" + name + "` is not a valid hexadecimal string");
            }
        }
    }
}