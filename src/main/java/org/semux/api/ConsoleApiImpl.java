/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import org.semux.Kernel;
import org.semux.api.response.GetBlockResponse;
import org.semux.api.response.Types;
import org.semux.core.Block;

/**
 */
public class ConsoleApiImpl implements ConsoleApi {

    private Kernel kernel;

    public ConsoleApiImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    public ApiHandlerResponse failure(String message) {
        return new ApiHandlerResponse(false, message);
    }

    @Override
    public ApiHandlerResponse getBlockByNumber(String blockNum) {
        if (blockNum == null) {
            return failure("Parameter `number` is required");
        }
        Long number;
        try {
            number = Long.parseLong(blockNum);
        } catch (NumberFormatException ex) {
            return failure("Parameter `number` is not a valid number");
        }

        Block block = kernel.getBlockchain().getBlock(number);

        if (block == null) {
            return failure("The requested block was not found");
        }

        return new GetBlockResponse(true, new Types.BlockType(block));
    }
}
