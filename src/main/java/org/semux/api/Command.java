/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.HashMap;
import java.util.Map;

public enum Command {

    // =======================
    // general
    // =======================
    /**
     * Get the general info.
     */
    GET_INFO,

    // =======================
    // network
    // =======================
    /**
     * Get active peer list.
     */
    GET_PEERS,

    /**
     * Add a new node to connect.
     */
    ADD_NODE,

    /**
     * Add an IP address to the blacklist.
     */
    ADD_TO_BLACKLIST,

    /**
     * Add an IP address to the whitelist.
     */
    ADD_TO_WHITELIST,

    // =======================
    // block
    // =======================
    /**
     * Get the number of the latest block.
     */
    GET_LATEST_BLOCK_NUMBER,

    /**
     * Get the latest block.
     */
    GET_LATEST_BLOCK,

    /**
     * Get block by its number or hash.
     */
    GET_BLOCK,

    // =======================
    // transaction
    // =======================
    /**
     * Get pending transactions.
     */
    GET_PENDING_TRANSACTIONS,

    /**
     * Get transactions by account.
     */
    GET_ACCOUNT_TRANSACTIONS,

    /**
     * Get transaction by its hash.
     */
    GET_TRANSACTION,

    /**
     * Send a signed raw transaction.
     */
    SEND_TRANSACTION,

    // =======================
    // state query
    // =======================
    /**
     * Get the info of an account.
     */
    GET_ACCOUNT,

    /**
     * Get delegate by address or name.
     */
    GET_DELEGATE,

    /**
     * Get all validators.
     */
    GET_VALIDATORS,

    /**
     * Get all delegates.
     */
    GET_DELEGATES,

    /**
     * Get the number of votes one has bonded to a delegate.
     */
    GET_VOTE,

    /**
     * Get all votes of a delegate.
     */
    GET_VOTES,

    // =======================
    // wallet (auth required)
    // =======================
    /**
     * List accounts in the wallet.
     */
    LIST_ACCOUNTS,

    /**
     * Create a new account.
     */
    CREATE_ACCOUNT,

    /**
     * Get transaction limits.
     */
    GET_TRANSACTION_LIMITS,

    /**
     * Balance transfer.
     */
    TRANSFER,

    /**
     * Register as a delegate.
     */
    DELEGATE,

    /**
     * Vote for a delegate.
     */
    VOTE,

    /**
     * Unvote for a delegate.
     */
    UNVOTE;

    private static Map<String, Command> map = new HashMap<>();
    static {
        for (Command cmd : Command.values()) {
            map.put(cmd.name().toLowerCase(), cmd);
        }
    }

    /**
     * Parse the command from an input string.
     * 
     * @param cmd
     *            the input string
     * @return the command if successful, otherwise null
     */
    public static Command of(String cmd) {
        return map.get(cmd);
    }
}
