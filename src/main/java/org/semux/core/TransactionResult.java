/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.semux.utils.Bytes;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class TransactionResult {

    /**
     * Indicates whether this transaction is valid or not.
     */
    protected boolean valid;

    /**
     * Transaction output.
     */
    protected byte[] output;

    /**
     * Transaction logs.
     */
    protected List<byte[]> logs;

    /**
     * Create a transaction result.
     * 
     * @param valid
     * @param output
     * @param logs
     */
    public TransactionResult(boolean valid, byte[] output, List<byte[]> logs) {
        super();
        this.valid = valid;
        this.output = output;
        this.logs = logs;
    }

    /**
     * Create a transaction result.
     * 
     * @param valid
     */
    public TransactionResult(boolean valid) {
        this(valid, Bytes.EMPY_BYTES, new ArrayList<>());
    }

    /**
     * Create a transaction result.
     */
    public TransactionResult() {
        this(false);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public byte[] getOutput() {
        return output;
    }

    public void setOutput(byte[] output) {
        this.output = output;
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
        enc.writeBoolean(valid);
        enc.writeBytes(output);
        enc.writeInt(logs.size());
        for (byte[] log : logs) {
            enc.writeBytes(log);
        }

        return enc.toBytes();
    }

    public static TransactionResult fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        boolean valid = dec.readBoolean();
        byte[] output = dec.readBytes();
        List<byte[]> logs = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            logs.add(dec.readBytes());
        }

        return new TransactionResult(valid, output, logs);
    }

    @Override
    public String toString() {
        return "TransactionResult [valid=" + valid + ", output=" + Arrays.toString(output) + ", # logs=" + logs.size()
                + "]";
    }
}