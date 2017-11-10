package org.semux;

import org.semux.cli.SemuxCLI;

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
            GUI.main(args);
        }
    }
}
