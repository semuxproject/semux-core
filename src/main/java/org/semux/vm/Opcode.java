/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

public enum Opcode {

    // ====================================
    // Stop and arithmetic
    // ====================================

    /**
     * (0x00) stop the program.
     */
    STOP(0x00, 0, 0),

    /**
     * (0x01) add.
     */
    ADD(0x01, 2, 1),

    /**
     * (0x02) subtract.
     */
    SUB(0x02, 2, 1),

    /**
     * (0x03) multiply.
     */
    MUL(0x03, 2, 1),

    /**
     * (0x04) divide.
     */
    DIV(0x04, 2, 1),

    /**
     * (0x05) modulus operation.
     */
    MOD(0x05, 2, 1),

    /**
     * (0x06) exponentiation.
     */
    EXP(0x06, 2, 1),

    // ====================================
    // Comparison & bitwise logic
    // ====================================

    /**
     * (0x10) less than.
     */
    LT(0x10, 2, 1),

    /**
     * (0x11) greater than.
     */
    GT(0x11, 2, 1),

    /**
     * (0x12) is equal.
     */
    EQ(0x12, 2, 1),

    /**
     * (0x13) is zero.
     */
    ISZERO(0x13, 1, 1),

    /**
     * (0x14) bitwise AND.
     */
    AND(0x14, 2, 1),

    /**
     * (0x15) bitwise OR.
     */
    OR(0x15, 2, 1),

    /**
     * (0x16) bitwise XOR.
     */
    XOR(0x16, 2, 1),

    /**
     * (0x17) bitwise NOT.
     */
    NOT(0x17, 1, 1),

    /**
     * (0x18) retrieve one byte form a data word.
     */
    BYTE(0x18, 2, 1),

    // ====================================
    // Hash
    // ====================================

    /**
     * (0x20) compute the 256-bit hash.
     */
    H256(0x20, 2, 4),

    /**
     * (0x21) compute the 160-bit hash.
     */
    H160(0x21, 2, 3),

    // ====================================
    // Environmental information
    // ====================================

    /**
     * (0x30) the address of the account.
     */
    ADDRESS(0x30, 0, 3),

    /**
     * (0x31) the available balance of the account.
     */
    AVAILABLE(0x31, 0, 1),

    /**
     * (0x32) the address of the caller.
     */
    CALLER(0x32, 0, 3),

    /**
     * (0x33) the value of the transaction.
     */
    CALLVALUE(0x33, 0, 1),

    /**
     * (0x34) the size of call data.
     */
    CALLDATASIZE(0x34, 0, 1),

    /**
     * (0x35) load call data to stack.
     */
    CALLDATALOAD(0x35, 1, 1),

    /**
     * (0x36) copy call data to memory.
     */
    CALLDATACOPY(0x36, 2, 0),

    // ====================================
    // Block information
    // ====================================

    /**
     * (0x40) last block hash.
     */
    BLOCKHASH(0x40, 0, 4),

    /**
     * (0x41) the coinbase of last block.
     */
    COINBASE(0x41, 0, 3),

    /**
     * (0x42) the time stamp of last block.
     */
    TIMESTAMP(0x42, 0, 1),

    /**
     * (0x43) the number of last block.
     */
    NUMBER(0x43, 0, 1),

    // ====================================
    // Stack, memory, storage and flow
    // ====================================

    /**
     * (0x50) remove the top item from the stack.
     */
    POP(0x50, 1, 0),

    /**
     * (0x51) load one word from memory.
     */
    MLOAD(0x51, 1, 1),

    /**
     * (0x52) save one word to memory.
     */
    MSTORE(0x52, 2, 0),

    /**
     * (0x53) load one word from storage.
     */
    SLOAD(0x53, 1, 1),

    /**
     * (0x54) save one word to storage.
     */
    SSTORE(0x54, 2, 0),

    /**
     * (0x55) tag a jump destination.
     */
    JUMPDEST(0x55, 0, 0),

    /**
     * (0x56) jump to a destination.
     */
    JUMP(0x56, 0, 0),

