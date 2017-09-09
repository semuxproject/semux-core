package org.semux;

import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class Semux {
    private static final Logger logger = LoggerFactory.getLogger(Semux.class);

    public static void main(String[] args) {
        boolean cli = false;
        for (String arg : args) {
            if ("--cli".equals(arg)) {
                cli = true;
            }
        }

        if (cli) {
            CLI.main(args);
        } else {
            GUI.main(args);
        }

        new Thread(() -> {
            try {
                String domain = "version.semux.org";
                Lookup lookup = new Lookup(domain, Type.TXT);
                lookup.setResolver(new SimpleResolver());
                lookup.setCache(null);
                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    for (Record record : records) {
                        TXTRecord txt = (TXTRecord) record;
                        for (Object str : txt.getStrings()) {
                            String version = str.toString();

                            if (SystemUtil.versionCompare(Config.CLIENT_VERSION, version) < 0) {
                                JOptionPane.showMessageDialog(null, "Your wallet need to be upgraded!");
                                System.exit(-1);
                            }
                        }
                    }
                }
            } catch (TextParseException | UnknownHostException e) {
                logger.debug("Failed to get min client version");
            }
        }).start();
    }
}
