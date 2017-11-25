/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.semux.vm.Opcode;
import org.semux.vm.SemuxRuntimeMock;
import org.semux.vm.SemuxVM;
import org.semux.vm.VMTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VMPerformance {
    private static Logger logger = LoggerFactory.getLogger(VMPerformance.class);

    private static byte[] ops = new byte[5 * 1024];
    private static long gas = 10 * 1024;
    private static int repeat = 1000;

    private static SemuxVM vm = new SemuxVM();

    public static void setup() {
        Random r = new Random();

        for (int i = 0; i < ops.length; i++) {
            Opcode op;
            do {
                op = Opcode.of(r.nextInt(256));
            } while (op == null || op == Opcode.STOP || op.getRequires() != 0);

            ops[i] = op.toByte();
        }

        // warm up
        SemuxRuntimeMock rt = new SemuxRuntimeMock();
        VMTask task = new VMTask(rt, ops, gas);
        vm.run(Collections.singletonList(task), 1);
    }

    public static void testBench(int nThreads) {
        long t1 = System.nanoTime();
        List<VMTask> tasks = new ArrayList<>();
        for (int i = 0; i < repeat; i++) {
            SemuxRuntimeMock rt = new SemuxRuntimeMock();
            VMTask task = new VMTask(rt, ops, gas);
            tasks.add(task);
        }
        vm.run(tasks, nThreads);
        long t2 = System.nanoTime();

        logger.info("Perf_vm_{}: {} μs/time", nThreads, (t2 - t1) / 1_000 / repeat);
    }

    public static void main(String[] args) throws Exception {
        setup();

        testBench(1);
        testBench(2);
        testBench(4);
    }
}
