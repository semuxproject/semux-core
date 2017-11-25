/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Semux virtual machine.
 *
 */
public class SemuxVM {

    private static final Logger logger = LoggerFactory.getLogger(SemuxVM.class);

    private static ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "vm-" + cnt.getAndIncrement());
        }
    };

    /**
     * Create a VM instance.
     */
    public SemuxVM() {
    }

    /**
     * Run a list of tasks in parallel.
     * 
     * @param tasks
     *            the tasks
     * @param nThreads
     *            number of threads
     */
    public void run(List<VMTask> tasks, int nThreads) {
        // [1] split tasks
        Map<ByteArray, List<VMTask>> queues = new HashMap<>();
        for (VMTask task : tasks) {
            ByteArray key = ByteArray.of(task.getRuntime().getAddress());
            queues.computeIfAbsent(key, k -> new LinkedList<>()).add(task);
        }

        // [2] create executor
        ExecutorService exec = Executors.newFixedThreadPool(nThreads, factory);

        // [3] submit tasks
        List<Future<?>> futures = new ArrayList<>();
        for (ByteArray k : queues.keySet()) {
            futures.add(exec.submit(new Worker(queues.get(k))));
        }

        try {
            // [4] wait until done
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    logger.warn("Exception in VM", e);
                }
            }

            // [5] stop executor
            exec.shutdown();
            while (!exec.awaitTermination(20, TimeUnit.SECONDS)) {
                logger.info("VM is still running, wait for another 20s");
            }

        } catch (InterruptedException e) {
            logger.warn("VM got interrupted");
        }
    }

    private static class Worker implements Runnable {
        private List<VMTask> tasks;

        public Worker(List<VMTask> queue) {
            this.tasks = queue;
        }

        @Override
        public void run() {
            for (VMTask task : tasks) {
                SemuxProcess proc = new SemuxProcess(task.getRuntime(), task.getCode(), task.getLimit());
                proc.run();
                task.setDone(true);
            }
        }
    }
}
