/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressBook {

    private static final Logger logger = LoggerFactory.getLogger(AddressBook.class);

    // NOTE: A better solution would be storing as a list of entries, with index on
    // name and address.

    private final File file;
    private JsonObject database;

    /**
     * Creates an address book instance.
     */
    public AddressBook(File file) {
        this.file = file;
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
        database = Json.createObjectBuilder(database).add(address.getName(), Json.createValue(address.getAddress()))
                .build();
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
        return database.containsKey(name) ? new Entry(name, database.getString(name)) : null;
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
        if (database.containsKey(name)) {
            database = Json.createObjectBuilder(database).remove(name).build();
            persist();
        }
    }

    /**
     * Clears all entries in the address book.
     */
    public void clear() {
        database = Json.createObjectBuilder().build();
        persist();
    }

    /**
     * Loads database from file.
     * 
     * @return
     */
    private JsonObject load() {
        if (file.exists()) {
            try (JsonReader jsonReader = Json.createReader(Files.newBufferedReader(file.toPath()))) {
                return jsonReader.readObject();
            } catch (IOException e) {
                logger.error("Failed to retrieve or access address book", e);
            }
        }
        return Json.createObjectBuilder().build();
    }

    /**
     * Persists database to file
     */
    private void persist() {
        try (JsonWriter jsonWriter = Json.createWriter(Files.newBufferedWriter(file.toPath()))) {
            jsonWriter.write(database);
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
            int c = name.compareTo(o.name);
            return (c == 0) ? address.compareTo(o.address) : c;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((address == null) ? 0 : address.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Entry) {
                Entry e = (Entry) obj;
                return name.equals(e.getName()) && address.equals(e.getAddress());
            }
            return false;
        }
    }
}