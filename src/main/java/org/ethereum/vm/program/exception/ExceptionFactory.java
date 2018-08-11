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
package org.ethereum.vm.program.exception;

import java.math.BigInteger;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.util.VMUtils;

public class ExceptionFactory {

    public static OutOfGasException notEnoughOpGas(OpCode op, long opGas, long programGas) {
        return new OutOfGasException("Not enough gas for '%s' operation executing: opGas[%d], programGas[%d];", op,
                opGas, programGas);
    }

    public static OutOfGasException notEnoughOpGas(OpCode op, DataWord opGas, DataWord programGas) {
        return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
    }

    public static OutOfGasException notEnoughOpGas(OpCode op, BigInteger opGas, BigInteger programGas) {
        return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
    }

    public static OutOfGasException notEnoughSpendingGas(String cause, long gasValue, Program program) {
        return new OutOfGasException("Not enough gas for '%s' cause spending: gas[%d], usedGas[%d];",
                cause, gasValue, program.getResult().getGasUsed());
    }

    public static OutOfGasException gasOverflow(BigInteger actualGas, BigInteger gasLimit) {
        return new OutOfGasException("Gas value overflow: actualGas[%d], gasLimit[%d];", actualGas.longValue(),
                gasLimit.longValue());
    }

    public static IllegalOperationException invalidOpCode(byte opCode) {
        return new IllegalOperationException("Invalid operation code: opCode[%s];",
                VMUtils.toHexString(new byte[] { opCode }));
    }

    public static BadJumpDestinationException badJumpDestination(int pc) {
        return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
    }

    public static StackUnderflowException tooSmallStack(int expectedSize, int actualSize) {
        return new StackUnderflowException("Expected stack size %d but actual %d;", expectedSize, actualSize);
    }

    public static StackOverflowException tooLargeStack(int expectedSize, int maxSize) {
        return new StackOverflowException("Expected stack size %d exceeds stack limit %d", expectedSize, maxSize);
    }
}
