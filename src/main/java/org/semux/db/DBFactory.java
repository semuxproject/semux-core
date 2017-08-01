/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

public interface DBFactory {
    /**
     * Get a KVDB instance for the specified database.
     * 
     * @param name
     * @return
     */
    public KVDB getDB(DBName name);
}
