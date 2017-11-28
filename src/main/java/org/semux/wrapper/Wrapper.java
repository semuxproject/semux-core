/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.semux.cli.SemuxCLI;
import org.semux.gui.SemuxGUI;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * This is a process wrapper mainly created for dynamic allocation of the
 * maximum heap size of Semux JVM. The wrapper dynamically allocates 80% of
 * available physical memory to Semux JVM if -Xmx option is not specified.
 */
public class Wrapper {

    final static Logger logger = LoggerFactory.getLogger(Wrapper.class);

    enum Mode {
        GUI, CLI
    }

    private static Class<?> getModeClass(Mode mode) {
        switch (mode) {
        case CLI:
            return SemuxCLI.class;

        case GUI:
            return SemuxGUI.class;

        default:
            throw new WrapperException("Selected mode is not supported");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        WrapperCLIParser wrapperCLIParser = new WrapperCLIParser(args);
        Wrapper wrapper = new Wrapper();
        String[] jvmOptions = wrapper.allocateHeapSize(wrapperCLIParser.jvmOptions);
        int exitValue = wrapper.exec(wrapperCLIParser.mode, jvmOptions, wrapperCLIParser.remainingArgs);
        SystemUtil.exit(exitValue);
    }

    protected String[] allocateHeapSize(String[] jvmOptions) {
        ArrayList<String> allocatedJvmOptions = new ArrayList<>(asList(jvmOptions));

        // dynamically specify maximum heap size according to available physical memory
        // if Xmx is not specified in jvm-options
        final Pattern xmxPattern = Pattern.compile("^-Xmx");
        if (Stream.of(jvmOptions).noneMatch(s -> xmxPattern.matcher(s).find())) {
            long toAllocateMB = (long) ((double) SystemUtil.getAvailableMemorySize() / 1024 / 1024 * 0.8);
            allocatedJvmOptions.add(String.format("-Xmx%dM", toAllocateMB));
            logger.debug("Allocating {} MB of memory as maximum heap size", toAllocateMB);
        }

        return allocatedJvmOptions.toArray(new String[allocatedJvmOptions.size()]);
    }

    protected int exec(Mode mode, String[] jvmOptions, String[] args) throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Paths.get(javaHome, "bin", "java");
        String[] args1 = ArrayUtils.addAll(
                ArrayUtils.addAll(new String[] { javaBin.toAbsolutePath().toString() }, jvmOptions),
                ArrayUtils.addAll(new String[] { "-cp", "semux.jar", getModeClass(mode).getCanonicalName() }, args));

        return startProcess(args1);
    }

    protected int startProcess(String[] args) throws IOException, InterruptedException {
        logger.debug(String.join(" ", args));

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        processBuilder.inheritIO();

        Process process = processBuilder.start();
        process.waitFor();

        return process.exitValue();
    }
}
