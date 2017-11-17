/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressBook {

    private static final String ADDRESSBOOK_FILENAME = "addressbook.txt";
    private static Logger logger = LoggerFactory.getLogger(AddressBook.class);
    private static Path ADDRESSBOOKFILE = Paths.get(".", ADDRESSBOOK_FILENAME);

    /**
     * add or update a address
     *
     * @param name
     *            must not be null
     * @param address
     *            must not be null
     */
    public static void put(String name, String address) {
        put(new SemuxAddress(name, address));
    }

    /**
     * retrieve all addresses from the address book
     *
     * @return List of SemuxAddresses
     */
    public static List<SemuxAddress> getAllAddresses() {
        List<SemuxAddress> adr = getDatabase().entrySet().stream().map(e -> new SemuxAddress(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return adr;
    }

    /**
     * delete the entry from the address book
     *
     * @param key
     */
    public static void delete(String key) {
        if (getDatabase().containsKey(key)) {
            Map<String, String> db = getDatabase();
            db.remove(key);
            persist(db);
        }
    }

    /**
     * insert or update an entry in the address book
     *
     * @param address
     * @throws IllegalAddressException
     */
    public static void put(SemuxAddress address) {
        Map<String, String> db = getDatabase();
        db.put(address.getName(), address.getAddress());
        persist(db);

    }

    /**
     * get {@link SemuxAddress} for name
     *
     * @param selected
     *            Name in address book
     * @return {@link SemuxAddress}
     */
    public static SemuxAddress getAddress(String selected) {
        return getDatabase().get(selected) != null ? new SemuxAddress(selected, getDatabase().get(selected)) : null;
    }

    private static Map<String, String> getDatabase() {
        try {
            if (Files.notExists(ADDRESSBOOKFILE)) {
                Files.createFile(ADDRESSBOOKFILE);
            }
            List<String> lines = Files.readAllLines(ADDRESSBOOKFILE);
            Map<String, String> addresses = new HashMap<>();
            for (String string : lines) {
                String[] addr = string.split(":");
                if ((null != addr) && (addr.length == 2)) {
                    addresses.put(addr[0], addr[1]);
                }
            }
            return addresses;
        } catch (IOException e) {
            logger.error("Failed to retrieve or accesss address book", e);
        }
        return Collections.emptyMap();
    }

    private static void persist(Map<String, String> db) {
        try {
            FileWriter filew = new FileWriter(ADDRESSBOOKFILE.toFile());
            db.forEach((k, v) -> {
                try {
                    filew.write((k + ":" + v + "\n"));
                } catch (IOException e) {
                    logger.error("Failed to retrieve or access address book", e);
                }
            });
            filew.close();
        } catch (IOException e) {
            logger.error("Failed to retrieve or access address book", e);
        }
    }
}
