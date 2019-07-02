/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import static org.semux.core.Amount.neg;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.chainspec.ByzantiumPrecompiledContracts;
import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.semux.core.Amount;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;

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
            return 21000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data, PrecompiledContractContext context) {

            if (context instanceof SemuxContext) {
                SemuxContext semuxContext = (SemuxContext) context;
                DelegateState ds = semuxContext.getDelegateState();
                AccountState as = semuxContext.getAccountState();
                // todo - fill these out from data/ensure right from
                byte[] from = "".getBytes();
                byte[] to = "".getBytes();
                long amt = 1;
                Amount amount = Amount.Unit.NANO_SEM.of(amt);
                Amount available = as.getAccount(from).getAvailable();

                if (amount.lte(available)) {
                    if (ds.vote(from, to, amount)) {
                        as.adjustAvailable(from, neg(amount));
                        as.adjustLocked(from, amount);
                    } else {
                        return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
                    }
                } else {
                    return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
                }

                return Pair.of(true, ArrayUtils.EMPTY_BYTE_ARRAY);
            } else {
                return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
            }
        }
    }

    public static class Unvote extends PrecompiledContract {
        @Override
        public long getGasForData(byte[] data) {
            return 21000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data, PrecompiledContractContext context) {
            if (context instanceof SemuxContext) {
                SemuxContext semuxContext = (SemuxContext) context;

                DelegateState ds = semuxContext.getDelegateState();
                AccountState as = semuxContext.getAccountState();
                // todo - fill these out from data/ensure right from
                byte[] from = "".getBytes();
                byte[] to = "".getBytes();
                long amt = 1;
                Amount amount = Amount.Unit.NANO_SEM.of(amt);
                Amount locked = as.getAccount(from).getLocked();

                if (locked.lt(amount)) {
                    return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
                }

                if (ds.unvote(from, to, amount)) {
                    as.adjustAvailable(from, amount);
                    as.adjustLocked(from, neg(amount));
                } else {
                    return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
                }
                return Pair.of(true, ArrayUtils.EMPTY_BYTE_ARRAY);
            } else {
                return Pair.of(false, ArrayUtils.EMPTY_BYTE_ARRAY);
            }
        }
    }
}
