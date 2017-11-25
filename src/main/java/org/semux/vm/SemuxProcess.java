/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import java.util.concurrent.atomic.AtomicLong;

import org.semux.Config;
import org.semux.vm.exception.InvalidOpCodeException;
import org.semux.vm.exception.OutOfGasException;
import org.semux.vm.exception.StackOverflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxProcess implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SemuxProcess.class);

    private static AtomicLong cnt = new AtomicLong(0);
    private long pid;

    private SemuxRuntime rt;
    private byte[] ops;
    private long gasLimit;

    private Status status = Status.INIT;

    private long[] stack = new long[Config.VM_STACK_SIZE_LIMIT];
    private byte[] heap = new byte[Config.VM_HEAP_SIZE_LIMIT];

    /**
     * Create a process that runs the specified byte code, with a gas limit.
     * 
     * @param rt
     * @param ops
     * @param gasLimit
     */
    public SemuxProcess(SemuxRuntime rt, byte[] ops, long gasLimit) {
        this.pid = cnt.getAndIncrement();
        this.rt = rt;
        this.ops = ops;
        this.gasLimit = gasLimit;
    }

    @Override
    public void run() {
        if (status != Status.INIT) {
            logger.error("This process has been started, status: " + status);
            return;
        } else {
            status = Status.RUNNING;
        }

        try {
            int size = 0; // stack size
            int gas = 0; // gas used

            for (byte op1 : ops) {
                // check gas
                if (gas >= gasLimit) {
                    throw new OutOfGasException();
                }

                // read opcode
                Opcode op = Opcode.of(0xff & op1);
                if (op == null) {
                    throw new InvalidOpCodeException(op1);
                }

                // check stack requirements
                if (op.getRequires() > size || op.getProduces() > stack.length - size) {
                    throw new StackOverflowException();
                }

                // TODO implement opcodes
                switch (op) {
                case STOP: {
                    gas++;
                    break;
                }
                case ADD: {
                    gas++;
                    break;
                }
                case SUB: {
                    gas++;
                    break;
                }
                case MUL: {
                    gas++;
                    break;
                }
                case DIV: {
                    gas++;
                    break;
                }
                case MOD: {
                    gas++;
                    break;
                }
                case EXP: {
                    gas++;
                    break;
                }
                case LT: {
                    gas++;
                    break;
                }
                case GT: {
                    gas++;
                    break;
                }
                case EQ: {
                    gas++;
                    break;
                }
                case ISZERO: {
                    gas++;
                    break;
                }
                case AND: {
                    gas++;
                    break;
                }
                case OR: {
                    gas++;
                    break;
                }
                case XOR: {
                    gas++;
                    break;
                }
                case NOT: {
                    gas++;
                    break;
                }
                case BYTE: {
                    gas++;
                    break;
                }
                case H256: {
                    gas++;
                    break;
                }
                case H160: {
                    gas++;
                    break;
                }
                case ADDRESS: {
                    gas++;
                    break;
                }
                case AVAILABLE: {
                    gas++;
                    break;
                }
                case CALLER: {
                    gas++;
                    break;
                }
                case CALLVALUE: {
                    gas++;
                    break;
                }
                case CALLDATASIZE: {
                    gas++;
                    break;
                }
                case CALLDATALOAD: {
                    gas++;
                    break;
                }
                case CALLDATACOPY: {
                    gas++;
                    break;
                }
                case BLOCKHASH: {
                    gas++;
                    break;
                }
                case COINBASE: {
                    gas++;
                    break;
                }
                case TIMESTAMP: {
                    gas++;
                    break;
                }
                case NUMBER: {
                    gas++;
                    break;
                }
                case POP: {
                    gas++;
                    break;
                }
                case MLOAD: {
                    gas++;
                    break;
                }
                case MSTORE: {
                    gas++;
                    break;
                }
                case SLOAD: {
                    gas++;
                    break;
                }
                case SSTORE: {
                    gas++;
                    break;
                }
                case JUMPDEST: {
                    gas++;
                    break;
                }
                case JUMP: {
                    gas++;
                    break;
                }
                case JUMPI: {
                    gas++;
                    break;
                }
                case PC: {
                    gas++;
                    break;
                }
                case PUSH1: {
                    gas++;
                    break;
                }
                case PUSH2: {
                    gas++;
                    break;
                }
                case PUSH3: {
                    gas++;
                    break;
                }
                case PUSH4: {
                    gas++;
                    break;
                }
                case PUSH5: {
                    gas++;
                    break;
                }
                case PUSH6: {
                    gas++;
                    break;
                }
                case PUSH7: {
                    gas++;
                    break;
                }
                case PUSH8: {
                    gas++;
                    break;
                }
                case DUP1: {
                    gas++;
                    break;
                }
                case DUP2: {
                    gas++;
                    break;
                }
                case DUP3: {
                    gas++;
                    break;
                }
                case DUP4: {
                    gas++;
                    break;
                }
                case DUP5: {
                    gas++;
                    break;
                }
                case DUP6: {
                    gas++;
                    break;
                }
                case DUP7: {
                    gas++;
                    break;
                }
                case DUP8: {
                    gas++;
                    break;
                }
                case SWAP1: {
                    gas++;
                    break;
                }
                case SWAP2: {
                    gas++;
                    break;
                }
                case SWAP3: {
                    gas++;
                    break;
                }
                case SWAP4: {
                    gas++;
                    break;
                }
                case SWAP5: {
                    gas++;
                    break;
                }
                case SWAP6: {
                    gas++;
                    break;
                }
                case SWAP7: {
                    gas++;
                    break;
                }
                case SWAP8: {
                    gas++;
                    break;
                }
                case SEND: {
                    gas++;
                    break;
                }
                case LOG: {
                    gas++;
                    break;
                }
                case RETURN: {
                    gas++;
                    break;
                }
                }
            }
        } catch (Throwable e) {
            logger.debug("VM exception", e);
        } finally {
            status = Status.STOPPED;
        }
    }

    public long getPid() {
        return pid;
    }

    public SemuxRuntime getRt() {
        return rt;
    }

    public byte[] getOps() {
        return ops;
    }

    public Status getStatus() {
        return status;
    }

    public long[] getStack() {
        return stack;
    }

    public byte[] getMemory() {
        return heap;
    }

    @Override
    public String toString() {
        return "SemuxProcess [pid=" + pid + ", ops.length=" + ops.length + ", gasLimit=" + gasLimit + ", status="
                + status + "]";
    }

    public enum Status {
        INIT, RUNNING, STOPPED
    }
}
