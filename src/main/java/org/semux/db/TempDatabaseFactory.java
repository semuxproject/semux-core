/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

/**
 * A temporary database.
 */
public class TempDatabaseFactory implements DatabaseFactory {

    static final String DIR_PREFIX = "semux-temp-db-";

    private EnumMap<DatabaseName, Database> databases = new EnumMap<>(DatabaseName.class);

    private Path tempDir;

    public TempDatabaseFactory() throws IOException {
        open();
    }

    @Override
    public Database getDB(DatabaseName name) {
        return databases.get(name);
    }

    @Override
    public void open() throws IOException {
        tempDir = Files.createTempDirectory(DIR_PREFIX);
        for (DatabaseName name : DatabaseName.values()) {
            File dbDir = new File(tempDir.toFile(), name.toString().toLowerCase());
            databases.put(name, new LeveldbDatabase(dbDir));
        }
    }

    @Override
    public void close() {
        for (Database db : databases.values()) {
            db.close();
        }
    }

    @Override
    public Path getDataDir() {
        return tempDir;
    }

    public void move(Path path) throws IOException {
        Files.move(tempDir, path, REPLACE_EXISTING, ATOMIC_MOVE);
    }
}
