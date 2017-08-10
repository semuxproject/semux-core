/*
 * Copyright (c) 2017 The Semux Developers
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

    /**
     * Stop the client.
     */
    STOP,

    // =======================
    // network
    // =======================
    /**
     * Get active peer list.
     */
    GET_ACTIVE_NODES,

    /**
     * Add a new node to connect.
     */
    ADD_NODE,

    /**
     * Block an IP address.
     */
    BLOCK_IP,

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
     * Get transaction by its hash.
     */
    GET_TRANSACTION,

    /**
     * Send a signed raw transaction.
     */
    SEND_TRANSACTION,

    // =======================
    // wallet
    // =======================
    /**
     * Unlock the wallet.
     */
    UNLOCK_WALLET,

    /**
     * Lock the wallet.
     */
    LOCK_WALLET,

    /**
     * List accounts in the wallet.
     */
    GET_ACCOUNTS,

    /**
     * Create a new account.
     */
    NEW_ACCOUNT,

    // =======================
    // others
    // =======================
    /**
     * Get the balance of an account.
     */
    GET_BALANCE,

    /**
     * Get the nonce of an account.
     */
    GET_NONCE,

    /**
     * Get all delegates.
     */
    GET_DELEGATES,

    /**
     * Get delegate by address or name.
     */
    GET_DELEGATE,

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
