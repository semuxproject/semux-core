/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ArrayUtils;
import org.semux.cli.SemuxCLI;
import org.semux.gui.SemuxGUI;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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

    final static HashMap<Mode, Class> modeClassMap = new HashMap<Mode, Class>() {
        {
            put(Mode.GUI, SemuxGUI.class);
            put(Mode.CLI, SemuxCLI.class);
        }
    };

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        Wrapper wrapper = new Wrapper();
        int exitValue = wrapper.parseAndExecute(args);
        SystemUtil.exit(exitValue);
    }

    public int parseAndExecute(String[] args) throws IOException, InterruptedException, ParseException {
        Options options = new Options();

        Option jvmOptions = Option.builder().longOpt("jvm-options").hasArg(true).type(String.class).build();
        options.addOption(jvmOptions);

        OptionGroup modeOption = new OptionGroup();
        modeOption.setRequired(true);
        Option guiMode = Option.builder().longOpt("gui").hasArg(false).build();
        modeOption.addOption(guiMode);
        Option cliMode = Option.builder().longOpt("cli").hasArg(false).build();
        modeOption.addOption(cliMode);
        options.addOptionGroup(modeOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args, true);

        String jvmOptionsString = "";
        if (commandLine.hasOption("jvm-options")) {
            jvmOptionsString = (String) commandLine.getParsedOptionValue("jvm-options");
        }

        // dynamically specify maximum heap size according to available physical memory
        // if Xmx is not specified in jvm-options
        if (!jvmOptionsString.matches(".*-Xmx.*")) {
            long toAllocateMB = getAvailableMemoryInMB();
            jvmOptionsString += " " + String.format("-Xmx%dM", toAllocateMB);
            jvmOptionsString = jvmOptionsString.trim();

            logger.info("Allocating {} MB of memory as maximum heap size", toAllocateMB);
        }

        Mode mode;
        if (commandLine.hasOption("gui")) {
            mode = Mode.GUI;
        } else if (commandLine.hasOption("cli")) {
            mode = Mode.CLI;
        } else {
            throw new RuntimeException("Either --gui or --cli has to be specified");
        }

        return exec(mode, jvmOptionsString, commandLine.getArgs());
    }

    protected int exec(Mode mode, String jvmOptions, String[] args) throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Paths.get(javaHome, "bin", "java");
        String[] args1 = ArrayUtils.addAll(
                ArrayUtils.addAll(new String[] { javaBin.toAbsolutePath().toString() }, jvmOptions.split(" ")),
                ArrayUtils.addAll(new String[] { "-cp", "semux.jar", modeClassMap.get(mode).getCanonicalName() },
                        args));

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(args1).inheritIO();

        logger.info(String.join(" ", builder.command()));

        Process process = builder.start();
        process.waitFor();

        return process.exitValue();
    }

    protected int getAvailableMemoryInMB() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return (int) (os.getTotalPhysicalMemorySize() / 1024 / 1024 * 0.8);
    }
}
