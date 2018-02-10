/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import java.io.IOException;

public class MessageException extends IOException {
    private static final long serialVersionUID = 1L;

    public MessageException() {
    }

    public MessageException(String s) {
        super(s);
    }

    public MessageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public MessageException(Throwable throwable) {
        super(throwable);
    }
}
