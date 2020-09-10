/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable set of capabilities. This set is sorted to guarantee
 * deterministic encoding.
 */
public class CapabilityTreeSet {

    private final TreeSet<Capability> capabilities;

    private CapabilityTreeSet(Collection<Capability> capabilities) {
        this.capabilities = new TreeSet<>(capabilities);
    }

    /**
     * Creates an empty set.
     */
    public static CapabilityTreeSet emptyList() {
        return new CapabilityTreeSet(Collections.emptyList());
    }

    /**
     * Converts an array of capability into capability set.
     *
     * @param capabilities
     *            the specified capabilities
     */
    public static CapabilityTreeSet of(Capability... capabilities) {
        return new CapabilityTreeSet(Stream.of(capabilities).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * Converts an array of capability into capability set.
     *
     * @param capabilities
     *            the specified capabilities
     * @ImplNode unknown capabilities are ignored
     */
    public static CapabilityTreeSet of(String... capabilities) {
        return new CapabilityTreeSet(
                Stream.of(capabilities).map(Capability::of).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * Checks whether the capability is supported by the ${@link CapabilityTreeSet}.
     *
     * @param capability
     *            the capability to be checked.
     * @return true if the capability is supported, false if not
     */
    public boolean isSupported(Capability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Returns the size of the capability set.
     */
    public int size() {
        return capabilities.size();
    }

    /**
     * Converts the capability set to an list of String.
     */
    public List<String> toList() {
        return capabilities.stream().map(Capability::name).collect(Collectors.toList());
    }

    /**
     * Converts the capability set to an array of String.
     */
    public String[] toArray() {
        return capabilities.stream().map(Capability::name).toArray(String[]::new);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof CapabilityTreeSet
                && Arrays.equals(toArray(), ((CapabilityTreeSet) object).toArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toArray());
    }
}
