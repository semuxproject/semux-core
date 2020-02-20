/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import org.apache.commons.lang3.StringUtils;

/**
 */
public class AddressBookEntry implements Comparable<AddressBookEntry> {
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
    public AddressBookEntry(String name, String address) {
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
    public int compareTo(AddressBookEntry o) {
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
        if (obj instanceof AddressBookEntry) {
            AddressBookEntry e = (AddressBookEntry) obj;
            return name.equals(e.getName()) && address.equals(e.getAddress());
        }
        return false;
    }
}