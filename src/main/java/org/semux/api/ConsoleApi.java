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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.semux.api.response.GetBlockResponse;

/**
 * Additional console-only API
 *
 */
public interface ConsoleApi {

    @GET
    @Path("get_block_by_number")
    @ApiOperation(value = "Get block by block number", notes = "Returns a block.", response = GetBlockResponse.class)
    @Produces(JSON_MIME)
    ApiHandlerResponse getBlockByNumber(
            @ApiParam(value = "Block number", required = true) @QueryParam("number") String blockNum);

}
