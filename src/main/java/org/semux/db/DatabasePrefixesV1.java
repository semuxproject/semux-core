/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

/**
 * @deprecated only being used for database v0 and v1
 */
public final class DatabasePrefixesV1 {

    public static final class IndexDB {
        public static final byte TYPE_LATEST_BLOCK_NUMBER = 0x00;
        public static final byte TYPE_VALIDATORS = 0x01;
        public static final byte TYPE_VALIDATOR_STATS = 0x02;
        public static final byte TYPE_BLOCK_HASH = 0x03;
        public static final byte TYPE_TRANSACTION_HASH = 0x04;
        public static final byte TYPE_ACCOUNT_TRANSACTION = 0x05;
        public static final byte TYPE_ACTIVATED_FORKS = 0x06;
        public static final byte TYPE_COINBASE_TRANSACTION_HASH = 0x07;
        public static final byte TYPE_DATABASE_VERSION = (byte) 0xff;
    }

    public static final class BlockDB {
        public static final byte TYPE_BLOCK_HEADER = 0x00;
        public static final byte TYPE_BLOCK_TRANSACTIONS = 0x01;
        public static final byte TYPE_BLOCK_RESULTS = 0x02;
        public static final byte TYPE_BLOCK_VOTES = 0x03;
    }

    public static final class AccountDB {
        public static final byte TYPE_ACCOUNT = 0x00;
        public static final byte TYPE_CODE = 0x01;
        public static final byte TYPE_STORAGE = 0x02;
    }

    // DelegateDB
    // [delegate address x 20 bytes]

    // VoteDB
    // [delegate address x 20 bytes][voter address x 20 bytes]

}
