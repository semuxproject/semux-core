/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.exception;

import org.semux.crypto.Hex;

public class InvalidOpCodeException extends VMException {

    private static final long serialVersionUID = 1L;

    public InvalidOpCodeException(byte code) {
        super("Invalid opcode 0x" + Hex.encode(new byte[] { code }));
    }
}
