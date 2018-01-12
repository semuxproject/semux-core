/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

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
import org.semux.api.response.GetVotesResponse;
import org.semux.api.response.ListAccountsResponse;
import org.semux.api.response.SendTransactionResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

/**
 * Interface defining Semux API
 */
@Path("/")
@Api(value = "/", authorizations = {
        @Authorization(value = "basicAuth")
})
public interface SemuxApi {

    // TODO: add query parameter descriptions.

    String JSON = "application/json";

    ApiHandlerResponse failure(@QueryParam("message") String message);

    @GET
    @Path("get_info")
    @ApiOperation(value = "Get info", notes = "Returns kernel info.", response = GetInfoResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getInfo();

    @GET
    @Path("get_peers")
    @ApiOperation(value = "Get peers", notes = "Returns connected peers.", response = GetPeersResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getPeers();

    @GET
    @Path("add_node")
    @ApiOperation(value = "Add node", notes = "Adds a node to connect.", response = AddNodeResponse.class)
    @Produces(JSON)
    ApiHandlerResponse addNode(@QueryParam("node") String node);

    @GET
    @Path("add_to_blacklist")
    @ApiOperation(value = "Add to blacklist", notes = "Adds an IP address to blacklist.", response = ApiHandlerResponse.class)
    @Produces(JSON)
    ApiHandlerResponse addToBlacklist(@QueryParam("ip") String ipAddress);

    @GET
    @Path("add_to_whitelist")
    @ApiOperation(value = "Add to Whitelist", notes = "Adds an IP address to whitelist.", response = ApiHandlerResponse.class)
    @Produces(JSON)
    ApiHandlerResponse addToWhitelist(@QueryParam("ip") String ipAddress);

    @GET
    @Path("get_latest_block_number")
    @ApiOperation(value = "Get latest block number", notes = "Returns the number of the latest block.", response = GetLatestBlockNumberResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getLatestBlockNumber();

    @GET
    @Path("get_latest_block")
    @ApiOperation(value = "Get latest block", notes = "Returns the latest block.", response = GetLatestBlockResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getLatestBlock();

    @GET
    @Path("get_block")
    @ApiOperation(value = "Get block", notes = "Returns a block.", response = GetBlockResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getBlock(@QueryParam("number") Long blockNum);

    @GET
    @Path("get_block")
    @ApiOperation(value = "Get block", notes = "Returns a block.", response = GetBlockResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getBlock(@QueryParam("hash") String hash);

    @GET
    @Path("get_pending_transactions")
    @ApiOperation(value = "Get pending transactions", notes = "Returns all the pending transactions.", response = GetPendingTransactionsResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getPendingTransactions();

    @GET
    @Path("get_account_transactions")
    @ApiOperation(value = "Get account transactions", notes = "Returns transactions from/to an account.", response = GetAccountTransactionsResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getAccountTransactions(@QueryParam("address") String address, @QueryParam("from") String from,
            @QueryParam("to") String to);

    @GET
    @Path("get_transaction")
    @ApiOperation(value = "Get transaction", notes = "Returns a transactions if exists.", response = GetTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getTransaction(@QueryParam("hash") String hash);

    @GET
    @Path("send_transaction")
    @ApiOperation(value = "Send a raw transaction", notes = "Broadcast a raw transaction to the network.", response = SendTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse sendTransaction(@QueryParam("raw") String raw);

    @GET
    @Path("get_account")
    @ApiOperation(value = "Get account", notes = "Returns an account.", response = GetAccountResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getAccount(@QueryParam("address") String address);

    @GET
    @Path("get_delegate")
    @ApiOperation(value = "Get a delegate", notes = "Returns a delegate.", response = GetDelegateResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getDelegate(@QueryParam("address") String delegate);

    @GET
    @Path("get_delegates")
    @ApiOperation(value = "Get all delegates", notes = "Returns a list of delegates.", response = GetDelegatesResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getDelegates();

    @GET
    @Path("get_validators")
    @ApiOperation(value = "Get valididators", notes = "Returns a list of validators.", response = GetValidatorsResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getValidators();

    @GET
    @Path("get_votes")
    @ApiOperation(value = "Get votes", notes = "Returns a voter one voter has given to a delegate.", response = GetVotesResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getVotes(@QueryParam("delegate") String delegate, @QueryParam("voter") String voterAddress);

    @GET
    @Path("get_votes")
    @ApiOperation(value = "Get votes", notes = "Returns all votes of a delegate", response = GetVotesResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getVotes(@QueryParam("delegate") String delegate);

    @GET
    @Path("list_accounts")
    @ApiOperation(value = "List accounts", notes = "Returns accounts in the wallet.", response = ListAccountsResponse.class)
    @Produces(JSON)
    ApiHandlerResponse listAccounts();

    @GET
    @Path("create_account")
    @ApiOperation(value = "Create account", notes = "Creates a new account.", response = CreateAccountResponse.class)
    @Produces(JSON)
    ApiHandlerResponse createAccount();

    @GET
    @Path("transfer")
    @ApiOperation(value = "Transfer coins", notes = "Transfers coins to another address.", response = DoTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse transfer(@QueryParam("value") String amountToSend, @QueryParam("from") String from,
            @QueryParam("to") String to, @QueryParam("fee") String fee, @QueryParam("data") String data);

    @GET
    @Path("delegate")
    @ApiOperation(value = "Register delegate", notes = "Registers as a delegate", response = DoTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse registerDelegate(@QueryParam("from") String fromAddress, @QueryParam("fee") String fee,
            @QueryParam("data") String delegateName);

    @GET
    @Path("vote")
    @ApiOperation(value = "Vote", notes = "Votes for a delegate.", response = DoTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse vote(@QueryParam("from") String from, @QueryParam("to") String to,
            @QueryParam("value") String value, @QueryParam("fee") String fee);

    @GET
    @Path("unvote")
    @ApiOperation(value = "Unvote", notes = "Unvotes for a delegate.", response = DoTransactionResponse.class)
    @Produces(JSON)
    ApiHandlerResponse unvote(@QueryParam("from") String from, @QueryParam("to") String to,
            @QueryParam("value") String value, @QueryParam("fee") String fee);

    @GET
    @Path("get_transaction_limits")
    @ApiOperation(value = "Get transaction limits", notes = "Get minimum fee and maximum size.", response = GetTransactionLimitsResponse.class)
    @Produces(JSON)
    ApiHandlerResponse getTransactionLimits(@QueryParam("type") String type);

}
