/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.exception;

public class VMException extends Exception {

    private static final long serialVersionUID = 1L;

    public VMException() {
        super("Semux VM exception");
    }

    public VMException(String msg) {
        super(msg);
    }

    public VMException(Throwable cause) {
        super(cause);
    }

    public VMException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
