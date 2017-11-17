/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import org.apache.commons.lang3.StringUtils;

/**
 * A pojo for name : address pairs
 *
 */
public class SemuxAddress implements Comparable<SemuxAddress> {

    private String name;
    private String address;

    /**
     * creates a new SemuxAddress
     *
     * @param keyStr
     *            must not be null
     * @param address
     *            must not be null
     */
    public SemuxAddress(String keyStr, String address) {
        if (StringUtils.isEmpty(keyStr))
            throw new IllegalArgumentException("name can't be null");
        if (StringUtils.isEmpty(address))
            throw new IllegalArgumentException("address can't be null");
        this.name = keyStr;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public void setName(String text) {
        name = text;
    }

    public void setAddress(String text) {
        address = text;
    }

    @Override
    public int compareTo(SemuxAddress o) {
        return name.compareTo(o.name);
    }

}
