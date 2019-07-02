/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.ByzantiumPrecompiledContracts;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.ethereum.vm.chainspec.Spec;

public class SemuxPrecompiledContracts extends ByzantiumPrecompiledContracts {

    private static final Vote vote = new Vote();
    private static final Unvote unvote = new Unvote();

    private static final DataWord voteAddr = DataWord.of(100);
    private static final DataWord unvoteAddr = DataWord.of(101);

    @Override
    public PrecompiledContract getContractForAddress(DataWord address) {

        if (address.equals(voteAddr)) {
            return vote;
        } else if (address.equals(unvoteAddr)) {
            return unvote;
        }
        return super.getContractForAddress(address);
    }

    public static class Vote extends PrecompiledContract {
        @Override
        public long getGasForData(byte[] data) {
            return 0;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data, PrecompiledContractContext context) {

            if (context instanceof SemuxContext) {

            }
            // todo
            return Pair.of(true, data);
        }
    }

    public static class Unvote extends PrecompiledContract {
        @Override
        public long getGasForData(byte[] data) {
            return 0;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data, PrecompiledContractContext context) {
            if (context instanceof SemuxContext) {

            }
            // todo
            return Pair.of(true, data);
        }
    }
}
