/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

public class BatchOperation {

    public enum Type {
        PUT, DELETE
    }

    public final Type type;

    public final byte[] key;

    public final byte[] value;

    public BatchOperation(Type type, byte[] key, byte[] value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public static BatchOperation put(byte[] key, byte[] value) {
        return new BatchOperation(Type.PUT, key, value);
    }

    public static BatchOperation delete(byte[] key) {
        return new BatchOperation(Type.DELETE, key, null);
    }
}
