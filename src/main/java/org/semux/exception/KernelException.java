/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.exception;

public class KernelException extends RuntimeException {

    public KernelException() {
    }

    public KernelException(String s) {
        super(s);
    }

    public KernelException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public KernelException(Throwable throwable) {
        super(throwable);
    }

    public KernelException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
