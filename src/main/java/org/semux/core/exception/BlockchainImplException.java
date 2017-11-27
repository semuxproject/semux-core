/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.exception;

public class BlockchainImplException extends RuntimeException {

    public BlockchainImplException() {
    }

    public BlockchainImplException(String s) {
        super(s);
    }

    public BlockchainImplException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BlockchainImplException(Throwable throwable) {
        super(throwable);
    }

    public BlockchainImplException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
