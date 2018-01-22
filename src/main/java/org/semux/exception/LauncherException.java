/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.exception;

public class LauncherException extends RuntimeException {

    public LauncherException() {
    }

    public LauncherException(String message) {
        super(message);
    }

    public LauncherException(String message, Throwable cause) {
        super(message, cause);
    }

    public LauncherException(Throwable cause) {
        super(cause);
    }

    public LauncherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
