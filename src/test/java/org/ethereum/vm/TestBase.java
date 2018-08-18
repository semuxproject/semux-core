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

import java.math.BigInteger;
import java.util.Arrays;

import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.BlockStoreMockImpl;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.RepositoryMockImpl;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvokeImpl;
import org.ethereum.vm.util.ByteArrayUtil;
import org.junit.After;
import org.junit.Before;

public class TestBase {

    protected byte[] address = address(1);
    protected byte[] origin = address(2);
    protected byte[] caller = address(2);
    protected long gas = 1_000_00L;
    protected BigInteger gasPrice = BigInteger.ONE;
    protected BigInteger value = BigInteger.ZERO;
    protected byte[] data = new byte[0];

    protected byte[] prevHash = ByteArrayUtil.random(32);
    protected byte[] coinbase = address(3);
    protected long timestamp = System.currentTimeMillis();
    protected long number = 1;
    protected BigInteger difficulty = BigInteger.TEN;
    protected long gasLimit = 10_000_000L;

    protected int callDepth = 0;
    protected boolean isStaticCall = false;

    protected Repository repository;
    protected BlockStore blockStore;

    protected ProgramInvokeImpl invoke;
    protected Program program;

    @Before
    public void setup() {
        this.repository = new RepositoryMockImpl();
        this.blockStore = new BlockStoreMockImpl();

        this.invoke = new ProgramInvokeImpl(
                new DataWord(address),
                new DataWord(origin),
                new DataWord(caller),
                new DataWord(gas),
                new DataWord(gasPrice),
                new DataWord(value),
                data,
                new DataWord(prevHash),
                new DataWord(coinbase),
                new DataWord(timestamp),
                new DataWord(number),
                new DataWord(difficulty),
                new DataWord(gasLimit),
                repository,
                blockStore,
                callDepth,
                isStaticCall);
    }

    @After
    public void tearDown() {
    }

    public byte[] address(int n) {
        byte[] a = new byte[20];
        Arrays.fill(a, (byte) n);
        return a;
    }
}
