/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.ethereum.vm.program.InternalTransaction;
import org.semux.Network;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

// TODO: rename to transaction receipts
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
         * The specified gas amount is larger than any gas limit
         */
        INVALID_GAS,

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
        INVALID_DELEGATE_BURN_ADDRESS,

        /**
         * Invalid delegate burn amount.
         */
        INVALID_DELEGATE_BURN_AMOUNT,

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
            return !isAccepted();
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
    protected List<LogInfo> logs;

    /**
     * Gas used
     */
    protected long gasUsed;

    /**
     * TODO: add block number and internal transactions
     */
    protected long blockNumber;
    protected List<InternalTransaction> internalTransactions;

    /**
     * Create a transaction result.
     *
     * 
     * @param code
     * @param output
     * @param logs
     */
    public TransactionResult(Code code, byte[] output, List<LogInfo> logs, long gasUsed) {
        super();
        this.code = code;
        this.returnData = output;
        this.logs = logs;
        this.gasUsed = gasUsed;
    }

    public TransactionResult(Code code) {
        this(code, Bytes.EMPTY_BYTES, new ArrayList<>(), 0);
    }

    public TransactionResult() {
        this(Code.SUCCESS, Bytes.EMPTY_BYTES, new ArrayList<>(), 0);
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

    public List<LogInfo> getLogs() {
        return logs;
    }

    public void setLogs(List<LogInfo> logs) {
        this.logs = logs;
    }

    public void addLog(LogInfo log) {
        this.logs.add(log);
    }

    public Long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(code == Code.SUCCESS);
        enc.writeBytes(returnData);
        enc.writeInt(logs.size());
        for (LogInfo log : logs) {
            enc.writeBytes(serializeLog(log));
        }

        // TODO: provide another version of toBytes() for resultsRoot validation.
        // TODO: use the computed results, rather than network bytes.

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
        boolean success = dec.readBoolean();
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

        // todo - this seems problematic, !success could be any number of things
        return new TransactionResult(success ? Code.SUCCESS : Code.FAILURE, returns, logs, gasUsed);
    }

    @Override
    public String toString() {
        return "TransactionResult [code=" + code + ", returnData=" + Hex.toHexString(returnData) + ", # logs="
                + logs.size() + "]";
    }
}
