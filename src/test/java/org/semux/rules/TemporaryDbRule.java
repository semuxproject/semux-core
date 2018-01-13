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
import org.semux.db.Db;
import org.semux.db.DbFactory;
import org.semux.db.DbName;
import org.semux.db.LevelDb;

public class TemporaryDbRule extends TemporaryFolder implements DbFactory {

    private EnumMap<DbName, Db> databases = new EnumMap<>(DbName.class);

    @Override
    public void before() throws Throwable {
        create();
        for (DbName name : DbName.values()) {
            File file = new File(getRoot(), Constants.DATABASE_DIR + File.separator + name.toString().toLowerCase());
            databases.put(name, new LevelDb(file));
        }
    }

    @Override
    public void after() {
        close();
        delete();
    }

    @Override
    public void close() {
        for (Db db : databases.values()) {
            db.close();
        }
    }

    @Override
    public Db getDB(DbName name) {
        return databases.get(name);
    }
}