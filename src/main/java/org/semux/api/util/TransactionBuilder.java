/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.util;

import static org.semux.core.Amount.Unit.NANO_SEM;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.util.Bytes;

/**
 * This is a builder class for building transactions required by Semux API with
 * provided inputs. The builder expects raw inputs from a HTTP request.
 */
public class TransactionBuilder {

    private Kernel kernel;

    /**
     * Network id
     */
    private Network network;

    /**
     * Transaction type
     */
    private TransactionType type;

    /**
     * Transaction sender account
     */
    private Key account;

    /**
     * Transaction recipient address
     */
    private byte[] to;

    /**
     * Transaction value
     */
    private Amount value;

    /**
     * Transaction fee
     */
    private Amount fee;

    /**
     * Transaction nonce.
     */
    private Long nonce;

    /**
     * Transaction timestamp.
     */
    private Long timestamp;

    /**
     * Transaction data
     */
    private byte[] data;

    public TransactionBuilder(Kernel kernel, TransactionType type) {
        this.kernel = kernel;
        this.type = type;
    }

    public TransactionBuilder(Kernel kernel) {
        this.kernel = kernel;
    }

    public TransactionBuilder withType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Parameter `type` is required");
        }

        this.type = TransactionType.valueOf(type);
        return this;
    }

    public TransactionBuilder withNetwork(String network) {
        if (network == null) {
            throw new IllegalArgumentException("Parameter `network` is required");
        }

        this.network = Network.valueOf(network);
        return this;
    }

    public TransactionBuilder withFrom(String from) {
        if (from == null) {
            throw new IllegalArgumentException("Parameter `from` is required");
        }

        try {
            account = kernel.getWallet().getAccount(Hex.decode0x(from));
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `from` is not a valid hexadecimal string");
        }

        if (account == null) {
            throw new IllegalArgumentException(
                    String.format("The provided address %s doesn't belong to the wallet", from));
        }

        return this;
    }

    public TransactionBuilder withTo(String to) {
        if (type == TransactionType.DELEGATE) {
            if (to != null && !to.isEmpty()) {
                throw new IllegalArgumentException("Parameter `to` is not needed for DELEGATE transaction");
            }
            return this; // ignore the provided parameter
        }

        if (to == null) {
            throw new IllegalArgumentException("Parameter `to` is required");
        }

        try {
            this.to = Hex.decode0x(to);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `to` is not a valid hexadecimal string");
        }

        if (this.to.length != Key.ADDRESS_LEN) {
            throw new IllegalArgumentException("Parameter `to` is not a valid address");
        }

        return this;
    }

    public TransactionBuilder withValue(String value) {
        if (type == TransactionType.DELEGATE) {
            if (value != null && !value.isEmpty()) {
                throw new IllegalArgumentException("Parameter `value` is not needed for DELEGATE transaction");
            }
            return this; // ignore the provided parameter
        }

        if (value == null) {
            throw new IllegalArgumentException("Parameter `value` is required");
        }

        try {
            this.value = NANO_SEM.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter `value` is not a valid number");
        }

        return this;
    }

    public TransactionBuilder withFee(String fee) {
        if (fee == null) {
            throw new IllegalArgumentException("Parameter `fee` is required");
        }

        try {
            this.fee = NANO_SEM.of(Long.parseLong(fee));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter `fee` is not a valid number");
        }

        return this;
    }

    public TransactionBuilder withNonce(String nonce) {
        try {
            this.nonce = Long.parseLong(nonce);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter 'nonce' is not a valid number");
        }
        return this;
    }

    public TransactionBuilder withTimestamp(String timestamp) {
        try {
            this.timestamp = timestamp != null && !timestamp.isEmpty() ? Long.parseLong(timestamp) : null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter 'timestamp' is not a valid number");
        }
        return this;
    }

    public TransactionBuilder withData(String data) {
        try {
            this.data = (data == null) ? Bytes.EMPTY_BYTES : Hex.decode0x(data);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Parameter `data` is not a valid hexadecimal string");
        }

        return this;
    }

    public Transaction buildUnsigned() {
        // DELEGATE transaction has fixed receiver and value
        if (type == TransactionType.DELEGATE) {
            to = Bytes.EMPTY_ADDRESS;
            value = kernel.getConfig().minDelegateBurnAmount();
        }

        return new Transaction(
                network != null ? network : kernel.getConfig().network(),
                type,
                to,
                value,
                fee,
                nonce != null ? nonce : kernel.getPendingManager().getNonce(account.toAddress()),
                timestamp != null ? timestamp : System.currentTimeMillis(),
                data);
    }

    public Transaction buildSigned() {
        if (account == null) {
            throw new IllegalArgumentException("TransactionBuilder#withFrom must be called");
        }

        return buildUnsigned().sign(account);
    }
}