/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.util.Objects;

public class ComboBoxItem<T> implements Comparable<ComboBoxItem<T>> {
    private final String displayName;
    private final T value;

    public ComboBoxItem(String displayName, T value) {
        this.displayName = displayName;
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ComboBoxItem<?> that = (ComboBoxItem<?>) o;

        return Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return displayName != null ? displayName.hashCode() : 0;
    }

    @Override
    public int compareTo(ComboBoxItem<T> o) {
        return displayName.compareTo(o.displayName);
    }
}
