/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;

public class SemuxVMTest {

    @Test
    public void testParallel() {
        SemuxRuntimeMock rt1 = new SemuxRuntimeMock();
        rt1.address = Hex.parse("0x1111111111111111111111111111111111111111");
        VMTask task1 = new VMTask(rt1, Bytes.EMPTY_BYTES, 10000);

        SemuxRuntimeMock rt2 = new SemuxRuntimeMock();
        rt2.address = Hex.parse("0x2222222222222222222222222222222222222222");
        VMTask task2 = new VMTask(rt2, Bytes.EMPTY_BYTES, 10000);

        List<VMTask> tasks = Arrays.asList(task1, task2);
        SemuxVM vm = new SemuxVM();
        vm.run(tasks, 2);
    }
}
