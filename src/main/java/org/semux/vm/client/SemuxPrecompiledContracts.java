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
import org.ethereum.vm.chainspec.ConstantinoplePrecompiledContracts;
import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.util.Pair;
import org.semux.core.Amount;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;

public class SemuxPrecompiledContracts extends ConstantinoplePrecompiledContracts {

    private static final Vote vote = new Vote();
    private static final Unvote unvote = new Unvote();

    private static final DataWord voteAddr = DataWord.of(100);
    private static final DataWord unvoteAddr = DataWord.of(101);

    private static final Pair<Boolean, byte[]> success = new Pair<>(true, ArrayUtils.EMPTY_BYTE_ARRAY);
    private static final Pair<Boolean, byte[]> failure = new Pair<>(false, ArrayUtils.EMPTY_BYTE_ARRAY);

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
            if (context.getData().length != 32 + 32) {
                return failure;
            }

            Repository track = context.getTrack();
            if (track instanceof SemuxRepository) {
                SemuxRepository semuxTrack = (SemuxRepository) track;
                AccountState as = semuxTrack.getAccountState();
                DelegateState ds = semuxTrack.getDelegateState();
                byte[] from = context.getCaller();
                byte[] to = Arrays.copyOfRange(context.getData(), 12, 32);
                Amount value = weiToAmount(new BigInteger(1, Arrays.copyOfRange(context.getData(), 32, 64)));

                if (as.getAccount(from).getAvailable().gte(value)
                        && ds.vote(from, to, value)) {
                    as.adjustAvailable(from, Amount.neg(value));
                    as.adjustLocked(from, value);
                    return success;
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
                AccountState as = semuxTrack.getAccountState();
                DelegateState ds = semuxTrack.getDelegateState();
                byte[] from = context.getCaller();
                byte[] to = Arrays.copyOfRange(context.getData(), 12, 32);
                Amount value = weiToAmount(new BigInteger(1, Arrays.copyOfRange(context.getData(), 32, 64)));

                if (as.getAccount(from).getLocked().gte(value)
                        && ds.unvote(from, to, value)) {
                    as.adjustAvailable(from, value);
                    as.adjustLocked(from, Amount.neg(value));
                    return success;
                }
            }

            return failure;
        }
    }
}
