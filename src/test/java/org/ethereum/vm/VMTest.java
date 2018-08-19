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

import static org.ethereum.vm.util.BytecodeCompiler.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.ethereum.vm.client.Repository;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.exception.BadJumpDestinationException;
import org.ethereum.vm.program.exception.StackUnderflowException;
import org.ethereum.vm.util.HexUtil;
import org.junit.Test;

public class VMTest extends TestBase {
    @Test // PUSH1 OP
    public void testPUSH1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xa0"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000000000A0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH2 OP
    public void testPUSH2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0xa0b0"), invoke);
        String expected = "000000000000000000000000000000000000000000000000000000000000A0B0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH3 OP
    public void testPUSH3() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0xA0B0C0"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000A0B0C0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH4 OP
    public void testPUSH4() {
        VM vm = new VM();
        program = new Program(compile("PUSH4 0xA0B0C0D0"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000A0B0C0D0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH5 OP
    public void testPUSH5() {
        VM vm = new VM();
        program = new Program(compile("PUSH5 0xA0B0C0D0E0"), invoke);
        String expected = "000000000000000000000000000000000000000000000000000000A0B0C0D0E0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH6 OP
    public void testPUSH6() {
        VM vm = new VM();
        program = new Program(compile("PUSH6 0xA0B0C0D0E0F0"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000A0B0C0D0E0F0";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH7 OP
    public void testPUSH7() {
        VM vm = new VM();
        program = new Program(compile("PUSH7 0xA0B0C0D0E0F0A1"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000A0B0C0D0E0F0A1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH8 OP
    public void testPUSH8() {
        VM vm = new VM();
        program = new Program(compile("PUSH8 0xA0B0C0D0E0F0A1B1"), invoke);
        String expected = "000000000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH9 OP
    public void testPUSH9() {
        VM vm = new VM();
        program = new Program(compile("PUSH9 0xA0B0C0D0E0F0A1B1C1"), invoke);
        String expected = "0000000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH10 OP
    public void testPUSH10() {
        VM vm = new VM();
        program = new Program(compile("PUSH10 0xA0B0C0D0E0F0A1B1C1D1"), invoke);
        String expected = "00000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH11 OP
    public void testPUSH11() {
        VM vm = new VM();
        program = new Program(compile("PUSH11 0xA0B0C0D0E0F0A1B1C1D1E1"), invoke);
        String expected = "000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH12 OP
    public void testPUSH12() {
        VM vm = new VM();
        program = new Program(compile("PUSH12 0xA0B0C0D0E0F0A1B1C1D1E1F1"), invoke);
        String expected = "0000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH13 OP
    public void testPUSH13() {
        VM vm = new VM();
        program = new Program(compile("PUSH13 0xA0B0C0D0E0F0A1B1C1D1E1F1A2"), invoke);
        String expected = "00000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH14 OP
    public void testPUSH14() {
        VM vm = new VM();
        program = new Program(compile("PUSH14 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2"), invoke);
        String expected = "000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH15 OP
    public void testPUSH15() {
        VM vm = new VM();
        program = new Program(compile("PUSH15 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2"), invoke);
        String expected = "0000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH16 OP
    public void testPUSH16() {
        VM vm = new VM();
        program = new Program(compile("PUSH16 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2"), invoke);
        String expected = "00000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH17 OP
    public void testPUSH17() {
        VM vm = new VM();
        program = new Program(compile("PUSH17 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2"), invoke);
        String expected = "000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH18 OP
    public void testPUSH18() {
        VM vm = new VM();
        program = new Program(compile("PUSH18 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2"), invoke);
        String expected = "0000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH19 OP
    public void testPUSH19() {
        VM vm = new VM();
        program = new Program(compile("PUSH19 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3"), invoke);
        String expected = "00000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH20 OP
    public void testPUSH20() {
        VM vm = new VM();
        program = new Program(compile("PUSH20 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3"), invoke);
        String expected = "000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH21 OP
    public void testPUSH21() {
        VM vm = new VM();
        program = new Program(compile("PUSH21 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3"), invoke);
        String expected = "0000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH22 OP
    public void testPUSH22() {
        VM vm = new VM();
        program = new Program(compile("PUSH22 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3"), invoke);
        String expected = "00000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH23 OP
    public void testPUSH23() {
        VM vm = new VM();
        program = new Program(compile("PUSH23 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3"), invoke);
        String expected = "000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH24 OP
    public void testPUSH24() {
        VM vm = new VM();
        program = new Program(compile("PUSH24 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3"), invoke);
        String expected = "0000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH25 OP
    public void testPUSH25() {
        VM vm = new VM();
        program = new Program(compile("PUSH25 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4"), invoke);
        String expected = "00000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH26 OP
    public void testPUSH26() {
        VM vm = new VM();
        program = new Program(compile("PUSH26 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4"), invoke);
        String expected = "000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH27 OP
    public void testPUSH27() {
        VM vm = new VM();
        program = new Program(compile("PUSH27 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4"), invoke);
        String expected = "0000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH28 OP
    public void testPUSH28() {
        VM vm = new VM();
        program = new Program(compile("PUSH28 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4"), invoke);
        String expected = "00000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH29 OP
    public void testPUSH29() {
        VM vm = new VM();
        program = new Program(compile("PUSH29 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4"), invoke);
        String expected = "000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH30 OP
    public void testPUSH30() {
        VM vm = new VM();
        program = new Program(compile("PUSH30 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4"), invoke);
        String expected = "0000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH31 OP
    public void testPUSH31() {
        VM vm = new VM();
        program = new Program(compile("PUSH31 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1"),
                invoke);
        String expected = "00A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSH32 OP
    public void testPUSH32() {
        VM vm = new VM();
        program = new Program(compile("PUSH32 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1"),
                invoke);
        String expected = "A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1";

        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSHN OP not enough data
    public void testPUSHN_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0xAA"), invoke);
        String expected = "000000000000000000000000000000000000000000000000000000000000AA00";

        vm.step(program);

        assertTrue(program.isStopped());
        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PUSHN OP not enough data
    public void testPUSHN_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH32 0xAABB"), invoke);
        String expected = "AABB000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);

        assertTrue(program.isStopped());
        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // AND OP
    public void testAND_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x0A PUSH1 0x0A AND"), invoke);
        String expected = "000000000000000000000000000000000000000000000000000000000000000A";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // AND OP
    public void testAND_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xC0 PUSH1 0x0A AND"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = RuntimeException.class) // AND OP mal data
    public void testAND_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xC0 AND"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // OR OP
    public void testOR_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xF0 PUSH1 0x0F OR"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000000000FF";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // OR OP
    public void testOR_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xC3 PUSH1 0x3C OR"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000000000FF";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = RuntimeException.class) // OR OP mal data
    public void testOR_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xC0 OR"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // XOR OP
    public void testXOR_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xFF PUSH1 0xFF XOR"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // XOR OP
    public void testXOR_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x0F PUSH1 0xF0 XOR"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000000000FF";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = RuntimeException.class) // XOR OP mal data
    public void testXOR_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xC0 XOR"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // BYTE OP
    public void testBYTE_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH6 0xAABBCCDDEEFF PUSH1 0x1E BYTE"), invoke);
        String expected = "00000000000000000000000000000000000000000000000000000000000000EE";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // BYTE OP
    public void testBYTE_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH6 0xAABBCCDDEEFF PUSH1 0x20 BYTE"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // BYTE OP
    public void testBYTE_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH6 0xAABBCCDDEE3A PUSH1 0x1F BYTE"), invoke);
        String expected = "000000000000000000000000000000000000000000000000000000000000003A";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // BYTE OP mal data
    public void testBYTE_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH6 0xAABBCCDDEE3A BYTE"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // ISZERO OP
    public void testISZERO_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x00 ISZERO"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // ISZERO OP
    public void testISZERO_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x2A ISZERO"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // ISZERO OP mal data
    public void testISZERO_3() {
        VM vm = new VM();
        program = new Program(compile("ISZERO"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // EQ OP
    public void testEQ_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x2A PUSH1 0x2A EQ"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // EQ OP
    public void testEQ_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x2A3B4C PUSH3 0x2A3B4C EQ"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // EQ OP
    public void testEQ_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x2A3B5C PUSH3 0x2A3B4C EQ"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // EQ OP mal data
    public void testEQ_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x2A3B4C EQ"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // GT OP
    public void testGT_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x02 GT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // GT OP
    public void testGT_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH2 0x0F00 GT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // GT OP
    public void testGT_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH4 0x01020304 PUSH2 0x0F00 GT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // GT OP mal data
    public void testGT_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x2A3B4C GT"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SGT OP
    public void testSGT_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x02 SGT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SGT OP
    public void testSGT_2() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0x000000000000000000000000000000000000000000000000000000000000001E " + // 30
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SGT"),
                invoke);

        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SGT OP
    public void testSGT_3() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF57 " + // -169
                        "SGT"),
                invoke);

        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SGT OP mal
    public void testSGT_4() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SGT"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // LT OP
    public void testLT_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x02 LT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // LT OP
    public void testLT_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH2 0x0F00 LT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // LT OP
    public void testLT_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH4 0x01020304 PUSH2 0x0F00 LT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // LT OP mal data
    public void testLT_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x2A3B4C LT"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SLT OP
    public void testSLT_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x02 SLT"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SLT OP
    public void testSLT_2() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0x000000000000000000000000000000000000000000000000000000000000001E " + // 30
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SLT"),
                invoke);

        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SLT OP
    public void testSLT_3() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF57 " + // -169
                        "SLT"),
                invoke);

        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SLT OP mal
    public void testSLT_4() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SLT"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // NOT OP
    public void testNOT_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 NOT"), invoke);
        String expected = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE";

        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // NOT OP
    public void testNOT_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0xA003 NOT"), invoke);
        String expected = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5FFC";

        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // BNOT OP
    public void testBNOT_4() {
        VM vm = new VM();
        program = new Program(compile("NOT"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // NOT OP test from real failure
    public void testNOT_5() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x00 NOT"), invoke);
        String expected = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";

        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // POP OP
    public void testPOP_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x0000 PUSH1 0x01 PUSH3 0x000002 POP"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // POP OP
    public void testPOP_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x0000 PUSH1 0x01 PUSH3 0x000002 POP POP"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // POP OP mal data
    public void testPOP_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x0000 PUSH1 0x01 PUSH3 0x000002 POP POP POP POP"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // DUP1...DUP16 OP
    public void testDUPS() {
        for (int i = 1; i < 17; i++) {
            testDUPN_1(i);
        }
    }

    /**
     * Generic test function for DUP1-16
     *
     * @param n
     *            in DUPn
     */
    private void testDUPN_1(int n) {
        VM vm = new VM();
        String programCode = "";

        for (int i = 0; i < n; i++) {
            programCode += "PUSH1 0x" + (12 + i) + " ";
        }

        programCode += "DUP" + n;

        program = new Program(compile(programCode), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000000012";
        int expectedLen = n + 1;

        for (int i = 0; i < expectedLen; i++) {
            vm.step(program);
        }

        assertEquals(expectedLen, program.getStack().toArray().length);
        assertEquals(expected, HexUtil.toHexString(program.stackPop().getData()).toUpperCase());
        for (int i = 0; i < expectedLen - 2; i++) {
            assertNotEquals(expected, HexUtil.toHexString(program.stackPop().getData()).toUpperCase());
        }
        assertEquals(expected, HexUtil.toHexString(program.stackPop().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // DUPN OP mal data
    public void testDUPN_2() {
        VM vm = new VM();
        program = new Program(compile("DUP1"), invoke);
        try {
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SWAP1...SWAP16 OP
    public void testSWAPS() {
        for (int i = 1; i < 17; ++i) {
            testSWAPN_1(i);
        }
    }

    /**
     * Generic test function for SWAP1-16
     *
     * @param n
     *            in SWAPn
     */
    private void testSWAPN_1(int n) {
        VM vm = new VM();

        String programCode = "";
        String top = new DataWord(0x10 + n).toString();

        for (int i = n; i > -1; --i) {
            programCode += "PUSH1 0x" + HexUtil.toHexString((byte) (0x10 + i)) + " ";
        }

        programCode += "SWAP" + n;

        program = new Program(compile(programCode), invoke);

        for (int i = 0; i < n + 2; ++i) {
            vm.step(program);
        }

        assertEquals(n + 1, program.getStack().toArray().length);
        assertEquals(top, HexUtil.toHexString(program.stackPop().getData()));
    }

    @Test(expected = StackUnderflowException.class) // SWAPN OP mal data
    public void testSWAPN_2() {
        VM vm = new VM();
        program = new Program(compile("SWAP1"), invoke);

        try {
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MSTORE OP
    public void testMSTORE_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000001234";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test // LOG0 OP
    public void tesLog0() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 LOG0"), invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        List<LogInfo> logInfoList = program.getResult().getLogs();
        LogInfo logInfo = logInfoList.get(0);

        assertEquals(HexUtil.toHexString(address), HexUtil.toHexString(logInfo.getAddress()));
        assertEquals(0, logInfo.getTopics().size());
        assertEquals("0000000000000000000000000000000000000000000000000000000000001234", HexUtil.toHexString(logInfo
                .getData()));
    }

    @Test // LOG1 OP
    public void tesLog1() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH1 0x20 PUSH1 0x00 LOG1"),
                invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        List<LogInfo> logInfoList = program.getResult().getLogs();
        LogInfo logInfo = logInfoList.get(0);

        assertEquals(HexUtil.toHexString(address), HexUtil.toHexString(logInfo.getAddress()));
        assertEquals(1, logInfo.getTopics().size());
        assertEquals("0000000000000000000000000000000000000000000000000000000000001234", HexUtil.toHexString(logInfo
                .getData()));
    }

    @Test // LOG2 OP
    public void tesLog2() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH2 0x6666 PUSH1 0x20 PUSH1 0x00 LOG2"), invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        List<LogInfo> logInfoList = program.getResult().getLogs();
        LogInfo logInfo = logInfoList.get(0);

        assertEquals(HexUtil.toHexString(address), HexUtil.toHexString(logInfo.getAddress()));
        assertEquals(2, logInfo.getTopics().size());
        assertEquals("0000000000000000000000000000000000000000000000000000000000001234", HexUtil.toHexString(logInfo
                .getData()));
    }

    @Test // LOG3 OP
    public void tesLog3() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH2 0x6666 PUSH2 0x3333 PUSH1 0x20 PUSH1 0x00 LOG3"),
                invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        List<LogInfo> logInfoList = program.getResult().getLogs();
        LogInfo logInfo = logInfoList.get(0);

        assertEquals(HexUtil.toHexString(address), HexUtil.toHexString(logInfo.getAddress()));
        assertEquals(3, logInfo.getTopics().size());
        assertEquals("0000000000000000000000000000000000000000000000000000000000001234", HexUtil.toHexString(logInfo
                .getData()));
    }

    @Test // LOG4 OP
    public void tesLog4() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH2 0x6666 PUSH2 0x3333 PUSH2 0x5555 PUSH1 0x20 PUSH1 0x00 LOG4"),
                invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        List<LogInfo> logInfoList = program.getResult().getLogs();
        LogInfo logInfo = logInfoList.get(0);

        assertEquals(HexUtil.toHexString(address), HexUtil.toHexString(logInfo.getAddress()));
        assertEquals(4, logInfo.getTopics().size());
        assertEquals("0000000000000000000000000000000000000000000000000000000000001234", HexUtil.toHexString(logInfo
                .getData()));
    }

    @Test // MSTORE OP
    public void testMSTORE_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x5566 PUSH1 0x20 MSTORE"), invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000001234" +
                "0000000000000000000000000000000000000000000000000000000000005566";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test // MSTORE OP
    public void testMSTORE_3() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x5566 PUSH1 0x20 MSTORE PUSH2 0x8888 PUSH1 0x00 MSTORE"),
                invoke);
        String expected = "0000000000000000000000000000000000000000000000000000000000008888" +
                "0000000000000000000000000000000000000000000000000000000000005566";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test // MSTORE OP
    public void testMSTORE_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0xA0 MSTORE"), invoke);
        String expected = "" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000001234";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test(expected = StackUnderflowException.class) // MSTORE OP
    public void testMSTORE_5() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 MSTORE"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MLOAD OP
    public void testMLOAD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x00 MLOAD"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000";
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // MLOAD OP
    public void testMLOAD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 MLOAD"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000";
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()).toUpperCase());
        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // MLOAD OP
    public void testMLOAD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x20 MLOAD"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000";
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // MLOAD OP
    public void testMLOAD_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x20 MSTORE PUSH1 0x20 MLOAD"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000001234";
        String s_expected = "0000000000000000000000000000000000000000000000000000000000001234";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // MLOAD OP
    public void testMLOAD_5() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x20 MSTORE PUSH1 0x1F MLOAD"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000001234";
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000012";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // MLOAD OP mal data
    public void testMLOAD_6() {
        VM vm = new VM();
        program = new Program(compile("MLOAD"), invoke);
        try {
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MSTORE8 OP
    public void testMSTORE8_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x11 PUSH1 0x00 MSTORE8"), invoke);
        String m_expected = "1100000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test // MSTORE8 OP
    public void testMSTORE8_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0x01 MSTORE8"), invoke);
        String m_expected = "0022000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test // MSTORE8 OP
    public void testMSTORE8_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0x21 MSTORE8"), invoke);
        String m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "0022000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected, HexUtil.toHexString(program.getMemory()));
    }

    @Test(expected = StackUnderflowException.class) // MSTORE8 OP mal
    public void testMSTORE8_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 MSTORE8"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SSTORE OP
    public void testSSTORE_1() {
        VM vm = new VM();

        program = new Program(compile("PUSH1 0x22 PUSH1 0xAA SSTORE"), invoke);
        String s_expected_key = "00000000000000000000000000000000000000000000000000000000000000AA";
        String s_expected_val = "0000000000000000000000000000000000000000000000000000000000000022";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord key = new DataWord(HexUtil.fromHexString(s_expected_key));
        DataWord val = program.getStorage().getStorageRow(invoke.getOwnerAddress().getLast20Bytes(), key);

        assertEquals(s_expected_val, HexUtil.toHexString(val.getData()).toUpperCase());
    }

    @Test // SSTORE OP
    public void testSSTORE_2() {
        VM vm = new VM();

        program = new Program(compile("PUSH1 0x22 PUSH1 0xAA SSTORE PUSH1 0x22 PUSH1 0xBB SSTORE"), invoke);
        String s_expected_key = "00000000000000000000000000000000000000000000000000000000000000BB";
        String s_expected_val = "0000000000000000000000000000000000000000000000000000000000000022";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        Repository repository = program.getStorage();
        DataWord key = new DataWord(HexUtil.fromHexString(s_expected_key));
        DataWord val = repository.getStorageRow(invoke.getOwnerAddress().getLast20Bytes(), key);

        assertEquals(s_expected_val, HexUtil.toHexString(val.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SSTORE OP
    public void testSSTORE_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 SSTORE"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SLOAD OP
    public void testSLOAD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xAA SLOAD"), invoke);
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SLOAD OP
    public void testSLOAD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0xAA SSTORE PUSH1 0xAA SLOAD"), invoke);
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000022";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // SLOAD OP
    public void testSLOAD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0xAA SSTORE PUSH1 0x33 PUSH1 0xCC SSTORE PUSH1 0xCC SLOAD"),
                invoke);
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000033";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SLOAD OP
    public void testSLOAD_4() {
        VM vm = new VM();
        program = new Program(compile("SLOAD"), invoke);
        try {
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // PC OP
    public void testPC_1() {
        VM vm = new VM();
        program = new Program(compile("PC"), invoke);
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // PC OP
    public void testPC_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0xAA MSTORE PUSH1 0xAA SLOAD PC"), invoke);
        String s_expected = "0000000000000000000000000000000000000000000000000000000000000008";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = BadJumpDestinationException.class) // JUMP OP mal data
    public void testJUMP_1() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH1 0xAA PUSH1 0xBB PUSH1 0x0E JUMP PUSH1 0xCC PUSH1 0xDD PUSH1 0xEE JUMPDEST PUSH1 0xFF"),
                invoke);
        String s_expected = "00000000000000000000000000000000000000000000000000000000000000FF";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test(expected = BadJumpDestinationException.class) // JUMP OP mal data
    public void testJUMP_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x0C PUSH1 0x0C SWAP1 JUMP PUSH1 0xCC PUSH1 0xDD PUSH1 0xEE PUSH1 0xFF"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // JUMPI OP
    public void testJUMPI_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x05 JUMPI JUMPDEST PUSH1 0xCC"), invoke);
        String s_expected = "00000000000000000000000000000000000000000000000000000000000000CC";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // JUMPI OP
    public void testJUMPI_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH4 0x00000000 PUSH1 0x44 JUMPI PUSH1 0xCC PUSH1 0xDD"), invoke);
        String s_expected_1 = "00000000000000000000000000000000000000000000000000000000000000DD";
        String s_expected_2 = "00000000000000000000000000000000000000000000000000000000000000CC";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        DataWord item2 = program.stackPop();

        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
        assertEquals(s_expected_2, HexUtil.toHexString(item2.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // JUMPI OP mal
    public void testJUMPI_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 JUMPI"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test(expected = BadJumpDestinationException.class) // JUMPI OP mal
    public void testJUMPI_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 PUSH1 0x22 SWAP1 SWAP1 JUMPI"), invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test(expected = BadJumpDestinationException.class) // JUMP OP mal data
    public void testJUMPDEST_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x23 PUSH1 0x08 JUMP PUSH1 0x01 JUMPDEST PUSH1 0x02 SSTORE"), invoke);

        String s_expected_key = "0000000000000000000000000000000000000000000000000000000000000002";
        String s_expected_val = "0000000000000000000000000000000000000000000000000000000000000023";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord key = new DataWord(HexUtil.fromHexString(s_expected_key));
        DataWord val = program.getStorage().getStorageRow(invoke.getOwnerAddress().getLast20Bytes(), key);

        assertTrue(program.isStopped());
        assertEquals(s_expected_val, HexUtil.toHexString(val.getData()).toUpperCase());
    }

    @Test // JUMPDEST OP for JUMPI
    public void testJUMPDEST_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x23 PUSH1 0x01 PUSH1 0x09 JUMPI PUSH1 0x01 JUMPDEST PUSH1 0x02 SSTORE"),
                invoke);

        String s_expected_key = "0000000000000000000000000000000000000000000000000000000000000002";
        String s_expected_val = "0000000000000000000000000000000000000000000000000000000000000023";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord key = new DataWord(HexUtil.fromHexString(s_expected_key));
        DataWord val = program.getStorage().getStorageRow(invoke.getOwnerAddress().getLast20Bytes(), key);

        assertTrue(program.isStopped());
        assertEquals(s_expected_val, HexUtil.toHexString(val.getData()).toUpperCase());
    }

    @Test // ADD OP mal
    public void testADD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x02 PUSH1 0x02 ADD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000004";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // ADD OP
    public void testADD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1002 PUSH1 0x02 ADD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000001004";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // ADD OP
    public void testADD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1002 PUSH6 0x123456789009 ADD"), invoke);
        String s_expected_1 = "000000000000000000000000000000000000000000000000000012345678A00B";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // ADD OP mal
    public void testADD_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 ADD"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // ADDMOD OP mal
    public void testADDMOD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x02 PUSH1 0x02 PUSH1 0x03 ADDMOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertTrue(program.isStopped());
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // ADDMOD OP
    public void testADDMOD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1000 PUSH1 0x02 PUSH2 0x1002 ADDMOD PUSH1 0x00"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000004";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertFalse(program.isStopped());
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // ADDMOD OP
    public void testADDMOD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1002 PUSH6 0x123456789009 PUSH1 0x02 ADDMOD"), invoke);
        String s_expected_1 = "000000000000000000000000000000000000000000000000000000000000093B";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertTrue(program.isStopped());
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // ADDMOD OP mal
    public void testADDMOD_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 ADDMOD"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MUL OP
    public void testMUL_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x02 MUL"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000006";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MUL OP
    public void testMUL_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x222222 PUSH1 0x03 MUL"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000666666";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MUL OP
    public void testMUL_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x222222 PUSH3 0x333333 MUL"), invoke);
        String s_expected_1 = "000000000000000000000000000000000000000000000000000006D3A05F92C6";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // MUL OP mal
    public void testMUL_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 MUL"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MULMOD OP
    public void testMULMOD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x02 PUSH1 0x04 MULMOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000002";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MULMOD OP
    public void testMULMOD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x222222 PUSH1 0x03 PUSH1 0x04 MULMOD"), invoke);
        String s_expected_1 = "000000000000000000000000000000000000000000000000000000000000000C";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MULMOD OP
    public void testMULMOD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x222222 PUSH3 0x333333 PUSH3 0x444444 MULMOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // MULMOD OP mal
    public void testMULMOD_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x01 MULMOD"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // DIV OP
    public void testDIV_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x02 PUSH1 0x04 DIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000002";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // DIV OP
    public void testDIV_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x33 PUSH1 0x99 DIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000003";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // DIV OP
    public void testDIV_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x22 PUSH1 0x99 DIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000004";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // DIV OP
    public void testDIV_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x15 PUSH1 0x99 DIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000007";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // DIV OP
    public void testDIV_5() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x04 PUSH1 0x07 DIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // DIV OP
    public void testDIV_6() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x07 DIV"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SDIV OP
    public void testSDIV_1() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH2 0x03E8 PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC18 SDIV"),
                invoke);
        String s_expected_1 = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SDIV OP
    public void testSDIV_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xFF PUSH1 0xFF SDIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SDIV OP
    public void testSDIV_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x00 PUSH1 0xFF SDIV"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SDIV OP mal
    public void testSDIV_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0xFF SDIV"), invoke);

        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SUB OP
    public void testSUB_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x04 PUSH1 0x06 SUB"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000002";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SUB OP
    public void testSUB_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x4444 PUSH2 0x6666 SUB"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000002222";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SUB OP
    public void testSUB_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x4444 PUSH4 0x99996666 SUB"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000099992222";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SUB OP mal
    public void testSUB_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH4 0x99996666 SUB"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // MSIZE OP
    public void testMSIZE_1() {
        VM vm = new VM();
        program = new Program(compile("MSIZE"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MSIZE OP
    public void testMSIZE_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x20 PUSH1 0x30 MSTORE MSIZE"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000060";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // STOP OP
    public void testSTOP_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x20 PUSH1 0x30 PUSH1 0x10 PUSH1 0x30 PUSH1 0x11 PUSH1 0x23 STOP"),
                invoke);
        int expectedSteps = 7;

        int i = 0;
        while (!program.isStopped()) {
            vm.step(program);
            ++i;
        }
        assertEquals(expectedSteps, i);
    }

    @Test // EXP OP
    public void testEXP_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x02 EXP"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000008";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();

        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // EXP OP
    public void testEXP_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x00 PUSH3 0x123456 EXP"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();

        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // EXP OP
    public void testEXP_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1122 PUSH1 0x01 EXP"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();

        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // EXP OP mal
    public void testEXP_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH3 0x123456 EXP"), invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // RETURN OP
    public void testRETURN_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 RETURN"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000001234";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected_1, HexUtil.toHexString(program.getResult().getHReturn()).toUpperCase());
        assertTrue(program.isStopped());
    }

    @Test // RETURN OP
    public void testRETURN_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x1F RETURN"), invoke);
        String s_expected_1 = "3400000000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected_1, HexUtil.toHexString(program.getResult().getHReturn()).toUpperCase());
        assertTrue(program.isStopped());
    }

    @Test // RETURN OP
    public void testRETURN_3() {
        VM vm = new VM();
        program = new Program(compile(
                "PUSH32 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 RETURN"),
                invoke);
        String s_expected_1 = "A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected_1, HexUtil.toHexString(program.getResult().getHReturn()).toUpperCase());
        assertTrue(program.isStopped());
    }

    @Test // RETURN OP
    public void testRETURN_4() {
        VM vm = new VM();
        program = new Program(compile(
                "PUSH32 0xA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x10 RETURN"),
                invoke);
        String s_expected_1 = "E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B100000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(s_expected_1, HexUtil.toHexString(program.getResult().getHReturn()).toUpperCase());
        assertTrue(program.isStopped());
    }

    @Test // CODECOPY OP
    public void testCODECOPY_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x07 PUSH1 0x00 CODECOPY SLT CALLVALUE JUMP"), invoke);
        String m_expected_1 = "1234560000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected_1, HexUtil.toHexString(program.getMemory()).toUpperCase());
    }

    @Test // CODECOPY OP
    public void testCODECOPY_2() {
        VM vm = new VM();
        program = new Program(compile(
                "PUSH1 0x5E PUSH1 0x07 PUSH1 0x00 CODECOPY PUSH1 0x00 PUSH1 0x5f SSTORE PUSH1 0x14 PUSH1 0x00 SLOAD PUSH1 0x1e PUSH1 0x20 SLOAD PUSH4 0xabcddcba PUSH1 0x40 SLOAD JUMPDEST MLOAD PUSH1 0x20 ADD PUSH1 0x0a MSTORE SLOAD MLOAD PUSH1 0x40 ADD PUSH1 0x14 MSTORE SLOAD MLOAD PUSH1 0x60 ADD PUSH1 0x1e MSTORE  SLOAD MLOAD PUSH1 0x80 ADD PUSH1 0x28 MSTORE SLOAD PUSH1 0xa0 MSTORE SLOAD PUSH1 0x16 PUSH1 0x48 PUSH1 0x00 CODECOPY PUSH1 0x16 PUSH1 0x00 CALLCODE PUSH1 0x00 PUSH1 0x3f SSTORE PUSH2 0x03e7 JUMP PUSH1 0x00 SLOAD PUSH1 0x00 MSTORE8 PUSH1 0x20 MUL CALLDATALOAD PUSH1 0x20 SLOAD"),
                invoke);
        String m_expected_1 = "6000605F556014600054601E60205463ABCDDCBA6040545B51602001600A5254516040016014525451606001601E5254516080016028525460A052546016604860003960166000F26000603F556103E756600054600053602002356020540000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected_1, HexUtil.toHexString(program.getMemory()).toUpperCase());
    }

    @Test // CODECOPY OP
    public void testCODECOPY_3() {
        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "611234600054615566602054607060006020396000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e756600054600053602002351234"),
                invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertFalse(program.isStopped());
    }

    @Test(expected = StackUnderflowException.class) // CODECOPY OP mal
    public void testCODECOPY_4() {
        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "605E6007396000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e756600054600053602002351234"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    private byte[] testAddress = HexUtil.fromHexString("471FD3AD3E9EEADEEC4608B92D16CE6B500704CC");
    private byte[] testCode = HexUtil.fromHexString("385E60076000396000605f556014600054601e60"
            + "205463abcddcba6040545b51602001600a525451"
            + "6040016014525451606001601e52545160800160"
            + "28525460a052546016604860003960166000f260"
            + "00603f556103e75660005460005360200235");

    @Test // EXTCODESIZE OP
    public void testEXTCODESIZE_1() {
        repository.saveCode(testAddress, testCode);

        VM vm = new VM();
        program = new Program(HexUtil.fromHexString("73471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3B"),
                invoke);
        String s_expected_1 = new DataWord(testCode.length).toString();

        vm.step(program);
        vm.step(program);

        assertEquals(s_expected_1, HexUtil.toHexString(program.getStack().peek().getData()).toUpperCase());
    }

    @Test // EXTCODECOPY OP
    public void testEXTCODECOPY_1() {
        repository.saveCode(testAddress, testCode);

        VM vm = new VM();
        program = new Program(HexUtil.fromHexString("60036007600073471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C123456"),
                invoke);
        String m_expected_1 = "6000600000000000000000000000000000000000000000000000000000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected_1, HexUtil.toHexString(program.getMemory()).toUpperCase());
    }

    @Test // EXTCODECOPY OP
    public void testEXTCODECOPY_2() {
        repository.saveCode(testAddress, testCode);

        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "603E6007600073471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C6000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e75660005460005360200235602054"),
                invoke);
        String m_expected_1 = "6000605F556014600054601E60205463ABCDDCBA6040545B51602001600A5254516040016014525451606001601E5254516080016028525460A0525460160000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected_1, HexUtil.toHexString(program.getMemory()).toUpperCase());
    }

    @Test // EXTCODECOPY OP
    public void testEXTCODECOPY_3() {
        repository.saveCode(testAddress, testCode);

        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "605E6007600073471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C6000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e75660005460005360200235"),
                invoke);

        String m_expected_1 = "6000605F556014600054601E60205463ABCDDCBA6040545B51602001600A5254516040016014525451606001601E5254516080016028525460A052546016604860003960166000F26000603F556103E756600054600053602002350000000000";

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertEquals(m_expected_1, HexUtil.toHexString(program.getMemory()).toUpperCase());
    }

    @Test // EXTCODECOPY OP
    public void testEXTCODECOPY_4() {
        repository.saveCode(testAddress, testCode);

        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "611234600054615566602054603E6000602073471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C6000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e756600054600053602002351234"),
                invoke);

        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);
        vm.step(program);

        assertFalse(program.isStopped());
    }

    @Test(expected = StackUnderflowException.class) // EXTCODECOPY OP mal
    public void testEXTCODECOPY_5() {
        VM vm = new VM();
        program = new Program(HexUtil.fromHexString("605E600773471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // CODESIZE OP
    public void testCODESIZE_1() {
        VM vm = new VM();
        program = new Program(HexUtil.fromHexString(
                "385E60076000396000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e75660005460005360200235"),
                invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000062";

        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MOD OP
    public void testMOD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x04 MOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MOD OP
    public void testMOD_2() {
        VM vm = new VM();
        program = new Program(compile("PUSH2 0x012C PUSH2 0x01F4 MOD"), invoke);
        String s_expected_1 = "00000000000000000000000000000000000000000000000000000000000000C8";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // MOD OP
    public void testMOD_3() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x04 PUSH1 0x02 MOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000002";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // MOD OP mal
    public void testMOD_4() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x04 MOD"), invoke);

        try {
            vm.step(program);
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }

    @Test // SMOD OP
    public void testSMOD_1() {
        VM vm = new VM();
        program = new Program(compile("PUSH1 0x03 PUSH1 0x04 SMOD"), invoke);
        String s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SMOD OP
    public void testSMOD_2() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE2 " + // -30
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SMOD"),
                invoke);
        String s_expected_1 = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEC";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test // SMOD OP
    public void testSMOD_3() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0x000000000000000000000000000000000000000000000000000000000000001E " + // 30
                        "PUSH32 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF56 " + // -170
                        "SMOD"),
                invoke);
        String s_expected_1 = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEC";

        vm.step(program);
        vm.step(program);
        vm.step(program);

        DataWord item1 = program.stackPop();
        assertEquals(s_expected_1, HexUtil.toHexString(item1.getData()).toUpperCase());
    }

    @Test(expected = StackUnderflowException.class) // SMOD OP mal
    public void testSMOD_4() {
        VM vm = new VM();
        program = new Program(
                compile("PUSH32 0x000000000000000000000000000000000000000000000000000000000000001E " + // 30
                        "SMOD"),
                invoke);
        try {
            vm.step(program);
            vm.step(program);
        } finally {
            assertTrue(program.isStopped());
        }
    }
}
