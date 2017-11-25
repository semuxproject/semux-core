/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.semux.Config;
import org.semux.util.Bytes;
import org.semux.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressBook {

    private static Logger logger = LoggerFactory.getLogger(AddressBook.class);

    private static final String FILENAME = "addressbook.json";

    // NOTE: A better solution would be storing as a list of entries, with index on
    // name and address.

    private File file;
    private JSONObject database;

    /**
     * Creates an address book instance.
     */
    public AddressBook() {
        this.file = new File(Config.DATA_DIR, FILENAME);
        this.database = load();
    }

    /**
     * Adds or updates a address
     *
     * @param name
     * @param address
     */
    public void put(String name, String address) {
        put(new Entry(name, address));
    }

    /**
     * Adds or update an entry to the address book
     *
     * @param address
     */
    public void put(Entry address) {
        database.put(address.getName(), address.getAddress());
        persist();
    }

    /**
     * Returns all entries from the address book
     *
     * @return List of entries
     */
    public List<Entry> list() {
        List<Entry> list = new ArrayList<>();
        for (String name : database.keySet()) {
            list.add(new Entry(name, database.getString(name)));
        }
        return list;
    }

    /**
     * Returns a {@link Entry} by name.
     *
     * @param name
     * @return An {@link Entry} if exists, otherwise null
     */
    public Entry getByName(String name) {
        return database.has(name) ? new Entry(name, database.getString(name)) : null;
    }

    /**
     * Returns an {@link Entry} by address.
     * 
     * @param address
     * @return An {@link Entry} if exists, otherwise null
     */
    public Entry getByAddress(String address) {
        throw new UnsupportedOperationException("Query by address is not yet supported");
    }

    /**
     * Removes an entry from the address book
     *
     * @param name
     */
    public void remove(String name) {
        if (database.remove(name) != null) {
            persist();
        }
    }

    /**
     * Clears all entries in the address book.
     */
    public void clear() {
        database = new JSONObject();
        persist();
    }

    /**
     * Loads database from file.
     * 
     * @return
     */
    private JSONObject load() {
        try {
            if (file.exists()) {
                String json = IOUtil.readFileAsString(file);
                return new JSONObject(json);
            }
        } catch (IOException e) {
            logger.error("Failed to retrieve or accesss address book", e);
        }
        return new JSONObject();
    }

    /**
     * Persists database to file
     */
    private void persist() {
        try {
            IOUtil.writeToFile(Bytes.of(database.toString()), file);
        } catch (IOException e) {
            logger.error("Failed to retrieve or access address book", e);
        }
    }

    /**
     * An immutable address book entry.
     */
    public static class Entry implements Comparable<Entry> {
        private String name;
        private String address;

        /**
         * creates a new Entry
         *
         * @param name
         *            must not be null
         * @param address
         *            must not be null
         */
        public Entry(String name, String address) {
            if (StringUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name can't be null or empty");
            }
            if (StringUtils.isEmpty(address)) {
                throw new IllegalArgumentException("Address can't be null or empty");
            }
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        @Override
        public int compareTo(Entry o) {
            return name.compareTo(o.name);
        }
    }
}
