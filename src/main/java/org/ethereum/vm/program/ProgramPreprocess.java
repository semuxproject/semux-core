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

import java.util.HashSet;
import java.util.Set;

import org.ethereum.vm.OpCode;

/**
 * Pre-compile the program code to speed up execution.
 *
 * Features included:
 * <ul>
 * <li>Collect the list of JUMP destinations</li>
 * </ul>
 */
public class ProgramPreprocess {
    private Set<Integer> jumpdest = new HashSet<>();

    public boolean hasJumpDest(int pc) {
        return jumpdest.contains(pc);
    }

    public static ProgramPreprocess compile(byte[] ops) {
        ProgramPreprocess ret = new ProgramPreprocess();

        for (int i = 0; i < ops.length; ++i) {
            OpCode op = OpCode.code(ops[i]);
            if (op == null) {
                continue;
            }

            if (op.equals(OpCode.JUMPDEST)) {
                ret.jumpdest.add(i);
            }

            if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                i += op.asInt() - OpCode.PUSH1.asInt() + 1;
            }
        }

        return ret;
    }
}
