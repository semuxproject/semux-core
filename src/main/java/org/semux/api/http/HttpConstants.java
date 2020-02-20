/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.http;

public class HttpConstants {
    public static final int MAX_BODY_SIZE = 4 * 1024 * 1024;

    public static final int MAX_INITIAL_LINE_LENGTH = 2 * 1024 * 1024;
    public static final int MAX_HEADER_SIZE = 2 * 1024 * 1024;
    public static final int MAX_CHUNK_SIZE = 2 * 1024 * 1024;
}
