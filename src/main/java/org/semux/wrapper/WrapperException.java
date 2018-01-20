/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

public class WrapperException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    WrapperException(String s) {
        super(s);
    }
}
