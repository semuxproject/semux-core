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
     * Transaction result code. There are currently three categories of code:
     * <ul>
     * <li>REJECTED: the transaction is invalid and should not be included in the
     * chain.</li>
     * <li>SUCCESS: the transaction is valid and the evaluation was successful.</li>
     * <li>FAILURE: the transaction is valid but some failure occurred during the
     * evaluation. For this type of transaction, the fee should be charged but state
     * changes should be reverted.</li>
     * </ul>
     */
    public enum Code {

        /**
         * The transaction was executed successfully.
         */
        SUCCESS,

        /**
         * The transaction was executed with failure (reserved for virtual machine).
         */
        FAILURE,

        /**
         * The transaction hash is duplicated.
         */
        DUPLICATE_TRANSACTION,

        /**
         * The transaction format is invalid. See {@link Transaction#validate(Network)}
         */
        INVALID_FORMAT,

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
        INVALID_DATA,

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
         * Invalid burning address.
         */
        INVALID_BURNING_ADDRESS,

        /**
         * Invalid delegate burn amount.
         */
        INVALID_BURNING_AMOUNT,

        /**
         * The DELEGATE operation is invalid.
         */
        INVALID_DELEGATING,

        /**
         * The VOTE operation is invalid.
         */
        INVALID_VOTING,

        /**
         * The UNVOTE operation is invalid.
         */
        INVALID_UNVOTING;

        public boolean isSuccess() {
            return this == SUCCESS;
        }

        public boolean isFailure() {
            return this == FAILURE;
        }

        public boolean isAccepted() {
            return isSuccess() || isFailure();
        }

        public boolean isRejected() {
            return !isSuccess() && !isFailure();
        }
    }

    /**
     * Transaction execution result code.
     */
    protected Code code;

    /**
     * Transaction returns.
     */
    protected byte[] returnData;

    /**
     * Transaction logs.
     */
    protected List<byte[]> logs;

    /**
     * Create a transaction result.
     *
     * @param code
     * @param output
     * @param logs
     */
    public TransactionResult(Code code, byte[] output, List<byte[]> logs) {
        super();
        this.code = code;
        this.returnData = output;
        this.logs = logs;
    }

    public TransactionResult(Code code) {
        this(code, Bytes.EMPTY_BYTES, new ArrayList<>());
    }

    public TransactionResult() {
        this(Code.SUCCESS, Bytes.EMPTY_BYTES, new ArrayList<>());
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public byte[] getReturnData() {
        return returnData;
    }

    public void setReturnData(byte[] returnData) {
        this.returnData = returnData;
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

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(code == Code.SUCCESS);
        enc.writeBytes(returnData);
        enc.writeInt(logs.size());
        for (byte[] log : logs) {
            enc.writeBytes(log);
        }

        return enc.toBytes();
    }

    public static TransactionResult fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        boolean success = dec.readBoolean();
        byte[] returns = dec.readBytes();
        List<byte[]> logs = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            logs.add(dec.readBytes());
        }

        return new TransactionResult(success ? Code.SUCCESS : Code.FAILURE, returns, logs);
    }

    @Override
    public String toString() {
        return "TransactionResult [code=" + code + ", returnData=" + Arrays.toString(returnData) + ", # logs="
                + logs.size() + "]";
    }
}
