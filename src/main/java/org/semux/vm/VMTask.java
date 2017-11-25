/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

/**
 * Represents a VM task
 *
 */
public class VMTask {
    private SemuxRuntime runtime;
    private byte[] code;
    private long limit;

    private boolean isDone;

    public VMTask(SemuxRuntime runtime, byte[] code, long limit) {
        super();
        this.runtime = runtime;
        this.code = code;
        this.limit = limit;
    }

    public SemuxRuntime getRuntime() {
        return runtime;
    }

    public byte[] getCode() {
        return code;
    }

    public long getLimit() {
        return limit;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean isDone) {
        this.isDone = isDone;
    }
}