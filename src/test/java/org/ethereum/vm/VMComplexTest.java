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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.util.BytecodeCompiler;
import org.ethereum.vm.util.HexUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VMComplexTest extends TestBase {

    private static Logger logger = LoggerFactory.getLogger(VMComplexTest.class);

    @Test // contract call recursive
    public void test1() {

        // a = contract.storage[999]
        // if (a > 0) {
        // contract.storage[999] = a - 1
        // send((tx.gas / 10 * 8), 0x77045e71a7a2c50903d88e564cd72fab11e82051, 0)
        // } else {
        // stop
        // }

        DataWord key1 = new DataWord(999);
        DataWord value1 = new DataWord(3);

        // Set contract into Database
        String address = "77045e71a7a2c50903d88e564cd72fab11e82051";
        String code = "PUSH2 0x03e7 SLOAD PUSH1 0x00 MSTORE PUSH1 0x00 PUSH1 0x00 MLOAD GT ISZERO PUSH4 0x0000004c JUMPI PUSH1 0x01 PUSH1 0x00 MLOAD SUB PUSH2 0x03e7 SSTORE PUSH1 0x00 PUSH1 0x00 PUSH1 0x00 PUSH1 0x00 PUSH1 0x00 PUSH20 0x"
                + address + " PUSH1 0x08 PUSH1 0x0a GAS DIV MUL CALL PUSH4 0x0000004c STOP JUMP JUMPDEST STOP";

        byte[] addressB = HexUtil.fromHexString(address);
        byte[] codeB = BytecodeCompiler.compile(code);

        ProgramInvoke pi = spy(invoke);
        when(pi.getOwnerAddress()).thenReturn(new DataWord(addressB));

        repository.createAccount(addressB);
        repository.saveCode(addressB, codeB);
        repository.putStorageRow(addressB, key1, value1);

        // Play the program
        VM vm = new VM();
        Program program = new Program(codeB, pi);

        try {
            while (!program.isStopped()) {
                vm.step(program);
            }
        } catch (RuntimeException e) {
            program.setRuntimeFailure(e);
        }

        long gasUsed = program.getResult().getGasUsed();
        Exception exception = program.getResult().getException();
        BigInteger balance = repository.getBalance(addressB);

        System.out.println();
        System.out.println("============ Results ============");
        System.out.println("*** Used gas: " + gasUsed);
        System.out.println("*** Exception: " + exception);
        System.out.println("*** Contract Balance: " + balance);

        assertEquals(18223, program.getResult().getGasUsed());
        assertNull(exception);
    }
}
