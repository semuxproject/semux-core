/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.rules;

import java.io.File;
import java.util.EnumMap;

import org.junit.rules.TemporaryFolder;
import org.semux.config.Constants;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;

public class TemporaryDBRule extends TemporaryFolder implements DBFactory {

    private EnumMap<DBName, KVDB> databases = new EnumMap<>(DBName.class);

    @Override
    public void before() throws Throwable {
        create();
        for (DBName name : DBName.values()) {
            File file = new File(getRoot(), Constants.DATABASE_DIR + File.separator + name.toString().toLowerCase());
            databases.put(name, new LevelDB(file));
        }
    }

    @Override
    public void after() {
        close();
        delete();
    }

    @Override
    public void close() {
        for (KVDB db : databases.values()) {
            db.close();
        }
    }

    @Override
    public KVDB getDB(DBName name) {
        return databases.get(name);
    }
}