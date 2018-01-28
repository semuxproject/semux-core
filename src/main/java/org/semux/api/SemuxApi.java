/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.semux.config.Constants.JSON_MIME;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
import org.semux.api.response.VerifyMessageResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 * Interface defining Semux API
 */
@Path("/")
@Api(value = "/", authorizations = {
        @Authorization(value = "basicAuth")
})
public interface SemuxApi {

    ApiHandlerResponse failure(@QueryParam("message") String message);

    @GET
    @Path("get_info")
    @ApiOperation(value = "Get info", notes = "Returns kernel info.", response = GetInfoResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getInfo();

    @GET
    @Path("get_peers")
    @ApiOperation(value = "Get peers", notes = "Returns connected peers.", response = GetPeersResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getPeers();

    @GET
    @Path("add_node")
    @ApiOperation(value = "Add node", notes = "Adds a node to node manager.", response = AddNodeResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse addNode(
            @ApiParam(value = "Name of the node in host:port format", required = true) @QueryParam("node") String node);

    @GET
    @Path("add_to_blacklist")
    @ApiOperation(value = "Add to blacklist", notes = "Adds an IP address to blacklist.", response = ApiHandlerResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse addToBlacklist(
            @ApiParam(value = "IP address", required = true) @QueryParam("ip") String ipAddress);

    @GET
    @Path("add_to_whitelist")
    @ApiOperation(value = "Add to Whitelist", notes = "Adds an IP address to whitelist.", response = ApiHandlerResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse addToWhitelist(
            @ApiParam(value = "IP address", required = true) @QueryParam("ip") String ipAddress);

    @GET
    @Path("get_latest_block_number")
    @ApiOperation(value = "Get latest block number", notes = "Returns the number of the latest block.", response = GetLatestBlockNumberResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getLatestBlockNumber();

    @GET
    @Path("get_latest_block")
    @ApiOperation(value = "Get latest block", notes = "Returns the latest block.", response = GetLatestBlockResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getLatestBlock();

    @GET
    @Path("get_block")
    @ApiOperation(value = "Get block", notes = "Returns a block.", response = GetBlockResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getBlock(@ApiParam(value = "Block number", required = true) @QueryParam("number") Long blockNum);

    @GET
    @Path("get_block")
    @ApiOperation(value = "Get block", notes = "Returns a block.", response = GetBlockResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getBlock(@ApiParam(value = "Hash of block", required = true) @QueryParam("hash") String hash);

    @GET
    @Path("get_pending_transactions")
    @ApiOperation(value = "Get pending transactions", notes = "Returns all the pending transactions.", response = GetPendingTransactionsResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getPendingTransactions();

    @GET
    @Path("get_account_transactions")
    @ApiOperation(value = "Get account transactions", notes = "Returns transactions from/to an account.", response = GetAccountTransactionsResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getAccountTransactions(
            @ApiParam(value = "Address of account", required = true) @QueryParam("address") String address,
            @ApiParam(value = "Starting range of transactions", required = true) @QueryParam("from") String from,
            @ApiParam(value = "Ending range of transactions", required = true) @QueryParam("to") String to);

    @GET
    @Path("get_transaction")
    @ApiOperation(value = "Get transaction", notes = "Returns a transactions if exists.", response = GetTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getTransaction(
            @ApiParam(value = "Transaction hash", required = true) @QueryParam("hash") String hash);

    @GET
    @Path("send_transaction")
    @ApiOperation(value = "Send a raw transaction", notes = "Broadcasts a raw transaction to the network.", response = SendTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse sendTransaction(
            @ApiParam(value = "Raw transaction", required = true) @QueryParam("raw") String raw);

    @GET
    @Path("get_account")
    @ApiOperation(value = "Get account", notes = "Returns an account.", response = GetAccountResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getAccount(
            @ApiParam(value = "Address of account", required = true) @QueryParam("address") String address);

    @GET
    @Path("get_delegate")
    @ApiOperation(value = "Get a delegate", notes = "Returns a delegate.", response = GetDelegateResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getDelegate(
            @ApiParam(value = "Delegate address", required = true) @QueryParam("address") String delegate);

    @GET
    @Path("get_delegates")
    @ApiOperation(value = "Get all delegates", notes = "Returns a list of delegates.", response = GetDelegatesResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getDelegates();

    @GET
    @Path("get_validators")
    @ApiOperation(value = "Get valididators", notes = "Returns a list of validators.", response = GetValidatorsResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getValidators();

    @GET
    @Path("get_vote")
    @ApiOperation(value = "Get vote", notes = "Returns the vote from a voter to a delegate.", response = GetVoteResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getVote(
            @ApiParam(value = "Delegate address", required = true) @QueryParam("delegate") String delegate,
            @ApiParam(value = "Voter address", required = true) @QueryParam("voter") String voterAddress);

    @GET
    @Path("get_votes")
    @ApiOperation(value = "Get votes", notes = "Returns all the votes to a delegate", response = GetVotesResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getVotes(
            @ApiParam(value = "Delegate address", required = true) @QueryParam("delegate") String delegate);

    @GET
    @Path("list_accounts")
    @ApiOperation(value = "List accounts", notes = "Returns accounts in the wallet.", response = ListAccountsResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse listAccounts();

    @GET
    @Path("create_account")
    @ApiOperation(value = "Create account", notes = "Creates a new account.", response = CreateAccountResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse createAccount();

    @GET
    @Path("transfer")
    @ApiOperation(value = "Transfer coins", notes = "Transfers coins to another address.", response = DoTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse transfer(
            @ApiParam(value = "Amount of SEM to transfer", required = true) @QueryParam("value") String amountToSend,
            @ApiParam(value = "Sending address", required = true) @QueryParam("from") String from,
            @ApiParam(value = "Receiving address", required = true) @QueryParam("to") String to,
            @ApiParam(value = "Transaction fee", required = true) @QueryParam("fee") String fee,
            @ApiParam(value = "Transaction data", required = true) @QueryParam("data") String data);

    @GET
    @Path("delegate")
    @ApiOperation(value = "Register delegate", notes = "Registers as a delegate", response = DoTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse registerDelegate(
            @ApiParam(value = "Registering address", required = true) @QueryParam("from") String fromAddress,
            @ApiParam(value = "Transaction fee", required = true) @QueryParam("fee") String fee,
            @ApiParam(value = "Delegate name", required = true) @QueryParam("data") String delegateName);

    @GET
    @Path("vote")
    @ApiOperation(value = "Vote", notes = "Votes for a delegate.", response = DoTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse vote(@ApiParam(value = "Voting address", required = true) @QueryParam("from") String from,
            @ApiParam(value = "Delegate address", required = true) @QueryParam("to") String to,
            @ApiParam(value = "Vote amount", required = true) @QueryParam("value") String value,
            @ApiParam(value = "Transaction fee", required = true) @QueryParam("fee") String fee);

    @GET
    @Path("unvote")
    @ApiOperation(value = "Unvote", notes = "Unvotes for a delegate.", response = DoTransactionResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse unvote(@ApiParam(value = "Voting address", required = true) @QueryParam("from") String from,
            @ApiParam(value = "Delegate address", required = true) @QueryParam("to") String to,
            @ApiParam(value = "Vote amount", required = true) @QueryParam("value") String value,
            @ApiParam(value = "Transaction fee", required = true) @QueryParam("fee") String fee);

    @GET
    @Path("get_transaction_limits")
    @ApiOperation(value = "Get transaction limits", notes = "Get minimum fee and maximum size.", response = GetTransactionLimitsResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getTransactionLimits(
            @ApiParam(value = "Type of transaction", required = true) @QueryParam("type") String type);

    @GET
    @Path("sign_message")
    @ApiOperation(value = "Sign a message", notes = "Sign a message.", response = SignMessageResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse signMessage(
            @ApiParam(value = "Signing address", required = true) @QueryParam("address") String address,
            @ApiParam(value = "Message to sign", required = true) @QueryParam("message") String message);

    @GET
    @Path("verify_message")
    @ApiOperation(value = "Verify a message", notes = "Verify a signed message.", response = VerifyMessageResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse verifyMessage(
            @ApiParam(value = "Address", required = true) @QueryParam("address") String address,
            @ApiParam(value = "Message", required = true) @QueryParam("message") String message,
            @ApiParam(value = "Signature to verify", required = true) @QueryParam("signature") String signature);

}
