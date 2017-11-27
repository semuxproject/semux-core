/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util.exception;

public class SimpleEncoderException extends RuntimeException {

    public SimpleEncoderException() {
    }

    public SimpleEncoderException(String s) {
        super(s);
    }

    public SimpleEncoderException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SimpleEncoderException(Throwable throwable) {
        super(throwable);
    }

    public SimpleEncoderException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
