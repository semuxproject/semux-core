/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.rules;

import org.junit.rules.TemporaryFolder;
import org.semux.config.Constants;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;

import java.io.File;
import java.util.EnumMap;

public class TemporaryDBRule extends TemporaryFolder implements DBFactory {

    private EnumMap<DBName, KVDB> databases = new EnumMap<>(DBName.class);

    @Override
    protected void before() throws Throwable {
        create();
        for (DBName name : DBName.values()) {
            File file = new File(getRoot(),
                    Constants.DATABASE_DIR + File.separator + name.toString().toLowerCase());
            databases.put(name, new LevelDB(file));
        }
    }

    @Override
    protected void after() {
        for (KVDB db : databases.values()) {
            db.close();
        }
        delete();
    }

    @Override
    public KVDB getDB(DBName name) {
        return databases.get(name);
    }
}
