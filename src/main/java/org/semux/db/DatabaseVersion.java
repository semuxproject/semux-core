/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import org.semux.db.exception.DatabaseException;
import org.semux.util.Bytes;

public enum DatabaseVersion {

    V0(null),

    V1(Bytes.of(1)),

    V2(Bytes.of(2));

    private final byte[] versionBytes;

    DatabaseVersion(byte[] versionBytes) {
        this.versionBytes = versionBytes;
    }

    public byte[] toBytes() {
        return versionBytes;
    }

    public static DatabaseVersion fromBytes(byte[] bytes) {
        if (bytes == null) {
            return V0;
        }

        final int v = Bytes.toInt(bytes);
        switch (v) {
        case 1:
            return V1;
        case 2:
            return V2;
        }
        throw new DatabaseException("Unsupported version " + v);
    }
}
