/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.program.InternalTransaction;
import org.semux.Network;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.vm.client.SemuxInternalTransaction;

public class TransactionResult {

    /**
     * Transaction execution result code.
     */
    public enum Code {

        /**
         * Success. The values has to be 0x01 for compatibility.
         */
        SUCCESS(0x01),

        /**
         * VM failure, e.g. REVERT, STACK_OVERFLOW, OUT_OF_GAS, etc.
         */
        FAILURE(0x02),

        /**
         * The transaction hash is invalid (should NOT be included on chain).
         */
        INVALID(0x20),

        /**
         * The transaction format is invalid. See {@link Transaction#validate(Network)}
         */
        INVALID_FORMAT(0x21),

        /**
         * The transaction timestamp is incorrect. See
         * {@link PendingManager#processTransaction(Transaction, boolean)}.
         */
        INVALID_TIMESTAMP(0x22),

        /**
         * The transaction type is invalid.
         */
        INVALID_TYPE(0x23),

        /**
         * The transaction nonce does not match the account nonce.
         */
        INVALID_NONCE(0x24),

        /**
         * The transaction fee (or gas * gasPrice) doesn't meet the minimum.
         */
        INVALID_FEE(0x25),

        /**
         * The transaction data is invalid, typically too large.
         */
        INVALID_DATA(0x27),

        /**
         * Insufficient available balance.
         */
        INSUFFICIENT_AVAILABLE(0x28),

        /**
         * Insufficient locked balance.
         */
        INSUFFICIENT_LOCKED(0x29),

        /**
         * Invalid delegate name.
         */
        INVALID_DELEGATE_NAME(0x2a),

        /**
         * Invalid burning address.
         */
        INVALID_DELEGATE_BURN_ADDRESS(0x2b),

        /**
         * Invalid delegate burn amount.
         */
        INVALID_DELEGATE_BURN_AMOUNT(0x2c),

        /**
         * The DELEGATE operation is invalid.
         */
        INVALID_DELEGATING(0x2d),

        /**
         * The VOTE operation is invalid.
         */
        INVALID_VOTING(0x2e),

        /**
         * The UNVOTE operation is invalid.
         */
        INVALID_UNVOTING(0x2f);

        private static Code[] map = new Code[256];

        static {
            for (Code code : Code.values()) {
                map[code.v] = code;
            }
        }

        private byte v;

        Code(int c) {
            this.v = (byte) c;
        }

        public static Code of(int c) {
            return map[c];
        }

        public byte toByte() {
            return v;
        }

        public boolean isSuccess() {
            return this == SUCCESS;
        }

        public boolean isFailure() {
            return this == FAILURE;
        }

        public boolean isRejected() {
            return !isSuccess() && !isFailure();
        }

        public boolean isAcceptable() {
            return isSuccess() || isFailure();
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
     * Gas info
     */
    protected long gas;
    protected long gasPrice;
    protected long gasUsed;

    /**
     * Block info
     */
    protected long blockNumber;

    /**
     * Internal transactions
     */
    protected List<SemuxInternalTransaction> internalTransactions;

    /**
     * Create a transaction result.
     */
    public TransactionResult() {
        this(Code.SUCCESS);
    }

    public TransactionResult(Code code) {
        this(code, Bytes.EMPTY_BYTES, new ArrayList<>());
    }

    public TransactionResult(Code code, byte[] returnData, List<LogInfo> logs) {
        this.code = code;
        this.returnData = returnData;
        this.logs = logs;

        this.gas = 0;
        this.gasPrice = 0;
        this.gasUsed = 0;

        this.blockNumber = 0;
        this.internalTransactions = new ArrayList<>();
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

    public long getGas() {
        return gas;
    }

    public void setGas(long gas, long gasPrice, long gasUsed) {
        this.gas = gas;
        this.gasPrice = gasPrice;
        this.gasUsed = gasUsed;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public List<SemuxInternalTransaction> getInternalTransactions() {
        return internalTransactions;
    }

    public void setInternalTransactions(List<SemuxInternalTransaction> internalTransactions) {
        this.internalTransactions = internalTransactions;
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

    protected static byte[] serializeInternalTransaction(InternalTransaction tx) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(tx.isRejected());
        enc.writeInt(tx.getDepth());
        enc.writeInt(tx.getIndex());
        enc.writeByte(tx.getType().val());
        enc.writeBytes(tx.getFrom());
        enc.writeBytes(tx.getTo());
        enc.writeLong(tx.getNonce());
        enc.writeLong(tx.getValue().longValue());
        enc.writeBytes(tx.getData());
        enc.writeLong(tx.getGas());
        enc.writeLong(tx.getGasPrice().longValue());

        return enc.toBytes();
    }

    protected static SemuxInternalTransaction deserializeInternalTransaction(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        boolean isRejected = dec.readBoolean();
        int depth = dec.readInt();
        int index = dec.readInt();
        OpCode type = OpCode.code(dec.readByte());
        byte[] from = dec.readBytes();
        byte[] to = dec.readBytes();
        long nonce = dec.readLong();
        Amount value = dec.readAmount();
        byte[] data = dec.readBytes();
        long gas = dec.readLong();
        Amount gasPrice = dec.readAmount();

        // TODO: fix the null parent
        SemuxInternalTransaction tx = new SemuxInternalTransaction(null, depth, index,
                type, from, to, nonce, value, data, gas, gasPrice);
        if (isRejected) {
            tx.reject();
        }

        return tx;
    }

    public static TransactionResult fromBytes(byte[] bytes) {
        TransactionResult result = new TransactionResult();

        SimpleDecoder dec = new SimpleDecoder(bytes);
        Code code = Code.of(dec.readByte());
        result.setCode(code);

        byte[] returnData = dec.readBytes();
        result.setReturnData(returnData);

        List<LogInfo> logs = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            logs.add(unserializeLog(dec.readBytes()));
        }
        result.setLogs(logs);

        // Dirty hack to maintain backward compatibility
        if (dec.getReadIndex() != bytes.length) {
            long gas = dec.readLong();
            long gasPrice = dec.readLong();
            long gasUsed = dec.readLong();
            result.setGas(gas, gasPrice, gasUsed);

            long blockNumber = dec.readLong();
            result.setBlockNumber(blockNumber);

            List<SemuxInternalTransaction> internalTransactions = new ArrayList<>();
            n = dec.readInt();
            for (int i = 0; i < n; i++) {
                internalTransactions.add(deserializeInternalTransaction(dec.readBytes()));
            }
            result.setInternalTransactions(internalTransactions);
        }

        return result;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(code.toByte());
        enc.writeBytes(returnData);
        enc.writeInt(logs.size());
        for (LogInfo log : logs) {
            enc.writeBytes(serializeLog(log));
        }

        // The following fields are incompatible with old versions,
        // but it's fine because the old clients are not reading
        // these data.

        enc.writeLong(gas);
        enc.writeLong(gasPrice);
        enc.writeLong(gasUsed);

        enc.writeLong(blockNumber);

        enc.writeInt(internalTransactions.size());
        for (InternalTransaction tx : internalTransactions) {
            enc.writeBytes(serializeInternalTransaction(tx));
        }

        return enc.toBytes();
    }

    public byte[] toBytesForMerkle() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(code.toByte());
        enc.writeBytes(returnData);
        enc.writeInt(logs.size());
        for (LogInfo log : logs) {
            enc.writeBytes(serializeLog(log));
        }

        return enc.toBytes();
    }

    @Override
    public String toString() {
        return "TransactionResult [code=" + code + ", returnData=" + Hex.encode(returnData) + ", # logs="
                + logs.size() + "]";
    }
}
