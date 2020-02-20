/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus.exception;

public class SemuxBftException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SemuxBftException() {
    }

    public SemuxBftException(String s) {
        super(s);
    }

    public SemuxBftException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SemuxBftException(Throwable throwable) {
        super(throwable);
    }

    public SemuxBftException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
