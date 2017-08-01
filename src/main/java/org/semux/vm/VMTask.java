/*
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
    private byte[] ops;
    private long gas;

    private volatile boolean isDone;

    public VMTask(SemuxRuntime runtime, byte[] ops, long gas) {
        super();
        this.runtime = runtime;
        this.ops = ops;
        this.gas = gas;
    }

    public SemuxRuntime getRuntime() {
        return runtime;
    }

    public byte[] getOps() {
        return ops;
    }

    public long getGas() {
        return gas;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean isDone) {
        this.isDone = isDone;
    }
}