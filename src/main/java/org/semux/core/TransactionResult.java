/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.semux.Network;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
         * The specified gas amount is larger than any gas limit
         */
        INVALID_GAS,

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
    protected List<LogInfo> logs;

    /**
     * Gas used
     */
    protected long gasUsed;

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
    public TransactionResult(boolean success, byte[] output, List<LogInfo> logs, long gasUsed) {
        super();
        this.success = success;
        this.returns = output;
        this.logs = logs;
        this.gasUsed = gasUsed;
    }

    /**
     * Create a transaction result.
     *
     * @param success
     */
    public TransactionResult(boolean success) {
        this(success, Bytes.EMPTY_BYTES, new ArrayList<>(), 0);
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

    public List<LogInfo> getLogs() {
        return logs;
    }

    public void setLogs(List<LogInfo> logs) {
        this.logs = logs;
    }

    public void addLog(LogInfo log) {
        this.logs.add(log);
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public Long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(success);
        enc.writeBytes(returns);
        enc.writeInt(logs.size());
        for (LogInfo log : logs) {
            enc.writeBytes(serializeLog(log));
        }

        // only write gasUsed if it exists to maintain backwards compatibility
        // this maintains backwards compatibility until VM calls are enabled with
        // fork check
        if (gasUsed > 0) {
            enc.writeLong(gasUsed);
        }

        return enc.toBytes();
    }

    private byte[] serializeLog(LogInfo log) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(log.getAddress());
        enc.writeBytes(log.getData());
        enc.writeInt(log.getTopics().size());
        for (DataWord dataWord : log.getTopics()) {
            enc.writeBytes(dataWord.getData());
        }

        return enc.toBytes();
    }

    private static LogInfo unserializeLog(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] address = dec.readBytes();
        byte[] data = dec.readBytes();
        int numTopics = dec.readInt();
        List<DataWord> topics = new ArrayList<>();

        for (int i = 0; i < numTopics; i++) {
            topics.add(DataWord.of(dec.readBytes()));
        }
        return new LogInfo(address, topics, data);
    }

    public static TransactionResult fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        boolean valid = dec.readBoolean();
        byte[] returns = dec.readBytes();
        List<LogInfo> logs = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            logs.add(unserializeLog(dec.readBytes()));
        }
        long gasUsed = 0;
        try {
            gasUsed = dec.readLong();
        } catch (Exception e) {
            // old blocks won't have this field
        }

        return new TransactionResult(valid, returns, logs, gasUsed);
    }

    @Override
    public String toString() {
        return "TransactionResult [success=" + success + ", output=" + Arrays.toString(returns) + ", # logs="
                + logs.size() + ", gasUsed=" + gasUsed + "]";
    }
}
