package org.semux.api.v1_1_0.api;

import org.semux.api.v1_1_0.model.AddNodeResponse;
import org.semux.api.v1_1_0.model.ApiHandlerResponse;
import org.semux.api.v1_1_0.model.CreateAccountResponse;
import org.semux.api.v1_1_0.model.DoTransactionResponse;
import org.semux.api.v1_1_0.model.GetAccountResponse;
import org.semux.api.v1_1_0.model.GetAccountTransactionsResponse;
import org.semux.api.v1_1_0.model.GetBlockResponse;
import org.semux.api.v1_1_0.model.GetDelegateResponse;
import org.semux.api.v1_1_0.model.GetDelegatesResponse;
import org.semux.api.v1_1_0.model.GetInfoResponse;
import org.semux.api.v1_1_0.model.GetLatestBlockNumberResponse;
import org.semux.api.v1_1_0.model.GetLatestBlockResponse;
import org.semux.api.v1_1_0.model.GetPeersResponse;
import org.semux.api.v1_1_0.model.GetPendingTransactionsResponse;
import org.semux.api.v1_1_0.model.GetRootResponse;
import org.semux.api.v1_1_0.model.GetTransactionLimitsResponse;
import org.semux.api.v1_1_0.model.GetTransactionResponse;
import org.semux.api.v1_1_0.model.GetValidatorsResponse;
import org.semux.api.v1_1_0.model.GetVoteResponse;
import org.semux.api.v1_1_0.model.GetVotesResponse;
import org.semux.api.v1_1_0.model.ListAccountsResponse;
import org.semux.api.v1_1_0.model.SendTransactionResponse;
import org.semux.api.v1_1_0.model.SignMessageResponse;
import org.semux.api.v1_1_0.model.VerifyMessageResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;
import io.swagger.jaxrs.PATCH;
import javax.validation.constraints.*;
import javax.validation.Valid;

/**
 * Semux
 *
 * <p>Semux is an experimental high-performance blockchain platform that powers decentralized application.
 *
 */
@Path("/v1.0.2")
@Api(value = "/", description = "")
public interface SemuxApi  {

    /**
     * Add node
     *
     * Adds a node to node manager.
     *
     */
    @GET
    @Path("/add_node")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Add node", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = AddNodeResponse.class) })
    public Response addNode(@QueryParam("node") @NotNull String node);

    /**
     * Add to blacklist
     *
     * Adds an IP address to blacklist.
     *
     */
    @GET
    @Path("/add_to_blacklist")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Add to blacklist", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ApiHandlerResponse.class) })
    public Response addToBlacklist(@QueryParam("ip") @NotNull String ip);

    /**
     * Add to whitelist
     *
     * Adds an IP address to whitelist.
     *
     */
    @GET
    @Path("/add_to_whitelist")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Add to whitelist", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ApiHandlerResponse.class) })
    public Response addToWhitelist(@QueryParam("ip") @NotNull String ip);

    /**
     * Create account
     *
     * Creates a new account.
     *
     */
    @GET
    @Path("/create_account")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create account", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = CreateAccountResponse.class) })
    public Response createAccount(@QueryParam("name") String name);

    /**
     * Get account
     *
     * Returns an account.
     *
     */
    @GET
    @Path("/get_account")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get account", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetAccountResponse.class) })
    public Response getAccount(@QueryParam("address") @NotNull String address);

    /**
     * Get account transactions
     *
     * Returns transactions from/to an account.
     *
     */
    @GET
    @Path("/get_account_transactions")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get account transactions", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetAccountTransactionsResponse.class) })
    public Response getAccountTransactions(@QueryParam("address") @NotNull String address, @QueryParam("from") @NotNull String from, @QueryParam("to") @NotNull String to);

    /**
     * Get block by hash
     *
     * Returns a block by block hash.
     *
     */
    @GET
    @Path("/get_block_by_hash")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get block by hash", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetBlockResponse.class) })
    public Response getBlockByHash(@QueryParam("hash") @NotNull @Pattern(regexp="^(0x)?[0-9a-fA-F]+$") String hash);

    /**
     * Get block by number
     *
     * Returns a block by block number.
     *
     */
    @GET
    @Path("/get_block_by_number")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get block by number", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetBlockResponse.class) })
    public Response getBlockByNumber(@QueryParam("number") @NotNull @Pattern(regexp="^\\d+$") String number);

    /**
     * Get a delegate
     *
     * Returns a delegate.
     *
     */
    @GET
    @Path("/get_delegate")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get a delegate", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetDelegateResponse.class) })
    public Response getDelegate(@QueryParam("address") @NotNull String address);

    /**
     * Get all delegates
     *
     * Returns a list of delegates.
     *
     */
    @GET
    @Path("/get_delegates")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get all delegates", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetDelegatesResponse.class) })
    public Response getDelegates();

    /**
     * Get info
     *
     * Returns kernel info.
     *
     */
    @GET
    @Path("/get_info")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get info", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetInfoResponse.class) })
    public Response getInfo();

    /**
     * Get latest block
     *
     * Returns the latest block.
     *
     */
    @GET
    @Path("/get_latest_block")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get latest block", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetLatestBlockResponse.class) })
    public Response getLatestBlock();

