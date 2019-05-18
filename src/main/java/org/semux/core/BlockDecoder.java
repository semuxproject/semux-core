/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public interface BlockDecoder {

    Block fromComponents(byte[] header, byte[] transactions, byte[] transactionResults, byte[] votes);

    Block fromBytes(byte[] bytes);

}
