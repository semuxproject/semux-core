/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public class BlockCodecV1 implements BlockCodec {

    private final static BlockEncoder encoder = new BlockEncoderV1();

    private final static BlockDecoder decoder = new BlockDecoderV1();

    @Override
    public BlockEncoder encoder() {
        return encoder;
    }

    @Override
    public BlockDecoder decoder() {
        return decoder;
    }
}
