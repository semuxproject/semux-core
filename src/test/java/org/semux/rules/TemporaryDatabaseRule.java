/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.rules;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;

import org.junit.rules.TemporaryFolder;
import org.semux.config.Constants;
import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;
import org.semux.db.LeveldbDatabase;

public class TemporaryDatabaseRule extends TemporaryFolder implements DatabaseFactory {

    private EnumMap<DatabaseName, Database> databases = new EnumMap<>(DatabaseName.class);

    @Override
    public void before() throws Throwable {
        create();
    }

    @Override
    public void after() {
        close();
        delete();
    }

    @Override
    public Database getDB(DatabaseName name) {
        return databases.computeIfAbsent(name, k -> {
            File file = new File(getRoot(), Constants.DATABASE_DIR + File.separator + k.toString().toLowerCase());
            return new LeveldbDatabase(file);
        });
    }

    @Override
    public void close() {
        for (Database db : databases.values()) {
            db.close();
        }
    }

    @Override
    public Path getDataDir() {
        return super.getRoot().toPath();
    }
}