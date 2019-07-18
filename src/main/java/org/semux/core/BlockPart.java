/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.List;

public enum BlockPart {
    HEADER(1 << 0), TRANSACTIONS(1 << 1), RESULTS(1 << 2), VOTES(1 << 3);

    private int code;

    BlockPart(int code) {
        this.code = code;
    }

    public static int encode(BlockPart... parts) {
        int result = 0;
        for (BlockPart part : parts) {
            result |= part.code;
        }
        return result;
    }

    public static List<BlockPart> decode(int parts) {
        List<BlockPart> result = new ArrayList<>();
        // NOTE: values() returns an array containing all of the values of the enum type
        // in the order they are declared.
        for (BlockPart bp : BlockPart.values()) {
            if ((parts & bp.code) != 0) {
                result.add(bp);
            }
        }

        return result;
    }
}
