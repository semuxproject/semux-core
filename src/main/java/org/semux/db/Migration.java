/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.db.exception.MigrationException;

public interface Migration {

    void checkBlockchainVersion(Blockchain blockchain) throws MigrationException;

    void migrate(Config config, DatabaseFactory dbFactory);
}
