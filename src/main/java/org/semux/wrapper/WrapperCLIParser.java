/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.wrapper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WrapperCLIParser {

    final static Logger logger = LoggerFactory.getLogger(WrapperCLIParser.class);

    final Wrapper.Mode mode;

    final String[] jvmOptions;

    final String[] remainingArgs;

    WrapperCLIParser(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(buildOptions(), args, true);

        this.mode = parseMode(commandLine);
        this.jvmOptions = parseJvmOptions(commandLine);
        this.remainingArgs = commandLine.getArgs();
    }

    protected String[] parseJvmOptions(CommandLine commandLine) {
        String jvmOptionsString = "";
        if (commandLine.hasOption("jvmoptions")) {
            jvmOptionsString = commandLine.getOptionValue("jvmoptions").trim();
        } else {
            logger.debug("Specify --jvmoptions for additional JVM options");
        }

        return jvmOptionsString.length() > 0 ? jvmOptionsString.split(" ") : new String[] {};
    }

    protected Wrapper.Mode parseMode(CommandLine commandLine) {
        if (commandLine.hasOption("gui")) {
            return Wrapper.Mode.GUI;
        } else if (commandLine.hasOption("cli")) {
            return Wrapper.Mode.CLI;
        } else {
            throw new WrapperException("Either --gui or --cli has to be specified");
        }
    }

    protected Options buildOptions() {
        Options options = new Options();

        options.addOption(Option.builder().longOpt("jvmoptions").hasArg(true).type(String.class).build());

        OptionGroup modeOption = new OptionGroup();
        modeOption.setRequired(true);
        Option guiMode = Option.builder().longOpt("gui").hasArg(false).build();
        modeOption.addOption(guiMode);
        Option cliMode = Option.builder().longOpt("cli").hasArg(false).build();
        modeOption.addOption(cliMode);
        options.addOptionGroup(modeOption);

        return options;
    }
}
