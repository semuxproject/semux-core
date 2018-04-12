/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.semux.Network;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class TransactionResult {

    /**
     * Transaction error code.
     */
    public enum Error {
        /**
         * The transaction format is invalid. See {@link Transaction#validate(Network)}
         */
        INVALID_FORMAT,

        /**
         * The transaction hash is duplicated.
         */
        DUPLICATED_HASH,

        /**
         * The transaction timestamp is incorrect. See
         * {@link PendingManager#processTransaction(Transaction, boolean)}.
         */
        INVALID_TIMESTAMP,

        /**
         * The transaction type is invalid.
         */
        INVALID_TYPE,

        /**
         * The transaction nonce does not match the account nonce.
         */
        INVALID_NONCE,

        /**
         * The transaction fee doesn't meet the minimum.
         */
        INVALID_FEE,

        /**
         * The transaction data is invalid, typically too large.
         */
        INVALID_DATA_LENGTH,

        /**
         * Insufficient available balance.
         */
        INSUFFICIENT_AVAILABLE,

        /**
         * Insufficient locked balance.
         */
        INSUFFICIENT_LOCKED,

        /**
         * Invalid delegate name.
         */
        INVALID_DELEGATE_NAME,

        /**
         * Invalid delegate burn amount.
         */
        INVALID_DELEGATE_BURN_AMOUNT,

        /**
         * Invalid transaction recipient.
         */
        INVALID_RECIPIENT,

        /**
         * Transaction failed.
         */
        FAILED
    }

    /**
     * Indicates whether this transaction is success or not.
     */
    protected boolean success;

    /**
     * Transaction returns.
     */
    protected byte[] returns;

    /**
     * Transaction logs.
     */
    protected List<byte[]> logs;

    /**
     * Error message for API/GUI, not sent over the network.
     */
    protected Error error;

    /**
     * Create a transaction result.
     * 
     * @param success
     * @param output
     * @param logs
     */
    public TransactionResult(boolean success, byte[] output, List<byte[]> logs) {
        super();
        this.success = success;
        this.returns = output;
        this.logs = logs;
    }

    /**
     * Create a transaction result.
     * 
     * @param success
     */
    public TransactionResult(boolean success) {
        this(success, Bytes.EMPTY_BYTES, new ArrayList<>());
    }

    /**
     * Validate transaction result.
     * 
     * @return
     */
    public boolean validate() {
        return success
                && returns != null
                && logs != null; // RESERVED FOR VM
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public byte[] getReturns() {
        return returns;
    }

    public void setReturns(byte[] returns) {
        this.returns = returns;
    }

    public List<byte[]> getLogs() {
        return logs;
    }

    public void setLogs(List<byte[]> logs) {
        this.logs = logs;
    }

    public void addLog(byte[] log) {
        this.logs.add(log);
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(success);
        enc.writeBytes(returns);
        enc.writeInt(logs.size());
        for (byte[] log : logs) {
            enc.writeBytes(log);
        }

        return enc.toBytes();
    }

    public static TransactionResult fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        boolean valid = dec.readBoolean();
        byte[] returns = dec.readBytes();
        List<byte[]> logs = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            logs.add(dec.readBytes());
        }

        return new TransactionResult(valid, returns, logs);
    }

    @Override
    public String toString() {
        return "TransactionResult [success=" + success + ", output=" + Arrays.toString(returns) + ", # logs="
                + logs.size() + "]";
    }
}
