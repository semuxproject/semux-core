/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.util;

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

    public TransactionBuilder(Kernel kernel, TransactionType type) {
        this.kernel = kernel;
        this.type = type;
    }

    public TransactionBuilder withFrom(String fromStr) {
        if (fromStr == null) {
            throw new IllegalArgumentException("Parameter `from` can't be null");
        }

        try {
            account = kernel.getWallet().getAccount(Hex.decode0x(fromStr));
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `from` is not a valid hexadecimal string");
        }

        if (account == null) {
            throw new IllegalArgumentException(
                    String.format("The provided address %s doesn't belong to the wallet", fromStr));
        }

        return this;
    }

    public TransactionBuilder withTo(String toStr) {
        if (type == TransactionType.DELEGATE) {
            if (toStr != null) {
                throw new IllegalArgumentException("Parameter `to` is not needed for DELEGATE transaction");
            }
            return this; // ignore the provided parameter
        }

        if (toStr == null) {
            throw new IllegalArgumentException("Parameter `to` can't be null");
        }

        try {
            this.to = Hex.decode0x(toStr);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `to` is not a valid hexadecimal string");
        }

        return this;
    }

    public TransactionBuilder withValue(String valueStr) {
        if (type == TransactionType.DELEGATE) {
            if (valueStr != null) {
                throw new IllegalArgumentException("Parameter `value` is not needed for DELEGATE transaction");
            }
            return this; // ignore the provided parameter
        }

        if (valueStr == null) {
            throw new IllegalArgumentException("Parameter `value` is required");
        }

        try {
            value = Long.parseLong(valueStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter `value` is not a valida number");
        }

        return this;
    }

    public TransactionBuilder withFee(String feeStr) {
        if (feeStr == null) {
            throw new IllegalArgumentException("Parameter `fee` is required");
        }

        try {
            fee = Long.parseLong(feeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter `fee` is not a valid number");
        }

        return this;
    }

    public TransactionBuilder withData(String dataStr) {
        try {
            data = (dataStr == null) ? Bytes.EMPTY_BYTES : Hex.decode0x(dataStr);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `data` is not a valid hexadecimal string");
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

        return new Transaction(type, to, value, fee, nonce, timestamp, data).sign(account);
    }
}