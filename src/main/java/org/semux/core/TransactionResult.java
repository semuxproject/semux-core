/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.List;

import org.semux.utils.Bytes;

public class TransactionResult {
    private boolean success;
    private byte[] output;
    private List<byte[]> logs;

    /**
     * Create a default transaction result:
     * 
     * <pre>
     * [success = false, output = empty, logs = empty]
     * </pre>
     */
    public TransactionResult() {
        this.success = false;
        this.output = Bytes.EMPY_BYTES;
        this.logs = new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
}