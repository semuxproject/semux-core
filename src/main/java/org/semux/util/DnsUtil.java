/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class DnsUtil {

    private static final Logger logger = LoggerFactory.getLogger(DnsUtil.class);

    private DnsUtil() {
    }

    /**
     * Returns the TXT records associated with a hostname.
     * 
     * @param hostname
     * @return
     */
    public static List<String> queryTxt(String hostname) {
        List<String> list = new ArrayList<>();

        try {
            Lookup lookup = new Lookup(hostname, Type.TXT);
            lookup.setResolver(new SimpleResolver());
            lookup.setCache(null);
            Record[] records = lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                for (Record record : records) {
                    TXTRecord txt = (TXTRecord) record;
                    for (Object str : txt.getStrings()) {
                        list.add(str.toString());
                    }
                }
            }
        } catch (TextParseException | UnknownHostException e) {
            logger.info("Failed to query TXT record: hostname = {}, error = {}", hostname, e.getMessage());
        }

        return list;
    }
}
