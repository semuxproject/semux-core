/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus.exception;

public class SemuxBFTException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SemuxBFTException() {
    }

    public SemuxBFTException(String s) {
        super(s);
    }

    public SemuxBFTException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SemuxBFTException(Throwable throwable) {
        super(throwable);
    }

    public SemuxBFTException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
