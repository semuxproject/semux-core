/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.transaction;

import org.semux.Kernel;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;

/**
 * This is a builder class for building transactions required by Semux API with
 * provided inputs. The builder expects raw inputs from a HTTP request.
 */
public class TransactionBuilder {

    private Kernel kernel;

    /**
     * Transaction type
     */
    private TransactionType type;

    /**
     * Transaction sender account
     */
    private EdDSA account;

    /**
     * Transaction recipient address
     */
    private byte[] to;

    /**
     * Transaction value
     */
    private long value;

    /**
     * Transaction fee
     */
    private long fee;

    /**
     * Transaction data
     */
    private byte[] data;

    public TransactionBuilder(Kernel kernel) {
        this.kernel = kernel;
    }

    public TransactionBuilder withType(TransactionType type) {
        this.type = type;
        return this;
    }

    public TransactionBuilder withFrom(String pFrom) {
        if (pFrom == null) {
            throw new IllegalArgumentException("parameter 'from' is required");
        }

        try {
            account = kernel.getWallet().getAccount(Hex.parse(pFrom));
        } catch (CryptoException e) {
            throw new IllegalArgumentException("parameter 'from' is not a valid hexadecimal string");
        }

        if (account == null) {
            throw new IllegalArgumentException(
                    String.format("provided address %s doesn't belong to the wallet", pFrom));
        }

        return this;
    }

    public TransactionBuilder withTo(String pTo) {
        if (type == TransactionType.DELEGATE) {
            if (pTo != null) {
                throw new IllegalArgumentException(
                        "DELEGATE transaction should never have a customized 'to' parameter");
            }
            return this; // ignore the provided parameter
        }

        if (pTo == null) {
            throw new IllegalArgumentException("parameter 'to' is required");
        }

        try {
            to = Hex.parse(pTo);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("'to' is not a valid hexadecimal string");
        }

        return this;
    }

    public TransactionBuilder withValue(String pValue) {
        if (type == TransactionType.DELEGATE) {
            if (pValue != null) {
                throw new IllegalArgumentException(
                        "DELEGATE transaction should never have a customized 'value' parameter");
            }
            return this; // ignore the provided parameter
        }

        if (pValue == null) {
            throw new IllegalArgumentException("parameter 'value' is required");
        }

        try {
            value = Long.parseLong(pValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("parameter 'value' is not a valida number");
        }

        return this;
    }

    public TransactionBuilder withFee(String pFee) {
        if (pFee == null) {
            throw new IllegalArgumentException("parameter 'fee' is required");
        }

        try {
            fee = Long.parseLong(pFee);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("parameter 'fee' is not a valid number");
        }

        return this;
    }

    public TransactionBuilder withData(String pData) {
        try {
            data = (pData == null) ? Bytes.EMPTY_BYTES : Hex.parse(pData);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("'data' is not a valid hexadecimal string");
        }

        return this;
    }

    public Transaction build() {
        long timestamp = System.currentTimeMillis();
        long nonce = kernel.getPendingManager().getNonce(account.toAddress());

        // DELEGATE transaction has fixed receiver and value
        if (type == TransactionType.DELEGATE) {
            to = account.toAddress();
            value = kernel.getConfig().minDelegateFee();
        }

        return new Transaction(
                type,
                to,
                value,
                fee,
                nonce,
                timestamp,
                data).sign(account);
    }
}