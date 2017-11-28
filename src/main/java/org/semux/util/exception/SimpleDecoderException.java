/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util.exception;

public class SimpleDecoderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SimpleDecoderException() {
    }

    public SimpleDecoderException(String s) {
        super(s);
    }

    public SimpleDecoderException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SimpleDecoderException(Throwable throwable) {
        super(throwable);
    }

    public SimpleDecoderException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