    /**
     * Get latest block number
     *
     * Returns the number of the latest block.
     *
     */
    @GET
    @Path("/get_latest_block_number")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get latest block number", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetLatestBlockNumberResponse.class) })
    public Response getLatestBlockNumber();

    /**
     * Get peers
     *
     * Returns connected peers.
     *
     */
    @GET
    @Path("/get_peers")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get peers", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetPeersResponse.class) })
    public Response getPeers();

    /**
     * Get pending transactions
     *
     * Returns all the pending transactions.
     *
     */
    @GET
    @Path("/get_pending_transactions")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get pending transactions", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetPendingTransactionsResponse.class) })
    public Response getPendingTransactions();

    /**
     * Get root
     *
     */
    @GET
    @Path("/")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get root", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetRootResponse.class) })
    public Response getRoot();

    /**
     * Get transaction
     *
     * Returns a transactions if exists.
     *
     */
    @GET
    @Path("/get_transaction")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get transaction", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetTransactionResponse.class) })
    public Response getTransaction(@QueryParam("hash") @NotNull String hash);

    /**
     * Get transaction limits
     *
     * Get minimum fee and maximum size.
     *
     */
    @GET
    @Path("/get_transaction_limits")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get transaction limits", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetTransactionLimitsResponse.class) })
    public Response getTransactionLimits(@QueryParam("type") @NotNull String type);

    /**
     * Get validators
     *
     * Returns a list of validators.
     *
     */
    @GET
    @Path("/get_validators")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get validators", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetValidatorsResponse.class) })
    public Response getValidators();

    /**
     * Get vote
     *
     * Returns the vote from a voter to a delegate.
     *
     */
    @GET
    @Path("/get_vote")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get vote", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetVoteResponse.class) })
    public Response getVote(@QueryParam("delegate") @NotNull String delegate, @QueryParam("voter") @NotNull String voter);

    /**
     * Get votes
     *
     * Returns all the votes to a delegate
     *
     */
    @GET
    @Path("/get_votes")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get votes", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetVotesResponse.class) })
    public Response getVotes(@QueryParam("delegate") @NotNull String delegate);

    /**
     * List accounts
     *
     * Returns accounts in the wallet.
     *
     */
    @GET
    @Path("/list_accounts")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "List accounts", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ListAccountsResponse.class) })
    public Response listAccounts();

    /**
     * Register delegate
     *
     * Registers as a delegate
     *
     */
    @GET
    @Path("/delegate")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Register delegate", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public Response registerDelegate(@QueryParam("from") @NotNull String from, @QueryParam("fee") @NotNull @Pattern(regexp="^\\d+$") String fee, @QueryParam("data") @NotNull @Pattern(regexp="^(0x)?[0-9a-fA-F]+$") String data);

    /**
     * Send a raw transaction
     *
     * Broadcasts a raw transaction to the network.
     *
     */
    @GET
    @Path("/send_transaction")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Send a raw transaction", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = SendTransactionResponse.class) })
    public Response sendTransaction(@QueryParam("raw") @NotNull @Pattern(regexp="^(0x)?[0-9a-fA-F]+$") String raw);

    /**
     * Sign a message
     *
     * Sign a message.
     *
     */
    @GET
    @Path("/sign_message")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Sign a message", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = SignMessageResponse.class) })
    public Response signMessage(@QueryParam("address") @NotNull String address, @QueryParam("message") @NotNull String message);

    /**
     * Transfer coins
     *
     * Transfers coins to another address.
     *
     */
    @GET
    @Path("/transfer")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Transfer coins", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public Response transfer(@QueryParam("value") @NotNull @Pattern(regexp="^\\d+$") String value, @QueryParam("from") @NotNull String from, @QueryParam("to") @NotNull String to, @QueryParam("fee") @NotNull @Pattern(regexp="^\\d+$") String fee, @QueryParam("data") @NotNull @Pattern(regexp="^(0x)?[0-9a-fA-F]+$") String data);

    /**
     * Unvote
     *
     * Unvotes for a delegate.
     *
     */
    @GET
    @Path("/unvote")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Unvote", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public Response unvote(@QueryParam("from") @NotNull String from, @QueryParam("to") @NotNull String to, @QueryParam("value") @NotNull @Pattern(regexp="^\\d+$") String value, @QueryParam("fee") @NotNull @Pattern(regexp="^\\d+$") String fee);

    /**
     * Verify a message
     *
     * Verify a signed message.
     *
     */
    @GET
    @Path("/verify_message")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Verify a message", tags={ "semux",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = VerifyMessageResponse.class) })
    public Response verifyMessage(@QueryParam("address") @NotNull String address, @QueryParam("message") @NotNull String message, @QueryParam("signature") @NotNull String signature);

    /**
     * Vote
     *
     * Votes for a delegate.
     *
     */
    @GET
    @Path("/vote")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Vote", tags={ "semux" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public Response vote(@QueryParam("from") @NotNull String from, @QueryParam("to") @NotNull String to, @QueryParam("value") @NotNull @Pattern(regexp="^\\d+$") String value, @QueryParam("fee") @NotNull @Pattern(regexp="^\\d+$") String fee);
}

