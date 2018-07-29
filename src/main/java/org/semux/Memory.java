/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import org.semux.util.SystemUtil;

public class Memory {
    public static void main(String[] args) {
        System.out.println(SystemUtil.getAvailableMemorySize());
    }
}
