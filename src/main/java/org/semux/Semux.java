/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import org.semux.cli.SemuxCLI;
import org.semux.gui.SemuxGUI;

public class Semux {
    public static void main(String[] args) {
        boolean cli = false;
        for (String arg : args) {
            if ("--cli".equals(arg)) {
                cli = true;
            }
        }

        if (cli) {
            SemuxCLI.main(args);
        } else {
            SemuxGUI.main(args);
        }
    }
}
