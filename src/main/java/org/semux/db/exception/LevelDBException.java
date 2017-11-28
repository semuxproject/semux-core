/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db.exception;

public class LevelDBException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LevelDBException() {
    }

    public LevelDBException(String s) {
        super(s);
    }

    public LevelDBException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LevelDBException(Throwable throwable) {
        super(throwable);
    }

    public LevelDBException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
