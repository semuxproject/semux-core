/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

/**
 * Interface for calling from REST API or command terminal.
 */
public interface SemuxApi {

    ApiHandlerResponse failure(String message);

    ApiHandlerResponse getInfo();

    ApiHandlerResponse getPeers();

    ApiHandlerResponse addNode(String node);

    ApiHandlerResponse addToBlacklist(String ipAddress);

    ApiHandlerResponse addToWhitelist(String ipAddress);

    ApiHandlerResponse getLatestBlockNumber();

    ApiHandlerResponse getLatestBlock();

    ApiHandlerResponse getBlock(long blockNum);

    ApiHandlerResponse getBlock(String hash);

    ApiHandlerResponse getPendingTransactions();

    ApiHandlerResponse getAccountTransactions(String address, String from, String to);

    ApiHandlerResponse getTransaction(String hash);

    ApiHandlerResponse sendTransaction(String raw);

    ApiHandlerResponse getAccount(String address);

    ApiHandlerResponse getDelegate(String delegate);

    ApiHandlerResponse getDelegates();

    ApiHandlerResponse getValidators();

    ApiHandlerResponse getVotes(String delegate, String voterAddress);

    ApiHandlerResponse getVotes(String delegate);

    ApiHandlerResponse listAccounts();

    ApiHandlerResponse createAccount();

    ApiHandlerResponse transfer(String from, String to, String value, String fee, String data);

    ApiHandlerResponse registerDelegate(String fromAddress, String fee, String delegateName);

    ApiHandlerResponse vote(String from, String to, String value, String fee);

    ApiHandlerResponse unvote(String from, String to, String value, String fee);

    ApiHandlerResponse getTransactionLimits(String type);

}
