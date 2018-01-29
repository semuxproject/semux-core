/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db.exception;

public class LevelDbException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LevelDbException() {
    }

    public LevelDbException(String s) {
        super(s);
    }

    public LevelDbException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LevelDbException(Throwable throwable) {
        super(throwable);
    }

    public LevelDbException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
