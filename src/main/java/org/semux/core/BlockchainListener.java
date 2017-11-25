/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public interface BlockchainListener {

    /**
     * Callback when a new block was added.
     * 
     * @param block
     */
    void onBlockAdded(Block block);
}
