package org.semux.api.v1_0_2.client;

import org.semux.api.v1_0_2.AddNodeResponse;
import org.semux.api.v1_0_2.ApiHandlerResponse;
import org.semux.api.v1_0_2.CreateAccountResponse;
import org.semux.api.v1_0_2.DoTransactionResponse;
import org.semux.api.v1_0_2.GetAccountResponse;
import org.semux.api.v1_0_2.GetAccountTransactionsResponse;
import org.semux.api.v1_0_2.GetBlockResponse;
import org.semux.api.v1_0_2.GetDelegateResponse;
import org.semux.api.v1_0_2.GetDelegatesResponse;
import org.semux.api.v1_0_2.GetInfoResponse;
import org.semux.api.v1_0_2.GetLatestBlockNumberResponse;
import org.semux.api.v1_0_2.GetLatestBlockResponse;
import org.semux.api.v1_0_2.GetPeersResponse;
import org.semux.api.v1_0_2.GetPendingTransactionsResponse;
import org.semux.api.v1_0_2.GetRootResponse;
import org.semux.api.v1_0_2.GetTransactionLimitsResponse;
import org.semux.api.v1_0_2.GetTransactionResponse;
import org.semux.api.v1_0_2.GetValidatorsResponse;
import org.semux.api.v1_0_2.GetVoteResponse;
import org.semux.api.v1_0_2.GetVotesResponse;
import org.semux.api.v1_0_2.ListAccountsResponse;
import org.semux.api.v1_0_2.SendTransactionResponse;
import org.semux.api.v1_0_2.SignMessageResponse;
import org.semux.api.v1_0_2.VerifyMessageResponse;

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

/**
 * Semux
 *
 * <p>Semux is an experimental high-performance blockchain platform that powers decentralized application.
 *
 */
@Path("/")
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
    @ApiOperation(value = "Add node", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = AddNodeResponse.class) })
    public AddNodeResponse addNode(@QueryParam("node")String node);

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
    @ApiOperation(value = "Add to blacklist", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ApiHandlerResponse.class) })
    public ApiHandlerResponse addToBlacklist(@QueryParam("ip")String ip);

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
    @ApiOperation(value = "Add to whitelist", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ApiHandlerResponse.class) })
    public ApiHandlerResponse addToWhitelist(@QueryParam("ip")String ip);

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
    @ApiOperation(value = "Create account", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = CreateAccountResponse.class) })
    public CreateAccountResponse createAccount(@QueryParam("name")String name);

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
    @ApiOperation(value = "Get account", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetAccountResponse.class) })
    public GetAccountResponse getAccount(@QueryParam("address")String address);

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
    @ApiOperation(value = "Get account transactions", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetAccountTransactionsResponse.class) })
    public GetAccountTransactionsResponse getAccountTransactions(@QueryParam("address")String address, @QueryParam("from")String from, @QueryParam("to")String to);

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
    @ApiOperation(value = "Get block by hash", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetBlockResponse.class) })
    public GetBlockResponse getBlockByHash(@QueryParam("hash")String hash);

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
    @ApiOperation(value = "Get block by number", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetBlockResponse.class) })
    public GetBlockResponse getBlockByNumber(@QueryParam("number")String number);

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
    @ApiOperation(value = "Get a delegate", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetDelegateResponse.class) })
    public GetDelegateResponse getDelegate(@QueryParam("address")String address);

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
    @ApiOperation(value = "Get all delegates", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetDelegatesResponse.class) })
    public GetDelegatesResponse getDelegates();

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
    @ApiOperation(value = "Get info", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetInfoResponse.class) })
    public GetInfoResponse getInfo();

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
    @ApiOperation(value = "Get latest block", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetLatestBlockResponse.class) })
    public GetLatestBlockResponse getLatestBlock();

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
    @ApiOperation(value = "Get latest block number", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetLatestBlockNumberResponse.class) })
    public GetLatestBlockNumberResponse getLatestBlockNumber();

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
    @ApiOperation(value = "Get peers", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetPeersResponse.class) })
    public GetPeersResponse getPeers();

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
    @ApiOperation(value = "Get pending transactions", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetPendingTransactionsResponse.class) })
    public GetPendingTransactionsResponse getPendingTransactions();

    /**
     * Get root
     *
     */
    @GET
    @Path("/")
    @Consumes({ "application/x-www-form-urlencoded" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get root", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetRootResponse.class) })
    public GetRootResponse getRoot();

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
    @ApiOperation(value = "Get transaction", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetTransactionResponse.class) })
    public GetTransactionResponse getTransaction(@QueryParam("hash")String hash);

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
    @ApiOperation(value = "Get transaction limits", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetTransactionLimitsResponse.class) })
    public GetTransactionLimitsResponse getTransactionLimits(@QueryParam("type")String type);

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
    @ApiOperation(value = "Get validators", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetValidatorsResponse.class) })
    public GetValidatorsResponse getValidators();

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
    @ApiOperation(value = "Get vote", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetVoteResponse.class) })
    public GetVoteResponse getVote(@QueryParam("delegate")String delegate, @QueryParam("voter")String voter);

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
    @ApiOperation(value = "Get votes", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = GetVotesResponse.class) })
    public GetVotesResponse getVotes(@QueryParam("delegate")String delegate);

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
    @ApiOperation(value = "List accounts", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ListAccountsResponse.class) })
    public ListAccountsResponse listAccounts();

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
    @ApiOperation(value = "Register delegate", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public DoTransactionResponse registerDelegate(@QueryParam("from")String from, @QueryParam("fee")String fee, @QueryParam("data")String data);

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
    @ApiOperation(value = "Send a raw transaction", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = SendTransactionResponse.class) })
    public SendTransactionResponse sendTransaction(@QueryParam("raw")String raw);

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
    @ApiOperation(value = "Sign a message", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = SignMessageResponse.class) })
    public SignMessageResponse signMessage(@QueryParam("address")String address, @QueryParam("message")String message);

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
    @ApiOperation(value = "Transfer coins", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public DoTransactionResponse transfer(@QueryParam("value")String value, @QueryParam("from")String from, @QueryParam("to")String to, @QueryParam("fee")String fee, @QueryParam("data")String data);

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
    @ApiOperation(value = "Unvote", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public DoTransactionResponse unvote(@QueryParam("from")String from, @QueryParam("to")String to, @QueryParam("value")String value, @QueryParam("fee")String fee);

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
    @ApiOperation(value = "Verify a message", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = VerifyMessageResponse.class) })
    public VerifyMessageResponse verifyMessage(@QueryParam("address")String address, @QueryParam("message")String message, @QueryParam("signature")String signature);

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
    @ApiOperation(value = "Vote", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = DoTransactionResponse.class) })
    public DoTransactionResponse vote(@QueryParam("from")String from, @QueryParam("to")String to, @QueryParam("value")String value, @QueryParam("fee")String fee);
}

