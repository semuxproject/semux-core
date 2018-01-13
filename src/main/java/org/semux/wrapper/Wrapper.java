/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.semux.cli.SemuxCli;
import org.semux.gui.SemuxGui;
import org.semux.util.SystemUtil;

/**
 * This is a process wrapper mainly created for dynamic allocation of the
 * maximum heap size of JVM. The wrapper sets the -Xmx to 80% of available
 * physical memory if the -Xmx option is not specified.
 */
public class Wrapper {

    public static final long MINIMUM_HEAP_SIZE_MB = 512L;

    enum Mode {
        GUI, CLI
    }

    protected static Class<?> getModeClass(Mode mode) {
        switch (mode) {
        case CLI:
            return SemuxCli.class;

        case GUI:
            return SemuxGui.class;

        default:
            throw new WrapperException("Selected mode is not supported");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {

        WrapperCLIParser wrapperCLIParser = new WrapperCLIParser(args);
        Wrapper wrapper = new Wrapper();
        String[] jvmOptions = wrapper.addDefaultJvmOptions(wrapperCLIParser.jvmOptions);
        int exitValue = wrapper.exec(wrapperCLIParser.mode, jvmOptions, wrapperCLIParser.remainingArgs);
        SystemUtil.exit(exitValue);
    }

    protected String[] addDefaultJvmOptions(final String[] jvmOptions) {
        ArrayList<String> allocatedJvmOptions = new ArrayList<>(asList(jvmOptions));

        // add non-existing options
        getDefaultJvmOptionSuppliers()
                .stream()
                .sequential()
                .filter(e -> Stream.of(jvmOptions).noneMatch(s -> e.getKey().matcher(s).find()))
                .forEachOrdered(e -> allocatedJvmOptions.add(e.getValue().get()));

        return allocatedJvmOptions.toArray(new String[allocatedJvmOptions.size()]);
    }

    private static List<ImmutablePair<Pattern, Supplier<String>>> getDefaultJvmOptionSuppliers() {
        return Arrays.asList(
                // dynamically specify maximum heap size according to available physical memory
                // if Xmx is not specified in jvmoptions
                ImmutablePair.of(
                        Pattern.compile("^-Xmx"),
                        () -> String.format("-Xmx%dM",
                                Math.max(SystemUtil.getAvailableMemorySize() / 1024 / 1024 * 8 / 10,
                                        MINIMUM_HEAP_SIZE_MB))),

                // Log4j2 default options
                ImmutablePair.of(
                        Pattern.compile("^-Dlog4j2\\.garbagefreeThreadContextMap"),
                        () -> "-Dlog4j2.garbagefreeThreadContextMap=true"),
                ImmutablePair.of(
                        Pattern.compile("^-Dlog4j2\\.shutdownHookEnabled"),
                        () -> "-Dlog4j2.shutdownHookEnabled=false"),
                ImmutablePair.of(
                        Pattern.compile("^-Dlog4j2\\.disableJmx"),
                        () -> "-Dlog4j2.disableJmx=true"));
    }

    protected int exec(Mode mode, String[] jvmOptions, String[] args) throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Paths.get(javaHome, "bin", "java");

        String classpath = System.getProperty("java.class.path");

        String[] newArgs = ArrayUtils.addAll(
                ArrayUtils.addAll(new String[] { javaBin.toAbsolutePath().toString(), "-cp", classpath }, jvmOptions),
                ArrayUtils.addAll(new String[] { getModeClass(mode).getCanonicalName() }, args));

        return startProcess(newArgs);
    }

    protected int startProcess(String[] args) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        processBuilder.inheritIO();

        Process process = processBuilder.start();
        process.waitFor();

        return process.exitValue();
    }
}
