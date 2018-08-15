/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm;

import java.math.BigInteger;
import java.util.Arrays;

import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvoke;
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
