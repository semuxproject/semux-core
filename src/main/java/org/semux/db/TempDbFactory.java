/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

/**
 * A temporary database.
 */
public class TempDbFactory implements DbFactory {

    static final String DIR_PREFIX = "semux-temp-db-";

    private EnumMap<DbName, Db> databases = new EnumMap<>(DbName.class);

    private Path tempDir;

    public TempDbFactory() throws IOException {
        open();
    }

    @Override
    public Db getDB(DbName name) {
        return databases.get(name);
    }

    @Override
    public void open() throws IOException {
        tempDir = Files.createTempDirectory(DIR_PREFIX);
        for (DbName name : DbName.values()) {
            File dbDir = new File(tempDir.toFile(), name.toString().toLowerCase());
            databases.put(name, new LevelDb(dbDir));
        }
    }

    @Override
    public void close() {
        for (Db db : databases.values()) {
            db.close();
        }
    }

    @Override
    public Path getDataDir() {
        return tempDir;
    }

    public void move(Path path) throws IOException {
        Files.move(tempDir, path, REPLACE_EXISTING);
    }
}
