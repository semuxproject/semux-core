/*
 * Copyright (c) [2018] [ The Semux Developers ]
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.ethereum.vm.OpCode.CALL;
import static org.ethereum.vm.OpCode.PUSH1;
import static org.ethereum.vm.OpCode.REVERT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.ethereum.vm.config.Config;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.Stack;
import org.ethereum.vm.program.exception.ExceptionFactory;
import org.ethereum.vm.program.exception.ReturnDataCopyIllegalBoundsException;
import org.ethereum.vm.program.exception.StaticCallModificationException;
import org.ethereum.vm.util.VMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Ethereum Virtual Machine (EVM) is responsible for initialization and
 * executing a transaction on a contract.
 *
 * It is a quasi-Turing-complete machine; the quasi qualification comes from the
 * fact that the computation is intrinsically bounded through a parameter, gas,
 * which limits the total amount of computation done.
 *
 * The EVM is a simple stack-based architecture. The word size of the machine
 * (and thus size of stack item) is 256-bit. This was chosen to facilitate the
 * SHA3-256 hash scheme and elliptic-curve computations. The memory model is a
 * simple word-addressed byte array. The stack has an unlimited size. The
 * machine also has an independent storage model; this is similar in concept to
 * the memory but rather than a byte array, it is a word-addressable word array.
 *
 * Unlike memory, which is volatile, storage is non volatile and is maintained
 * as part of the system state. All locations in both storage and memory are
 * well-defined initially as zero.
 *
 * The machine does not follow the standard von Neumann architecture. Rather
 * than storing program code in generally-accessible memory or storage, it is
 * stored separately in a virtual ROM interactable only though a specialised
 * instruction.
 *
 * The machine can have exceptional execution for several reasons, including
 * stack underflows and invalid instructions. These unambiguously and validly
 * result in immediate halting of the machine with all state changes left
 * intact. The one piece of exceptional execution that does not leave state
 * changes intact is the out-of-gas (OOG) exception.
 *
 * Here, the machine halts immediately and reports the issue to the execution
 * agent (either the transaction processor or, recursively, the spawning
 * execution environment) and which will deal with it separately.
 */
public class VM {

    private static final Logger logger = LoggerFactory.getLogger(VM.class);

    private static final BigInteger THIRTY_TWO = BigInteger.valueOf(32);

    // theoretical limit, used to reduce expensive BigInt arithmetic
    private static final BigInteger MAX_MEM_SIZE = BigInteger.valueOf(Integer.MAX_VALUE);

    private Config config;

    public VM() {
        this(Config.DEFAULT);
    }

    public VM(Config config) {
        this.config = config;
    }

    private long calcMemGas(FeeSchedule feeSchedule, long oldMemSize, BigInteger newMemSize, long copySize) {
        long gasCost = 0;

        // avoid overflows
        if (newMemSize.compareTo(MAX_MEM_SIZE) > 0) {
            throw ExceptionFactory.gasOverflow(newMemSize, MAX_MEM_SIZE);
        }

        // memory gas calc
        long memoryUsage = (newMemSize.longValue() + 31) / 32 * 32;
        if (memoryUsage > oldMemSize) {
            long memWords = (memoryUsage / 32);
            long memWordsOld = (oldMemSize / 32);
            long memGas = (feeSchedule.getMEMORY() * memWords + memWords * memWords / 512)
                    - (feeSchedule.getMEMORY() * memWordsOld + memWordsOld * memWordsOld / 512);
            gasCost += memGas;
        }

        if (copySize > 0) {
            long copyGas = feeSchedule.getCOPY_GAS() * ((copySize + 31) / 32);
            gasCost += copyGas;
        }
        return gasCost;
    }

    private boolean isDeadAccount(Program program, byte[] addr) {
        // TODO: check EVM specification
        return false;
    }

