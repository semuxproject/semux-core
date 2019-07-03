/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import static org.semux.vm.client.Conversion.weiToAmount;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.chainspec.ByzantiumPrecompiledContracts;
import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.util.Pair;
import org.semux.core.Amount;
import org.semux.core.state.DelegateState;

public class SemuxPrecompiledContracts extends ByzantiumPrecompiledContracts {

    private static final Vote vote = new Vote();
    private static final Unvote unvote = new Unvote();

    private static final DataWord voteAddr = DataWord.of(100);
    private static final DataWord unvoteAddr = DataWord.of(101);

    private static final Pair<Boolean, byte[]> success = Pair.of(true, ArrayUtils.EMPTY_BYTE_ARRAY);
    private static final Pair<Boolean, byte[]> failure = Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);

    // TODO: add unit test

    @Override
    public PrecompiledContract getContractForAddress(DataWord address) {

        if (address.equals(voteAddr)) {
            return vote;
        } else if (address.equals(unvoteAddr)) {
            return unvote;
        }
        return super.getContractForAddress(address);
    }

    public static class Vote implements PrecompiledContract {
        @Override
        public long getGasForData(byte[] data) {
            return 21000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(PrecompiledContractContext context) {
            if (context.getData().length != 32) {
                return failure;
            }

            // TODO: inside the VM, we should refund when the call is a failure.

            Repository track = context.getTrack();
            if (track instanceof SemuxRepository) {
                SemuxRepository semuxTrack = (SemuxRepository) track;
                DelegateState ds = semuxTrack.getDelegateState();
                byte[] from = context.getCaller();
                Amount value = weiToAmount(context.getValue()); // value passed to this contract
                byte[] to = Arrays.copyOfRange(context.getData(), 12, 32); // last 20 bytes

                if (ds.vote(from, to, value)) {
                    return success;
                } else {
                    return failure;
                }
            }

            return failure;
        }
    }

    public static class Unvote implements PrecompiledContract {
        @Override
        public long getGasForData(byte[] data) {
            return 21000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(PrecompiledContractContext context) {
            if (context.getData().length != 32 + 32) {
                return failure;
            }

            Repository track = context.getTrack();
            if (track instanceof SemuxRepository) {
                SemuxRepository semuxTrack = (SemuxRepository) track;
                DelegateState ds = semuxTrack.getDelegateState();
                byte[] from = context.getCaller();
                Amount value = weiToAmount(new BigInteger(0, Arrays.copyOfRange(context.getData(), 0, 32)));
                byte[] to = Arrays.copyOfRange(context.getData(), 44, 64);

                if (ds.unvote(from, to, value)) {
                    return success;
                } else {
                    return failure;
                }
            }

            return failure;
        }
    }
}
