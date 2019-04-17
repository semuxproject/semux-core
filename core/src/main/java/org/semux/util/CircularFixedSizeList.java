/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.LinkedList;

public class CircularFixedSizeList<T> {

    private int size;
    int index = 0;
    private LinkedList<T> values = new LinkedList<>();

    public CircularFixedSizeList(int size) {
        super();
        this.size = size;

    }

    /**
     * push to current location in list
     *
     * @param object
     * @return
     */
    public void add(T object) {
        while (values.size() >= size) {
            values.removeLast();
        }
        values.add(index, object);
    }

    public T forward() {
        if (values.isEmpty()) {
            return null;
        }
        index = (index + 1) % values.size();
        return values.get(index);
    }

    public T back() {
        if (values.isEmpty()) {
            return null;
        }
        index = (index - 1) % values.size();
        if (index < 0) {
            index = values.size() - 1;
        }
        return values.get(index);
    }

}