    /**
     * (0x57) jump to a destination if the top stack element is non-zero.
     */
    JUMPI(0x57, 1, 0),

    /**
     * (0x58) the program counter.
     */
    PC(0x58, 0, 1),

    // ====================================
    // Push
    // ====================================

    /**
     * (0x60) push one byte to stack.
     */
    PUSH1(0x60, 0, 1),

    /**
     * (0x61) push two bytes to stack.
     */
    PUSH2(0x61, 0, 1),

    /**
     * (0x62) push three bytes to stack.
     */
    PUSH3(0x62, 0, 1),

    /**
     * (0x63) push four bytes to stack.
     */
    PUSH4(0x63, 0, 1),

    /**
     * (0x64) push five bytes to stack.
     */
    PUSH5(0x64, 0, 1),

    /**
     * (0x65) push six bytes to stack.
     */
    PUSH6(0x65, 0, 1),

    /**
     * (0x66) push seven bytes to stack.
     */
    PUSH7(0x66, 0, 1),

    /**
     * (0x67) push eight bytes to stack.
     */
    PUSH8(0x67, 0, 1),

    // ====================================
    // Duplicate
    // ====================================

    /**
     * (0x70) duplicate the 1st stack item.
     */
    DUP1(0x70, 1, 1),

    /**
     * (0x71) duplicate the 2nd stack item.
     */
    DUP2(0x71, 2, 1),

    /**
     * (0x72) duplicate the 3rd stack item.
     */
    DUP3(0x72, 3, 1),

    /**
     * (0x73) duplicate the 4th stack item.
     */
    DUP4(0x73, 4, 1),

    /**
     * (0x74) duplicate the 5th stack item.
     */
    DUP5(0x74, 5, 1),

    /**
     * (0x75) duplicate the 6th stack item.
     */
    DUP6(0x75, 6, 1),

    /**
     * (0x76) duplicate the 7th stack item.
     */
    DUP7(0x76, 7, 1),

    /**
     * (0x77) duplicate the 8th stack item.
     */
    DUP8(0x77, 8, 1),

    // ====================================
    // Push
    // ====================================

    /**
     * (0x80) exchange the 1st and 2nd stack items.
     */
    SWAP1(0x80, 2, 0),

    /**
     * (0x81) exchange the 1st and 3rd stack items.
     */
    SWAP2(0x81, 3, 0),

    /**
     * (0x82) exchange the 1st and 4th stack items.
     */
    SWAP3(0x82, 4, 0),

    /**
     * (0x83) exchange the 1st and 5th stack items.
     */
    SWAP4(0x83, 5, 0),

    /**
     * (0x84) exchange the 1st and 6th stack items.
     */
    SWAP5(0x84, 6, 0),

    /**
     * (0x85) exchange the 1st and 7th stack items.
     */
    SWAP6(0x85, 7, 0),

    /**
     * (0x86) exchange the 1st and 8th stack items.
     */
    SWAP7(0x86, 8, 0),

    /**
     * (0x87) exchange the 1st and 2nd stack items.
     */
    SWAP8(0x87, 9, 0),

    // ====================================
    // Blockchain features
    // ====================================

    /**
     * (0x90) send a transaction.
     */
    SEND(0x90, 5, 0),

    /**
     * (0x91) create a log.
     */
    LOG(0x91, 3, 0),

    /**
     * (0x92) return data and stop
     */
    RETURN(0x92, 2, 0);

    private static Opcode[] map = new Opcode[256];

    static {
        for (Opcode op : Opcode.values()) {
            map[op.getCode()] = op;
        }
    }

    public static Opcode of(int code) {
        return map[0xff & code];
    }

    private int code;
    private int requires;
    private int produces;

    Opcode(int code, int requires, int produces) {
        this.code = code;
        this.requires = requires;
        this.produces = produces;
    }

    public int getCode() {
        return code;
    }

    public int getRequires() {
        return requires;
    }

    public int getProduces() {
        return produces;
    }

    public byte toByte() {
        return (byte) code;
    }
}
