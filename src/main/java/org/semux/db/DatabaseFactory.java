/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.io.IOException;
import java.nio.file.Path;

public interface DatabaseFactory {
    /**
     * Returns a KVDB instance for the specified database.
     * 
     * @param name
     * @return
     */
    Database getDB(DatabaseName name);

    /**
     * Open resources.
     */
    void open() throws IOException;

    /**
     * Close all opened resources.
     */
    void close();

    /**
     * Returns the data directory of created databases.
     *
     * @return
     */
    Path getDataDir();
}
