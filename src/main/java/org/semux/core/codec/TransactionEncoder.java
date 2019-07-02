/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import org.semux.core.Transaction;

public interface TransactionEncoder {

    byte[] encode(Transaction transaction);

    byte[] encodeUnsigned(Transaction transaction);

}
