/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.semux.cli.SemuxOption;
import org.semux.message.CliMessages;
import org.semux.util.SystemUtil;

public class LauncherTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testInvalidNetworkOption() throws ParseException {
        Launcher launcher = mock(Launcher.class);

        // mock options
        Options options = new Options();
        Option networkOption = Option.builder()
                .longOpt(SemuxOption.NETWORK.toString()).desc(CliMessages.get("SpecifyNetwork"))
                .hasArg(true).numberOfArgs(1).optionalArg(false).argName("name").type(String.class)
                .build();
        options.addOption(networkOption);

        when(launcher.getOptions()).thenReturn(options);
        when(launcher.parseOptions(any())).thenCallRealMethod();

        exit.expectSystemExitWithStatus(SystemUtil.Code.INVALID_NETWORK_LABEL);
        launcher.parseOptions(new String[] { "--network", "unknown" });
    }
}
