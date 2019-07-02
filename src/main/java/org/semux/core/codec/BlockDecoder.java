/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.core.Block;
import org.semux.crypto.Key;

public interface BlockDecoder {

    Block decodeComponents(byte[] header, byte[] transactions, byte[] transactionResults, byte[] votes);

    Block decode(byte[] bytes);

    Pair<Integer, List<Key.Signature>> decodeVotes(byte[] v);

}
