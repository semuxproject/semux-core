/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.semux.config.Config;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.TimeUtil;

public class TestUtils {

    public static Block createBlock(long number, List<Transaction> txs, List<TransactionResult> res) {
        return createBlock(Bytes.EMPTY_HASH, new Key(), number, txs, res);
    }

    public static Block createBlock(long timestamp, byte[] prevHash, Key coinbase, long number, List<Transaction> txs,
            List<TransactionResult> res) {
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase.toAddress(), prevHash, timestamp, transactionsRoot,
                resultsRoot,
                stateRoot, data);
        return new Block(header, txs, res);
    }

    public static Block createBlock(byte[] prevHash, Key coinbase, long number, List<Transaction> txs,
            List<TransactionResult> res) {
        return createBlock(TimeUtil.currentTimeMillis(), prevHash, coinbase, number, txs, res);
    }

    public static Block createEmptyBlock(long number) {
        return createBlock(number, Collections.emptyList(), Collections.emptyList());
    }

    public static Transaction createTransaction(Config config) {
        return createTransaction(config, new Key(), new Key(), Amount.ZERO);
    }

    public static Transaction createTransaction(Config config, Key from, Key to, Amount value) {
        return createTransaction(config, from, to, value, 0);
    }

    public static Transaction createTransaction(Config config, Key from, Key to, Amount value, long nonce) {
        Network network = config.network();
        TransactionType type = TransactionType.TRANSFER;
        Amount fee = config.minTransactionFee();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = {};

        return new Transaction(network, type, to.toAddress(), value, fee, nonce, timestamp, data).sign(from);
    }

    // From:
    // https://github.com/noushadali/powermock/blob/master/reflect/src/main/java/org/powermock/reflect/internal/WhiteboxImpl.java

    /**
     * Get the value of a field using reflection. Use this method when you need to
     * specify in which class the field is declared. This might be useful when you
     * have mocked the instance you are trying to access. Use this method to avoid
     * casting.
     *
     * @param <T>
     *            the expected type of the field
     * @param object
     *            the object to modify
     * @param fieldName
     *            the name of the field
     * @param where
     *            which class the field is defined
     * @return the internal state
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(Object object, String fieldName, Class<?> where) {
        if (object == null || fieldName == null || fieldName.equals("") || fieldName.startsWith(" ")) {
            throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
        }

        Field field = null;
        try {
            field = where.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field '" + fieldName + "' was not found in class " + where.getName() + ".");
        } catch (Exception e) {
            throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
        }
    }

    /**
     * Set the value of a field using reflection. Use this method when you need to
     * specify in which class the field is declared. This is useful if you have two
     * fields in a class hierarchy that has the same name but you like to modify the
     * latter.
     *
     * @param object
     *            the object to modify
     * @param fieldName
     *            the name of the field
     * @param value
     *            the new value of the field
     * @param where
     *            which class the field is defined
     */
    public static void setInternalState(Object object, String fieldName, Object value, Class<?> where) {
        if (object == null || fieldName == null || fieldName.equals("") || fieldName.startsWith(" ")) {
            throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
        }

        final Field field = getField(fieldName, where);
        try {
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Internal Error: Failed to set field in method setInternalState.", e);
        }
    }

    /**
     * Gets the field.
     *
     * @param fieldName
     *            the field name
     * @param where
     *            the where
     * @return the field
     */
    private static Field getField(String fieldName, Class<?> where) {
        if (where == null) {
            throw new IllegalArgumentException("where cannot be null");
        }

        Field field = null;
        try {
            field = where.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field '" + fieldName + "' was not found in class " + where.getName() + ".");
        }
        return field;
    }
}
