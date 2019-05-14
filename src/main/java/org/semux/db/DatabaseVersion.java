/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import org.semux.db.exception.DatabaseException;
import org.semux.util.Bytes;
import org.semux.util.exception.UnreachableException;

public enum DatabaseVersion {

    V0,

    V1,

    V2;

    public int toInt() {
        switch (this) {
        case V0:
            return 0;
        case V1:
            return 1;
        case V2:
            return 2;
        }
        throw new UnreachableException();
    }

    public byte[] toBytes() {
        return Bytes.of(toInt());
    }

    public static DatabaseVersion fromInt(int v) {
        switch (v) {
        case 0:
            return V0;
        case 1:
            return V1;
        case 2:
            return V2;
        }
        throw new DatabaseException("Unsupported version " + v);
    }

    public static DatabaseVersion fromBytes(byte[] bytes) {
        return fromInt(Bytes.toInt(bytes));
    }
}
