/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

public class DatabasePrefixesV2 {

    // former BlockDB
    public static final byte TYPE_BLOCK_HEADER = 0x00;
    public static final byte TYPE_BLOCK_TRANSACTIONS = 0x01;
    public static final byte TYPE_BLOCK_RESULTS = 0x02;
    public static final byte TYPE_BLOCK_VOTES = 0x03;

    // former IndexDB
    public static final byte TYPE_LATEST_BLOCK_NUMBER = 0x20;
    public static final byte TYPE_VALIDATORS = 0x21;
    public static final byte TYPE_VALIDATOR_STATS = 0x22;
    public static final byte TYPE_BLOCK_HASH = 0x23;
    public static final byte TYPE_TRANSACTION_HASH = 0x24;
    public static final byte TYPE_ACCOUNT_TRANSACTION = 0x25;
    public static final byte TYPE_ACTIVATED_FORKS = 0x26;
    public static final byte TYPE_COINBASE_TRANSACTION_HASH = 0x27;

    // former AccountDB
    public static final byte TYPE_ACCOUNT = 0x40;
    public static final byte TYPE_CODE = 0x41;
    public static final byte TYPE_STORAGE = 0x42;

    // former delegateDB
    // [0x60][delegate address x 20 bytes]
    public static final byte TYPE_DELEGATE = 0x60;
    // [0x61][delegate address x 20 bytes] => index to the delegate
    public static final byte TYPE_DELEGATE_INDEX_TO_ADDRESS = 0x61;
    public static final byte TYPE_DELEGATE_ADDRESS_TO_INDEX = 0x62;
    // [0x62] => number of delegates
    public static final byte TYPE_DELEGATE_COUNT = 0x63;

    // former voteDB
    // [0x80][delegate address x 20 bytes][voter address x 20 bytes]
    public static final byte TYPE_DELEGATE_VOTE = (byte) 0x80;

    public static final byte TYPE_DATABASE_VERSION = (byte) 0xff;

}
