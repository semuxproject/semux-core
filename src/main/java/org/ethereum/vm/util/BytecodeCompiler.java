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
package org.ethereum.vm.util;

import java.util.ArrayList;
import java.util.List;

import org.ethereum.vm.OpCode;

public class BytecodeCompiler {

    public static void main(String[] args) {
        byte[] code = HexUtil.fromHexString(
                "605E60076000396000605f556014600054601e60205463abcddcba6040545b51602001600a5254516040016014525451606001601e5254516080016028525460a052546016604860003960166000f26000603f556103e75660005460005360200235");
        System.out.println(decompile(code));
    }

    public static String decompile(byte[] code) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < code.length; i++) {
            OpCode op = OpCode.code(code[i]);
            if (op == null) {
                sb.append(HexUtil.toHexString(code[i])).append(" ");
                continue;
            }

            sb.append(op.name()).append(" ");

            if (op.val() >= OpCode.PUSH1.val() && op.val() <= OpCode.PUSH32.val()) {
                int n = op.val() - OpCode.PUSH1.val() + 1;
                sb.append("0x");
                for (int j = 0; j < n; j++) {
                    sb.append(HexUtil.toHexString(code[++i]));
                }
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    public static byte[] compile(String code) {
        return compile(code.split("\\s+"));
    }

    private static byte[] compile(String[] tokens) {
        List<Byte> bytecodes = new ArrayList<>();
        int ntokens = tokens.length;

        for (int i = 0; i < ntokens; i++) {
            String token = tokens[i].trim().toUpperCase();

            if (token.isEmpty()) {
                continue;
            }

            if (isHexadecimal(token)) {
                compileHexadecimal(token, bytecodes);
            } else {
                bytecodes.add(OpCode.byteVal(token));
            }
        }

        int nbytes = bytecodes.size();
        byte[] bytes = new byte[nbytes];

        for (int k = 0; k < nbytes; k++)
            bytes[k] = bytecodes.get(k).byteValue();

        return bytes;
    }

    private static boolean isHexadecimal(String token) {
        return token.startsWith("0X");
    }

    private static void compileHexadecimal(String token, List<Byte> bytecodes) {
        byte[] bytes = HexUtil.fromHexString(token.substring(2));

        for (int k = 0; k < bytes.length; k++)
            bytecodes.add(bytes[k]);
    }
}
