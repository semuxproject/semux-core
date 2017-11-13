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
