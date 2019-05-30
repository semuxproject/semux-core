/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.chainspec.ByzantiumSpec;

public class SemuxSpec extends ByzantiumSpec {

    private static final PrecompiledContracts precompiledContracts = new SemuxPrecompiledContracts();

    @Override
    public PrecompiledContracts precompiledContracts() {
        return precompiledContracts;
    }
}
