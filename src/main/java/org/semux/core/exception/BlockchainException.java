/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.exception;

public class BlockchainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BlockchainException() {
    }

    public BlockchainException(String s) {
        super(s);
    }

    public BlockchainException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BlockchainException(Throwable throwable) {
        super(throwable);
    }

    public BlockchainException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