    public void step(Program program) {
        try {
            OpCode op = OpCode.code(program.getCurrentOp());
            if (op == null) {
                throw ExceptionFactory.invalidOpCode(program.getCurrentOp());
            }

            program.verifyStackUnderflow(op.require());
            program.verifyStackOverflow(op.require(), op.ret()); // Check not exceeding stack limits

            long oldMemSize = program.getMemSize();
            Stack stack = program.getStack();

            long gasCost = op.getTier().asInt();
            FeeSchedule feeSchedule = config.getFeeSchedule();
            DataWord adjustedCallGas = null;

            // Calculate fees and spend gas
            switch (op) {
            case STOP:
                gasCost = feeSchedule.getSTOP();
                break;
            case SUICIDE:
                gasCost = feeSchedule.getSUICIDE();
                DataWord suicideAddressWord = stack.get(stack.size() - 1);
                if (isDeadAccount(program, suicideAddressWord.getLast20Bytes()) &&
                        !program.getBalance(program.getOwnerAddress()).isZero()) {
                    gasCost += feeSchedule.getNEW_ACCT_SUICIDE();
                }
                break;
            case SSTORE:
                DataWord newValue = stack.get(stack.size() - 2);
                DataWord oldValue = program.storageLoad(stack.peek());

                if (oldValue == null && !newValue.isZero()) {
                    gasCost = feeSchedule.getSET_SSTORE();
                } else if (oldValue != null && newValue.isZero()) {
                    program.futureRefundGas(feeSchedule.getREFUND_SSTORE());
                    gasCost = feeSchedule.getCLEAR_SSTORE();
                } else {
                    gasCost = feeSchedule.getRESET_SSTORE();
                }
                break;
            case SLOAD:
                gasCost = feeSchedule.getSLOAD();
                break;
            case BALANCE:
                gasCost = feeSchedule.getBALANCE();
                break;

            // These all operate on memory and therefore potentially expand it:
            case MSTORE:
                gasCost += calcMemGas(feeSchedule, oldMemSize, memNeeded(stack.peek(), new DataWord(32)), 0);
                break;
            case MSTORE8:
                gasCost += calcMemGas(feeSchedule, oldMemSize, memNeeded(stack.peek(), new DataWord(1)), 0);
                break;
            case MLOAD:
                gasCost += calcMemGas(feeSchedule, oldMemSize, memNeeded(stack.peek(), new DataWord(32)), 0);
                break;
            case RETURN:
            case REVERT:
                gasCost = feeSchedule.getSTOP() + calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0);
                break;
            case SHA3:
                gasCost = feeSchedule.getSHA3() + calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0);
                DataWord size = stack.get(stack.size() - 2);
                long chunkUsed = (size.longValueSafe() + 31) / 32;
                gasCost += chunkUsed * feeSchedule.getSHA3_WORD();
                break;
            case CALLDATACOPY:
            case RETURNDATACOPY:
                gasCost += calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                        stack.get(stack.size() - 3).longValueSafe());
                break;
            case CODECOPY:
                gasCost += calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                        stack.get(stack.size() - 3).longValueSafe());
                break;
            case EXTCODESIZE:
                gasCost = feeSchedule.getEXT_CODE_SIZE();
                break;
            case EXTCODECOPY:
                gasCost = feeSchedule.getEXT_CODE_COPY() + calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 4)),
                        stack.get(stack.size() - 4).longValueSafe());
                break;
            case CALL:
            case CALLCODE:
            case DELEGATECALL:
            case STATICCALL:

                gasCost = feeSchedule.getCALL();
                DataWord callGasWord = stack.get(stack.size() - 1);

                DataWord callAddressWord = stack.get(stack.size() - 2);

                DataWord value = op.callHasValue() ? stack.get(stack.size() - 3) : DataWord.ZERO;

                // check to see if account does not exist and is not a precompiled contract
                if (op == CALL) {
                    if (isDeadAccount(program, callAddressWord.getLast20Bytes()) && !value.isZero()) {
                        gasCost += feeSchedule.getNEW_ACCT_CALL();
                    }
                }

                // TODO: Make sure this is converted to BigInteger (256num support)
                if (!value.isZero())
                    gasCost += feeSchedule.getVT_CALL();

                int opOff = op.callHasValue() ? 4 : 3;
                BigInteger in = memNeeded(stack.get(stack.size() - opOff),
                        stack.get(stack.size() - opOff - 1)); // in offset+size
                BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2),
                        stack.get(stack.size() - opOff - 3)); // out offset+size
                gasCost += calcMemGas(feeSchedule, oldMemSize, in.max(out), 0);

                if (gasCost > program.getGas().longValueSafe()) {
                    throw ExceptionFactory.notEnoughOpGas(op, callGasWord, program.getGas());
                }

                DataWord gasLeft = program.getGas();
                gasLeft = gasLeft.sub(new DataWord(gasCost));
                adjustedCallGas = config.getCallGas(op, callGasWord, gasLeft);
                gasCost += adjustedCallGas.longValueSafe();
                break;
            case CREATE:
                gasCost = feeSchedule.getCREATE() + calcMemGas(feeSchedule, oldMemSize,
                        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0);
                break;
            case LOG0:
            case LOG1:
            case LOG2:
            case LOG3:
            case LOG4:
                int nTopics = op.val() - OpCode.LOG0.val();

                BigInteger dataSize = stack.get(stack.size() - 2).value();
                BigInteger dataCost = dataSize.multiply(BigInteger.valueOf(feeSchedule.getLOG_DATA_GAS()));
                if (program.getGas().value().compareTo(dataCost) < 0) {
                    throw ExceptionFactory.notEnoughOpGas(op, dataCost, program.getGas().value());
                }

                gasCost = feeSchedule.getLOG_GAS() +
                        feeSchedule.getLOG_TOPIC_GAS() * nTopics +
                        feeSchedule.getLOG_DATA_GAS() * stack.get(stack.size() - 2).longValue() +
                        calcMemGas(feeSchedule, oldMemSize, memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0);
                break;
            case EXP:

                DataWord exp = stack.get(stack.size() - 2);
                int bytesOccupied = exp.bytesOccupied();
                gasCost = feeSchedule.getEXP_GAS() + feeSchedule.getEXP_BYTE_GAS() * bytesOccupied;
                break;
            default:
                break;
            }

            program.spendGas(gasCost, op.name());

            // Execute operation
            switch (op) {

            case STOP: {
                program.setHReturn(EMPTY_BYTE_ARRAY);
                program.stop();
            }
                break;
            case ADD: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.add(word2);
                program.stackPush(result);
                program.step();

            }
                break;
            case MUL: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.mul(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case SUB: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.sub(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case DIV: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.div(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case SDIV: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.sDiv(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case MOD: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.mod(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case SMOD: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.sMod(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case EXP: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.exp(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case SIGNEXTEND: {
                DataWord word1 = program.stackPop();
                BigInteger k = word1.value();

                if (k.compareTo(THIRTY_TWO) < 0) {
                    DataWord word2 = program.stackPop();
                    DataWord result = word2.signExtend(k.byteValue());
                    program.stackPush(result);
                }
                program.step();
            }
                break;
            case NOT: {
                DataWord word1 = program.stackPop();
                DataWord result = word1.bnot();

                program.stackPush(result);
                program.step();
            }
                break;
            case LT: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = (word1.value().compareTo(word2.value()) < 0) ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;
            case SLT: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = (word1.sValue().compareTo(word2.sValue()) < 0) ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;
            case SGT: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = (word1.sValue().compareTo(word2.sValue()) > 0) ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;
            case GT: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = (word1.value().compareTo(word2.value()) > 0) ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;
            case EQ: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.equals(word2) ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;
            case ISZERO: {
                DataWord word1 = program.stackPop();
                DataWord result = word1.isZero() ? DataWord.ONE : DataWord.ZERO;
                program.stackPush(result);
                program.step();
            }
                break;

            case AND: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.and(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case OR: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.or(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case XOR: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();

                DataWord result = word1.xor(word2);
                program.stackPush(result);
                program.step();
            }
                break;
            case BYTE: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();
                final DataWord result;
                if (word1.value().compareTo(THIRTY_TWO) < 0) {
                    byte tmp = word2.getByte(word1.intValue());
                    result = new DataWord(new byte[] { tmp });
                } else {
                    result = DataWord.ZERO;
                }

                program.stackPush(result);
                program.step();
            }
                break;
            case ADDMOD: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();
                DataWord word3 = program.stackPop();
                DataWord result = word1.addmod(word2, word3);
                program.stackPush(result);
                program.step();
            }
                break;
            case MULMOD: {
                DataWord word1 = program.stackPop();
                DataWord word2 = program.stackPop();
                DataWord word3 = program.stackPop();
                DataWord result = word1.mulmod(word2, word3);
                program.stackPush(result);
                program.step();
            }
                break;

            case SHA3: {
                DataWord memOffsetData = program.stackPop();
                DataWord lengthData = program.stackPop();
                byte[] buffer = program.memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

                byte[] encoded = VMUtil.keccak256(buffer);
                DataWord word = new DataWord(encoded);

                program.stackPush(word);
                program.step();
            }
                break;

            case ADDRESS: {
                DataWord address = program.getOwnerAddress();

                program.stackPush(address);
                program.step();
            }
                break;
            case BALANCE: {
                DataWord address = program.stackPop();
                DataWord balance = program.getBalance(address);

                program.stackPush(balance);
                program.step();
            }
                break;
            case ORIGIN: {
                DataWord originAddress = program.getOriginAddress();

                program.stackPush(originAddress);
                program.step();
            }
                break;
            case CALLER: {
                DataWord callerAddress = program.getCallerAddress();

                program.stackPush(callerAddress);
                program.step();
            }
                break;
            case CALLVALUE: {
                DataWord callValue = program.getCallValue();

                program.stackPush(callValue);
                program.step();
            }
                break;
            case CALLDATALOAD: {
                DataWord dataOffs = program.stackPop();
                DataWord value = program.getDataValue(dataOffs);

                program.stackPush(value);
                program.step();
            }
                break;
            case CALLDATASIZE: {
                DataWord dataSize = program.getDataSize();

                program.stackPush(dataSize);
                program.step();
            }
                break;
            case CALLDATACOPY: {
                DataWord memOffsetData = program.stackPop();
                DataWord dataOffsetData = program.stackPop();
                DataWord lengthData = program.stackPop();

                byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);

                program.memorySave(memOffsetData.intValueSafe(), lengthData.intValueSafe(), msgData);
                program.step();
            }
                break;
            case RETURNDATASIZE: {
                DataWord dataSize = program.getReturnDataBufferSize();

                program.stackPush(dataSize);
                program.step();
            }
                break;
            case RETURNDATACOPY: {
                DataWord memOffsetData = program.stackPop();
                DataWord dataOffsetData = program.stackPop();
                DataWord lengthData = program.stackPop();

                byte[] msgData = program.getReturnDataBufferData(dataOffsetData, lengthData);

                if (msgData == null) {
                    throw new ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData,
                            program.getReturnDataBufferSize().longValueSafe());
                }

                program.memorySave(memOffsetData.intValueSafe(), lengthData.intValueSafe(), msgData);
                program.step();
            }
                break;
            case CODESIZE:
            case EXTCODESIZE: {

                int length;
                if (op == OpCode.CODESIZE)
                    length = program.getCode().length;
                else {
                    DataWord address = program.stackPop();
                    length = program.getCodeAt(address).length;
                }
                DataWord codeLength = new DataWord(length);

                program.stackPush(codeLength);
                program.step();
            }
                break;
            case CODECOPY:
            case EXTCODECOPY: {

                byte[] fullCode = EMPTY_BYTE_ARRAY;
                if (op == OpCode.CODECOPY)
                    fullCode = program.getCode();

                if (op == OpCode.EXTCODECOPY) {
                    DataWord address = program.stackPop();
                    fullCode = program.getCodeAt(address);
                }

                int memOffset = program.stackPop().intValueSafe();
                int codeOffset = program.stackPop().intValueSafe();
                int lengthData = program.stackPop().intValueSafe();

                int sizeToBeCopied = (long) codeOffset + lengthData > fullCode.length
                        ? (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
                        : lengthData;

                byte[] codeCopy = new byte[lengthData];

                if (codeOffset < fullCode.length)
                    System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);

                program.memorySave(memOffset, lengthData, codeCopy);
                program.step();
            }
                break;
            case GASPRICE: {
                DataWord gasPrice = program.getGasPrice();

                program.stackPush(gasPrice);
                program.step();
            }
                break;

            case BLOCKHASH: {

                int blockIndex = program.stackPop().intValueSafe();

                DataWord blockHash = program.getBlockHash(blockIndex);

                program.stackPush(blockHash);
                program.step();
            }
                break;
            case COINBASE: {
                DataWord coinbase = program.getCoinbase();

                program.stackPush(coinbase);
                program.step();
            }
                break;
            case TIMESTAMP: {
                DataWord timestamp = program.getTimestamp();

                program.stackPush(timestamp);
                program.step();
            }
                break;
            case NUMBER: {
                DataWord number = program.getNumber();

                program.stackPush(number);
                program.step();
            }
                break;
            case DIFFICULTY: {
                DataWord difficulty = program.getDifficulty();

                program.stackPush(difficulty);
                program.step();
            }
                break;
            case GASLIMIT: {
                DataWord gaslimit = program.getGasLimit();

                program.stackPush(gaslimit);
                program.step();
            }
                break;
            case POP: {
                program.stackPop();
                program.step();
            }
                break;
            case DUP1:
            case DUP2:
            case DUP3:
            case DUP4:
            case DUP5:
            case DUP6:
            case DUP7:
            case DUP8:
            case DUP9:
            case DUP10:
            case DUP11:
            case DUP12:
            case DUP13:
            case DUP14:
            case DUP15:
            case DUP16: {
                int n = op.val() - OpCode.DUP1.val() + 1;
                program.stackPush(stack.get(stack.size() - n)); // same object ref
                program.step();

            }
                break;
            case SWAP1:
            case SWAP2:
            case SWAP3:
            case SWAP4:
            case SWAP5:
            case SWAP6:
            case SWAP7:
            case SWAP8:
            case SWAP9:
            case SWAP10:
            case SWAP11:
            case SWAP12:
            case SWAP13:
            case SWAP14:
            case SWAP15:
            case SWAP16: {

                int n = op.val() - OpCode.SWAP1.val() + 2;
                stack.swap(stack.size() - 1, stack.size() - n);
                program.step();
            }
                break;
            case LOG0:
            case LOG1:
            case LOG2:
            case LOG3:
            case LOG4: {

                if (program.isStaticCall())
                    throw new StaticCallModificationException();
                DataWord address = program.getOwnerAddress();

                DataWord memStart = stack.pop();
                DataWord memOffset = stack.pop();

                int nTopics = op.val() - OpCode.LOG0.val();

                List<DataWord> topics = new ArrayList<>();
                for (int i = 0; i < nTopics; ++i) {
                    DataWord topic = stack.pop();
                    topics.add(topic);
                }

                byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

                LogInfo logInfo = new LogInfo(address.getLast20Bytes(), topics, data);

                program.getResult().addLogInfo(logInfo);
                program.step();
            }
                break;
            case MLOAD: {
                DataWord addr = program.stackPop();
                DataWord data = program.memoryLoad(addr);

                program.stackPush(data);
                program.step();
            }
                break;
            case MSTORE: {
                DataWord addr = program.stackPop();
                DataWord value = program.stackPop();

                program.memorySave(addr, value);
                program.step();
            }
                break;
            case MSTORE8: {
                DataWord addr = program.stackPop();
                DataWord value = program.stackPop();
                byte[] byteVal = { value.getByte(31) };
                program.memorySave(addr.intValueSafe(), byteVal);
                program.step();
            }
                break;
            case SLOAD: {
                DataWord key = program.stackPop();
                DataWord val = program.storageLoad(key);

                if (val == null) {
                    val = DataWord.ZERO;
                }

                program.stackPush(val);
                program.step();
            }
                break;
            case SSTORE: {
                if (program.isStaticCall())
                    throw new StaticCallModificationException();

                DataWord addr = program.stackPop();
                DataWord value = program.stackPop();

                program.storageSave(addr, value);
                program.step();
            }
                break;
            case JUMP: {
                DataWord pos = program.stackPop();
                int nextPC = program.verifyJumpDest(pos);

                program.setPC(nextPC);
            }
                break;
            case JUMPI: {
                DataWord pos = program.stackPop();
                DataWord cond = program.stackPop();

                if (!cond.isZero()) {
                    int nextPC = program.verifyJumpDest(pos);

                    program.setPC(nextPC);
                } else {
                    program.step();
                }

            }
                break;
            case PC: {
                int pc = program.getPC();
                DataWord pcWord = new DataWord(pc);

                program.stackPush(pcWord);
                program.step();
            }
                break;
            case MSIZE: {
                int memSize = program.getMemSize();
                DataWord wordMemSize = new DataWord(memSize);

                program.stackPush(wordMemSize);
                program.step();
            }
                break;
            case GAS: {
                DataWord gas = program.getGas();

                program.stackPush(gas);
                program.step();
            }
                break;

            case PUSH1:
            case PUSH2:
            case PUSH3:
            case PUSH4:
            case PUSH5:
            case PUSH6:
            case PUSH7:
            case PUSH8:
            case PUSH9:
            case PUSH10:
            case PUSH11:
            case PUSH12:
            case PUSH13:
            case PUSH14:
            case PUSH15:
            case PUSH16:
            case PUSH17:
            case PUSH18:
            case PUSH19:
            case PUSH20:
            case PUSH21:
            case PUSH22:
            case PUSH23:
            case PUSH24:
            case PUSH25:
            case PUSH26:
            case PUSH27:
            case PUSH28:
            case PUSH29:
            case PUSH30:
            case PUSH31:
            case PUSH32: {
                program.step();
                int nPush = op.val() - PUSH1.val() + 1;

                byte[] data = program.sweep(nPush);

                program.stackPush(data);
            }
                break;
            case JUMPDEST: {
                program.step();
            }
                break;
            case CREATE: {
                if (program.isStaticCall())
                    throw new StaticCallModificationException();

                DataWord value = program.stackPop();
                DataWord inOffset = program.stackPop();
                DataWord inSize = program.stackPop();

                program.createContract(value, inOffset, inSize);

                program.step();
            }
                break;
            case CALL:
            case CALLCODE:
            case DELEGATECALL:
            case STATICCALL: {
                program.stackPop(); // use adjustedCallGas instead of requested
                DataWord codeAddress = program.stackPop();
                DataWord value = op.callHasValue() ? program.stackPop() : DataWord.ZERO;

                if (program.isStaticCall() && op == CALL && !value.isZero())
                    throw new StaticCallModificationException();

                if (!value.isZero()) {
                    adjustedCallGas = adjustedCallGas.add(new DataWord(feeSchedule.getSTIPEND_CALL()));
                }

                DataWord inDataOffs = program.stackPop();
                DataWord inDataSize = program.stackPop();

                DataWord outDataOffs = program.stackPop();
                DataWord outDataSize = program.stackPop();

                program.memoryExpand(outDataOffs, outDataSize);

                MessageCall msg = new MessageCall(
                        op, adjustedCallGas, codeAddress, value, inDataOffs, inDataSize,
                        outDataOffs, outDataSize);

                PrecompiledContracts.PrecompiledContract contract = PrecompiledContracts
                        .getContractForAddress(codeAddress, config);

                if (contract != null) {
                    program.callToPrecompiledAddress(msg, contract);
                } else {
                    program.callToAddress(msg);
                }

                program.step();
            }
                break;
            case RETURN:
            case REVERT: {
                DataWord offset = program.stackPop();
                DataWord size = program.stackPop();

                byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
                program.setHReturn(hReturn);

                program.step();
                program.stop();

                if (op == REVERT) {
                    program.getResult().setRevert();
                }
            }
                break;
            case SUICIDE: {
                if (program.isStaticCall())
                    throw new StaticCallModificationException();

                DataWord address = program.stackPop();
                program.suicide(address);

                program.stop();
            }
                break;
            default:
                break;
            }

        } catch (RuntimeException e) {
            program.spendAllGas();
            program.resetFutureRefund();
            program.stop();
            throw e;
        }
    }

    public void play(Program program) {
        try {
            while (!program.isStopped()) {
                this.step(program);
            }

        } catch (RuntimeException e) {
            program.setRuntimeFailure(e);
        } catch (StackOverflowError soe) {
            logger.error("\n !!! StackOverflowError: update your java run command with -Xss2M !!!\n", soe);
            System.exit(-1);
        }
    }

    /**
     * Utility to calculate new total memory size needed for an operation. <br/>
     * Basically just offset + size, unless size is 0, in which case the result is
     * also 0.
     *
     * @param offset
     *            starting position of the memory
     * @param size
     *            number of bytes needed
     * @return offset + size, unless size is 0. In that case memNeeded is also 0.
     */
    private static BigInteger memNeeded(DataWord offset, DataWord size) {
        return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
    }
}