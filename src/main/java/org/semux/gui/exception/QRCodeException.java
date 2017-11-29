/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.exception;

public class QRCodeException extends Exception {

    public QRCodeException() {
    }

    public QRCodeException(String s) {
        super(s);
    }

    public QRCodeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public QRCodeException(Throwable throwable) {
        super(throwable);
    }

    public QRCodeException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
