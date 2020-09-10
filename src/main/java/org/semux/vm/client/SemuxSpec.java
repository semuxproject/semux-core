/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.chainspec.ConstantinopleSpec;
import org.ethereum.vm.chainspec.PrecompiledContracts;

public class SemuxSpec extends ConstantinopleSpec {

    private static final PrecompiledContracts precompiledContracts = new SemuxPrecompiledContracts();

    @Override
    public PrecompiledContracts getPrecompiledContracts() {
        return precompiledContracts;
    }
}
