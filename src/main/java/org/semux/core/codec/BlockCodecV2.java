/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import org.semux.core.state.DelegateStateV2;

public class BlockCodecV2 implements BlockCodec {

    private final BlockEncoderV2 encoder;

    private final BlockDecoderV2 decoder;

    public BlockCodecV2(DelegateStateV2 delegateState) {
        encoder = new BlockEncoderV2(delegateState);
        decoder = new BlockDecoderV2(delegateState);
    }

    @Override
    public BlockEncoder encoder() {
        return encoder;
    }

    @Override
    public BlockDecoder decoder() {
        return decoder;
    }
}
