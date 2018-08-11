/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
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
package org.ethereum.vm.program;

import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.MessageCall;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.VM;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.config.Config;
import org.ethereum.vm.program.exception.BytecodeExecutionException;
import org.ethereum.vm.program.exception.ExceptionFactory;
import org.ethereum.vm.program.exception.StackTooSmallException;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.util.VMUtils;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Program {

    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    /**
     * This attribute defines the number of recursive calls allowed in the EVM Note:
     * For the JVM to reach this level without a StackOverflow exception, ethereumj
     * may need to be started with a JVM argument to increase the stack size. For
     * example: -Xss10m
     */
    private static final int MAX_DEPTH = 1024;

    // Max size for stack checks
    private static final int MAX_STACKSIZE = 1024;

    private Transaction transaction;

    private ProgramInvoke invoke;
    private ProgramInvokeFactory programInvokeFactory; // TODO: add implementation

    private Config config;
    private ProgramPrecompile programPrecompile;

    private Stack stack;
    private Memory memory;
    private Storage storage;
    private byte[] returnDataBuffer;

    private ProgramResult result = new ProgramResult();

    private byte[] codeHash;
    private byte[] ops;
    private int pc;
    private byte lastOp;
    private byte previouslyExecutedOp;
    private boolean stopped;
    private Set<ByteArray> touchedAccounts = new HashSet<ByteArray>();

    public Program(byte[] ops, ProgramInvoke programInvoke, Transaction transaction, Config config) {
        this.ops = nullToEmpty(ops);
        this.invoke = programInvoke;

        this.memory = new Memory();
        this.stack = new Stack();
        this.storage = new Storage(programInvoke);

        this.transaction = transaction;
        this.config = config;
    }

    public ProgramPrecompile getProgramPrecompile() {
        if (programPrecompile == null) {
            programPrecompile = ProgramPrecompile.compile(ops);
        }
        return programPrecompile;
    }

    public int getCallDeep() {
        return invoke.getCallDeep();
    }

    private InternalTransaction addInternalTx(byte[] nonce, DataWord gasLimit, byte[] senderAddress,
            byte[] receiveAddress,
            BigInteger value, byte[] data, String note) {

        InternalTransaction result = null;
        if (transaction != null) {
            byte[] senderNonce = isEmpty(nonce) ? getStorage().getNonce(senderAddress).toByteArray() : nonce;
            result = getResult().addInternalTransaction(transaction.getHash(), getCallDeep(), senderNonce,
                    getGasPrice(), gasLimit, senderAddress, receiveAddress, value.toByteArray(), data, note);
        }

        return result;
    }

    public byte getOp(int pc) {
        return (getLength(ops) <= pc) ? 0 : ops[pc];
    }

    public byte getCurrentOp() {
        return isEmpty(ops) ? 0 : ops[pc];
    }

    /**
     * Last Op can only be set publicly (no getLastOp method), is used for logging.
     */
    public void setLastOp(byte op) {
        this.lastOp = op;
    }

    /**
     * Should be set only after the OP is fully executed.
     */
    public void setPreviouslyExecutedOp(byte op) {
        this.previouslyExecutedOp = op;
    }

    /**
     * Returns the last fully executed OP.
     */
    public byte getPreviouslyExecutedOp() {
        return this.previouslyExecutedOp;
    }

    public void stackPush(byte[] data) {
        stackPush(new DataWord(data));
    }

    public void stackPushZero() {
        stackPush(new DataWord(0));
    }

    public void stackPushOne() {
        DataWord stackWord = new DataWord(1);
        stackPush(stackWord);
    }

    public void stackPush(DataWord stackWord) {
        verifyStackOverflow(0, 1); // Sanity Check
        stack.push(stackWord);
    }

    public Stack getStack() {
        return this.stack;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(DataWord pc) {
        this.setPC(pc.intValue());
    }

    public void setPC(int pc) {
        this.pc = pc;

        if (this.pc >= ops.length) {
            stop();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }

    public void setHReturn(byte[] buff) {
        getResult().setHReturn(buff);
    }

    public void step() {
        setPC(pc + 1);
    }

    public byte[] sweep(int n) {

        if (pc + n > ops.length)
            stop();

        byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
        pc += n;
        if (pc >= ops.length)
            stop();

        return data;
    }

    public DataWord stackPop() {
        return stack.pop();
    }

    /**
     * Verifies that the stack is at least <code>stackSize</code>
     *
     * @param stackSize
     *            int
     * @throws StackTooSmallException
     *             If the stack is smaller than <code>stackSize</code>
     */
    public void verifyStackUnderflow(int stackSize) throws StackTooSmallException {
        if (stack.size() < stackSize) {
            throw ExceptionFactory.tooSmallStack(stackSize, stack.size());
        }
    }

    public void verifyStackOverflow(int argsReqs, int returnReqs) {
        if ((stack.size() - argsReqs + returnReqs) > MAX_STACKSIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
        }
    }

    public int getMemSize() {
        return memory.size();
    }

    public void memorySave(DataWord addrB, DataWord value) {
        memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
    }

    public void memorySaveLimited(int addr, byte[] data, int dataSize) {
        memory.write(addr, data, dataSize, true);
    }

    public void memorySave(int addr, byte[] value) {
        memory.write(addr, value, value.length, false);
    }

    public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue());
        }
    }

    /**
     * Allocates a piece of memory and stores value at given offset address
     *
     * @param addr
     *            is the offset address
     * @param allocSize
     *            size of memory needed to write
     * @param value
     *            the data to write to memory
     */
    public void memorySave(int addr, int allocSize, byte[] value) {
        memory.extendAndWrite(addr, allocSize, value);
    }

    public DataWord memoryLoad(DataWord addr) {
        return memory.readWord(addr.intValue());
    }

    public DataWord memoryLoad(int address) {
        return memory.readWord(address);
    }

    public byte[] memoryChunk(int offset, int size) {
        return memory.read(offset, size);
    }

    /**
     * Allocates extra memory in the program for a specified size, calculated from a
     * given offset
     *
     * @param offset
     *            the memory address offset
     * @param size
     *            the number of bytes to allocate
     */
    public void allocateMemory(int offset, int size) {
        memory.extend(offset, size);
    }

    public void suicide(DataWord beneficiary) {
        byte[] owner = getOwnerAddress().getLast20Bytes();
        byte[] obtainer = beneficiary.getLast20Bytes();
        BigInteger balance = getStorage().getBalance(owner);

        addInternalTx(null, null, owner, obtainer, balance, null, "suicide");

        if (Arrays.equals(owner, obtainer)) {
            // if owner == obtainer just zeroing account according to Yellow Paper
            getStorage().addBalance(owner, balance.negate());
        } else {
            transfer(getStorage(), owner, obtainer, balance);
        }

        getResult().addDeleteAccount(this.getOwnerAddress());
    }

    public Repository getStorage() {
        return this.storage;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        BigInteger endowment = value.value();
        if (isNotCovers(getStorage().getBalance(senderAddress), endowment)) {
            stackPushZero();
            return;
        }

        // [1] FETCH THE CODE FROM THE MEMORY
        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

        if (logger.isInfoEnabled())
            logger.info("creating a new contract inside contract run: [{}]", Hex.encode(senderAddress));

        // actual gas subtract
        DataWord gasLimit = config.getCreateGas(getGas());
        spendGas(gasLimit.longValue(), "internal call");

        // [2] CREATE THE CONTRACT ADDRESS
        byte[] nonce = getStorage().getNonce(senderAddress).toByteArray();
        byte[] newAddress = VMUtils.calcNewAddr(getOwnerAddress().getLast20Bytes(), nonce);

        boolean contractAlreadyExists = getStorage().isExist(newAddress);

        // [3] UPDATE THE NONCE
        // (THIS STAGE IS NOT REVERTED BY ANY EXCEPTION)
        getStorage().increaseNonce(senderAddress);

        Repository track = getStorage().startTracking();

        // In case of hashing collisions, check for any balance before createAccount()
        BigInteger oldBalance = track.getBalance(newAddress);
        track.increaseNonce(newAddress);
        track.addBalance(newAddress, oldBalance);

        // [4] TRANSFER THE BALANCE
        BigInteger newBalance = ZERO;
        track.addBalance(senderAddress, endowment.negate());
        newBalance = track.addBalance(newAddress, endowment);

        // [5] COOK THE INVOKE AND EXECUTE
        InternalTransaction internalTx = addInternalTx(nonce, getGasLimit(), senderAddress, null, endowment,
                programCode, "create");
        ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                this, new DataWord(newAddress), getOwnerAddress(), value, gasLimit,
                newBalance, null, track, this.invoke.getBlockStore(), false);

        ProgramResult result = ProgramResult.createEmpty();

        if (contractAlreadyExists) {
            result.setException(new BytecodeExecutionException(
                    "Trying to create a contract with existing contract address: 0x" + Hex.encode(newAddress)));
        } else if (isNotEmpty(programCode)) {
            VM vm = new VM(config);
            Program program = new Program(programCode, programInvoke, internalTx, config);
            vm.play(program);
            result = program.getResult();

            getResult().merge(result);
        }

        // 4. CREATE THE CONTRACT OUT OF RETURN
        byte[] code = result.getHReturn();

        long storageCost = getLength(code) * config.getGasCost().getCREATE_DATA();
        long afterSpend = programInvoke.getGas().longValue() - storageCost - result.getGasUsed();
        if (afterSpend < 0) {
            if (!config.getConstants().createEmptyContractOnOOG()) {
                result.setException(ExceptionFactory.notEnoughSpendingGas("No gas to return just created contract",
                        storageCost, this));
            } else {
                track.saveCode(newAddress, EMPTY_BYTE_ARRAY);
            }
        } else if (getLength(code) > config.getConstants().getMAX_CONTRACT_SZIE()) {
            result.setException(
                    ExceptionFactory.notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                            storageCost, this));
        } else if (!result.isRevert()) {
            result.spendGas(storageCost);
            track.saveCode(newAddress, code);
        }

        if (result.getException() != null || result.isRevert()) {
            logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                    Hex.encode(newAddress),
                    result.getException());

            internalTx.reject();
            result.rejectInternalTransactions();

            track.rollback();
            stackPushZero();

            if (result.getException() != null) {
                return;
            } else {
                returnDataBuffer = result.getHReturn();
            }
        } else {
            track.commit();

            // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
            stackPush(new DataWord(newAddress));
        }

        // 5. REFUND THE REMAIN GAS
        long refundGas = gasLimit.longValue() - result.getGasUsed();
        if (refundGas > 0) {
            refundGas(refundGas, "remain gas from the internal call");
            if (logger.isInfoEnabled()) {
                logger.info("The remaining gas is refunded, account: [{}], gas: [{}] ",
                        Hex.encode(getOwnerAddress().getLast20Bytes()),
                        refundGas);
            }
        }
    }

    /**
     * That method is for internal code invocations
     * <p/>
     * - Normal calls invoke a specified contract which updates itself - Stateless
     * calls invoke code from another contract, within the context of the caller
     *
     * @param msg
     *            is the message call object
     */
    public void callToAddress(MessageCall msg) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

        // FETCH THE SAVED STORAGE
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] senderAddress = getOwnerAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

        if (logger.isInfoEnabled())
            logger.info(
                    msg.getType().name()
                            + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
                    Hex.encode(contextAddress), msg.getOutDataOffs().longValue(), msg.getOutDataSize().longValue());

        Repository track = getStorage().startTracking();

        // 2.1 PERFORM THE VALUE (endowment) PART
        BigInteger endowment = msg.getEndowment().value();
        BigInteger senderBalance = track.getBalance(senderAddress);
        if (isNotCovers(senderBalance, endowment)) {
            stackPushZero();
            refundGas(msg.getGas().longValue(), "refund gas from message call");
            return;
        }

        // FETCH THE CODE
        byte[] programCode = getStorage().isExist(codeAddress) ? getStorage().getCode(codeAddress) : EMPTY_BYTE_ARRAY;

        BigInteger contextBalance = ZERO;
        track.addBalance(senderAddress, endowment.negate());
        contextBalance = track.addBalance(contextAddress, endowment);

        // CREATE CALL INTERNAL TRANSACTION
        InternalTransaction internalTx = addInternalTx(null, getGasLimit(), senderAddress, contextAddress, endowment,
                data, "call");

        ProgramResult result = null;
        if (isNotEmpty(programCode)) {
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                    this, new DataWord(contextAddress),
                    msg.getType().callIsDelegate() ? getCallerAddress() : getOwnerAddress(),
                    msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment(),
                    msg.getGas(), contextBalance, data, track, this.invoke.getBlockStore(),
                    msg.getType().callIsStatic() || isStaticCall());

            VM vm = new VM(config);
            Program program = new Program(programCode, programInvoke, internalTx, config);
            vm.play(program);
            result = program.getResult();

            getResult().merge(result);

            if (result.getException() != null || result.isRevert()) {
                logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                        Hex.encode(contextAddress),
                        result.getException());

                internalTx.reject();
                result.rejectInternalTransactions();

                track.rollback();
                stackPushZero();

                if (result.getException() != null) {
                    return;
                }
            } else {
                // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
                track.commit();
                stackPushOne();
            }

        } else {
            // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
            track.commit();
            stackPushOne();
        }

        // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
        if (result != null) {
            byte[] buffer = result.getHReturn();
            int offset = msg.getOutDataOffs().intValue();
            int size = msg.getOutDataSize().intValue();

            memorySaveLimited(offset, buffer, size);

            returnDataBuffer = buffer;
        }

        // 5. REFUND THE REMAIN GAS
        if (result != null) {
            BigInteger refundGas = msg.getGas().value().subtract(toBI(result.getGasUsed()));
            if (isPositive(refundGas)) {
                refundGas(refundGas.longValue(), "remaining gas from the internal call");
                if (logger.isInfoEnabled())
                    logger.info("The remaining gas refunded, account: [{}], gas: [{}] ",
                            Hex.encode(senderAddress),
                            refundGas.toString());
            }
        } else {
            refundGas(msg.getGas().longValue(), "remaining gas from the internal call");
        }
    }

    public void spendGas(long gasValue, String cause) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Spent for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
        }

        if (getGasLong() < gasValue) {
            throw ExceptionFactory.notEnoughSpendingGas(cause, gasValue, this);
        }
        getResult().spendGas(gasValue);
    }

    public void spendAllGas() {
        spendGas(getGas().longValue(), "Spending all remaining");
    }

    public void refundGas(long gasValue, String cause) {
        logger.info("[{}] Refund for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
        getResult().refundGas(gasValue);
    }

    public void futureRefundGas(long gasValue) {
        logger.info("Future refund added: [{}]", gasValue);
        getResult().addFutureRefund(gasValue);
    }

    public void resetFutureRefund() {
        getResult().resetFutureRefund();
    }

    public void storageSave(DataWord word1, DataWord word2) {
        storageSave(word1.getData(), word2.getData());
    }

    public void storageSave(byte[] key, byte[] val) {
        DataWord keyWord = new DataWord(key);
        DataWord valWord = new DataWord(val);
        getStorage().addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord);
    }

    public byte[] getCode() {
        return ops;
    }

    public byte[] getCodeAt(DataWord address) {
        byte[] code = invoke.getRepository().getCode(address.getLast20Bytes());
        return nullToEmpty(code);
    }

    public DataWord getOwnerAddress() {
        return invoke.getOwnerAddress().clone();
    }

    public DataWord getBlockHash(int index) {
        return index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().intValue()) - 256
                ? new DataWord(this.invoke.getBlockStore().getBlockHashByNumber(index)).clone()
                : DataWord.ZERO.clone();
    }

    public DataWord getBalance(DataWord address) {
        BigInteger balance = getStorage().getBalance(address.getLast20Bytes());
        return new DataWord(balance.toByteArray());
    }

    public DataWord getOriginAddress() {
        return invoke.getOriginAddress().clone();
    }

    public DataWord getCallerAddress() {
        return invoke.getCallerAddress().clone();
    }

    public DataWord getGasPrice() {
        return invoke.getMinGasPrice().clone();
    }

    public long getGasLong() {
        return invoke.getGasLong() - getResult().getGasUsed();
    }

    public DataWord getGas() {
        return new DataWord(invoke.getGasLong() - getResult().getGasUsed());
    }

    public DataWord getCallValue() {
        return invoke.getCallValue().clone();
    }

    public DataWord getDataSize() {
        return invoke.getDataSize().clone();
    }

    public DataWord getDataValue(DataWord index) {
        return invoke.getDataValue(index);
    }

    public byte[] getDataCopy(DataWord offset, DataWord length) {
        return invoke.getDataCopy(offset, length);
    }

    public DataWord getReturnDataBufferSize() {
        return new DataWord(getReturnDataBufferSizeI());
    }

    private int getReturnDataBufferSizeI() {
        return returnDataBuffer == null ? 0 : returnDataBuffer.length;
    }

    public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
        if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI())
            return null;
        return returnDataBuffer == null ? new byte[0]
                : Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(), off.intValueSafe() + size.intValueSafe());
    }

    public DataWord storageLoad(DataWord key) {
        DataWord ret = getStorage().getStorageValue(getOwnerAddress().getLast20Bytes(), key.clone());
        return ret == null ? null : ret.clone();
    }

    public DataWord getCoinbase() {
        return invoke.getCoinbase().clone();
    }

    public DataWord getTimestamp() {
        return invoke.getTimestamp().clone();
    }

    public DataWord getNumber() {
        return invoke.getNumber().clone();
    }

    public DataWord getDifficulty() {
        return invoke.getDifficulty().clone();
    }

    public DataWord getGasLimit() {
        return invoke.getGaslimit().clone();
    }

    public boolean isStaticCall() {
        return invoke.isStaticCall();
    }

    public ProgramResult getResult() {
        return result;
    }

    public void setRuntimeFailure(RuntimeException e) {
        getResult().setException(e);
    }

    static class ByteCodeIterator {
        byte[] code;
        int pc;

        public ByteCodeIterator(byte[] code) {
            this.code = code;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public int getPC() {
            return pc;
        }

        public OpCode getCurOpcode() {
            return pc < code.length ? OpCode.code(code[pc]) : null;
        }

        public boolean isPush() {
            return getCurOpcode() != null ? getCurOpcode().name().startsWith("PUSH") : false;
        }

        public byte[] getCurOpcodeArg() {
            if (isPush()) {
                int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
                return data;
            } else {
                return new byte[0];
            }
        }

        public boolean next() {
            pc += 1 + getCurOpcodeArg().length;
            return pc < code.length;
        }
    }

    static BitSet buildReachableBytecodesMask(byte[] code) {
        NavigableSet<Integer> gotos = new TreeSet<>();
        ByteCodeIterator it = new ByteCodeIterator(code);
        BitSet ret = new BitSet(code.length);
        int lastPush = 0;
        int lastPushPC = 0;
        do {
            ret.set(it.getPC()); // reachable bytecode
            if (it.isPush()) {
                lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
                lastPushPC = it.getPC();
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
                if (it.getPC() != lastPushPC + 1) {
                    // some PC arithmetic we totally can't deal with
                    // assuming all bytecodes are reachable as a fallback
                    ret.set(0, code.length);
                    return ret;
                }
                int jumpPC = lastPush;
                if (!ret.get(jumpPC)) {
                    // code was not explored yet
                    gotos.add(jumpPC);
                }
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN ||
                    it.getCurOpcode() == OpCode.STOP) {
                if (gotos.isEmpty())
                    break;
                it.setPC(gotos.pollFirst());
            }
        } while (it.next());
        return ret;
    }

    public int verifyJumpDest(DataWord nextPC) {
        if (nextPC.bytesOccupied() > 4) {
            throw ExceptionFactory.badJumpDestination(-1);
        }
        int ret = nextPC.intValue();
        if (!getProgramPrecompile().hasJumpDest(ret)) {
            throw ExceptionFactory.badJumpDestination(ret);
        }
        return ret;
    }

    public void callToPrecompiledAddress(MessageCall msg, PrecompiledContracts.PrecompiledContract contract) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            this.refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        Repository track = getStorage().startTracking();

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

        BigInteger endowment = msg.getEndowment().value();
        BigInteger senderBalance = track.getBalance(senderAddress);
        if (senderBalance.compareTo(endowment) < 0) {
            stackPushZero();
            this.refundGas(msg.getGas().longValue(), "refund gas from message call");
            return;
        }

        byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
                msg.getInDataSize().intValue());

        // Charge for endowment - is not reversible by rollback
        transfer(track, senderAddress, contextAddress, msg.getEndowment().value());

        long requiredGas = contract.getGasForData(data);
        if (requiredGas > msg.getGas().longValue()) {

            this.refundGas(0, "call pre-compiled"); // matches cpp logic
            this.stackPushZero();
            track.rollback();
        } else {

            if (logger.isDebugEnabled())
                logger.debug("Call {}(data = {})", contract.getClass().getSimpleName(), Hex.encode(data));

            Pair<Boolean, byte[]> out = contract.execute(data);

            if (out.getLeft()) { // success
                this.refundGas(msg.getGas().longValue() - requiredGas, "call pre-compiled");
                this.stackPushOne();
                returnDataBuffer = out.getRight();
                track.commit();
            } else {
                // spend all gas on failure, push zero and revert state changes
                this.refundGas(0, "call pre-compiled");
                this.stackPushZero();
                track.rollback();
            }

            this.memorySave(msg.getOutDataOffs().intValue(), msg.getOutDataSize().intValueSafe(), out.getRight());
        }
    }

    @SuppressWarnings("serial")
    public class StackTooLargeException extends BytecodeExecutionException {
        public StackTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * used mostly for testing reasons
     */
    public byte[] getMemory() {
        return memory.read(0, memory.size());
    }

    /**
     * used mostly for testing reasons
     */
    public void initMem(byte[] data) {
        this.memory.write(0, data, data.length, false);
    }

    private static BigInteger toBI(long data) {
        return BigInteger.valueOf(data);
    }

    private static boolean isPositive(BigInteger value) {
        return value.signum() > 0;
    }

    private static boolean isCovers(BigInteger covers, BigInteger value) {
        return !isNotCovers(covers, value);
    }

    private static boolean isNotCovers(BigInteger covers, BigInteger value) {
        return covers.compareTo(value) < 0;
    }

    private static void transfer(Repository repository, byte[] fromAddr, byte[] toAddr, BigInteger value) {
        repository.addBalance(fromAddr, value.negate());
        repository.addBalance(toAddr, value);
    }
}
