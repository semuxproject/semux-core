package org.semux.core;

import java.math.BigInteger;

import org.semux.core.state.AccountStateV2;
import org.semux.util.Bytes;
import org.semux.util.SimpleEncoder;

public class CompactTransactionEncoder extends TransactionEncoderV1 {

    AccountStateV2 accountState;

    public CompactTransactionEncoder(AccountStateV2 accountState) {
        this.accountState = accountState;
    }

    @Override
    public byte[] encode(Transaction transaction) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(transaction.getHash());
        enc.writeBytes(transaction.getEncoded());
        BigInteger accountIndex = accountState.getAccountIndex(transaction.getSignature().getAddress());
        assert (accountIndex != null && accountIndex.compareTo(BigInteger.ZERO) >= 0);
        enc.writeBytes(Bytes.of(accountIndex));
        enc.writeBytes(transaction.getSignature().getS());
        return enc.toBytes();
    }
}
