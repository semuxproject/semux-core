/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;

public interface Delegate extends Comparable<Delegate> {

    byte[] getAbyte();

    byte[] getAddress();

    String getAddressString();

    byte[] getName();

    String getNameString();

    long getRegisteredAt();

    Amount getVotes();

    void setVotes(Amount votes);
}
