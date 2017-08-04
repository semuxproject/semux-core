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

public class TransactionResult {

    /**
     * Indicate whether this transaction is valid or not.
     */
    protected boolean valid;

    /**
     * Error indicator.
     */
    protected int code;

    /**
     * Transaction output/return.
     */
    protected byte[] output;

    /**
     * Transaction logs.
     */
    protected List<byte[]> logs;

    /**
     * Create a default transaction result:
     * 
     * <pre>
     * [valid = false, code = 1, output = empty, logs = empty]
     * </pre>
     */
    public TransactionResult() {
        this.valid = false;
        this.code = 1;
        this.output = Bytes.EMPY_BYTES;
        this.logs = new ArrayList<>();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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

    @Override
    public String toString() {
        return "TransactionResult [valid=" + valid + ", error=" + code + ", output=" + Arrays.toString(output)
                + ", logs=" + logs + "]";
    }
}