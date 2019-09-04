/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.util;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

/**
 * This is a builder class for building transactions required by Semux API with
 * provided inputs. The builder expects raw inputs from a HTTP request.
 */
public class TransactionBuilder {

    private final Kernel kernel;

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

    private Long gas = null;
    private Amount gasPrice = Amount.ZERO;

    public TransactionBuilder(Kernel kernel) {
        this.kernel = kernel;
    }

    public TransactionBuilder withType(TransactionType type) {
        if (type != null) {
            this.type = type;
        }
        return this;
    }

    public TransactionBuilder withType(String type) {
        if (type != null) {
            try {
                this.type = TransactionType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Parameter `type` is invalid");
            }
        }
        return this;
    }

    public TransactionBuilder withNetwork(String network) {
        if (network != null) {
            try {
                this.network = Network.valueOf(network);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Parameter `network` is invalid");
            }
        }
        return this;
    }

    public TransactionBuilder withFrom(String from) {
        if (from != null) {
            try {
                this.account = kernel.getWallet().getAccount(Hex.decode0x(from));
            } catch (CryptoException e) {
                throw new IllegalArgumentException("Parameter `from` is not a valid hexadecimal string");
            }

            if (account == null) {
                throw new IllegalArgumentException(
                        String.format("The provided address %s doesn't belong to the wallet", from));
            }
        }
        return this;
    }

    public TransactionBuilder withTo(String to) {
        if (to != null) {
            try {
                this.to = Hex.decode0x(to);
            } catch (CryptoException e) {
                throw new IllegalArgumentException("Parameter `to` is not a valid hexadecimal string");
            }

            if (this.to.length != Key.ADDRESS_LEN) {
                throw new IllegalArgumentException("Parameter `to` is not a valid address");
            }
        }

        return this;
    }

    // value is optional
    public TransactionBuilder withValue(String value) {
        if (value != null) {
            try {
                this.value = Amount.of(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter `value` is not a valid number");
            }
        }

        return this;
    }

    public TransactionBuilder withFee(String fee) {
        if (fee != null) {
            try {
                this.fee = Amount.of(fee);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter `fee` is not a valid number");
            }
        }

        return this;
    }

    public TransactionBuilder withNonce(String nonce) {
        if (nonce != null) {
            try {
                this.nonce = Long.parseLong(nonce);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'nonce' is not a valid number");
            }
        }
        return this;
    }

    public TransactionBuilder withTimestamp(String timestamp) {
        if (timestamp != null) {
            try {
                this.timestamp = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'timestamp' is not a valid number");
            }
        }
        return this;
    }

    public TransactionBuilder withData(String data) {
        if (data != null) {
            try {
                this.data = Hex.decode0x(data);
            } catch (CryptoException e) {
                throw new IllegalArgumentException("Parameter `data` is not a valid hexadecimal string");
            }
        }
        return this;
    }

    public TransactionBuilder withGas(String gasLimit) {
        if (gasLimit != null) {
            try {
                this.gas = Long.parseLong(gasLimit);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter `gas` is not a valid number");
            }
        }
        return this;
    }

    public TransactionBuilder withGasPrice(String gasPrice) {
        if (gasPrice != null) {
            try {
                this.gasPrice = Amount.of(gasPrice);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter `gasPrice` is not a valid number");
            }
        }
        return this;
    }

    public Transaction buildUnsigned() {
        Network network = (this.network != null) ? this.network : kernel.getConfig().network();

        TransactionType type = this.type;
        if (type == null) {
            throw new IllegalArgumentException("Parameter `type` is required");
        }

        Key account = this.account;
        if (account == null) {
            account = kernel.getCoinbase();
        }

        byte[] to = this.to;
        if (to == null) {
            if (type == TransactionType.DELEGATE || type == TransactionType.CREATE) {
                to = Bytes.EMPTY_ADDRESS;
            } else {
                throw new IllegalArgumentException("Parameter `to` is required");
            }
        }

        Amount value = this.value;
        if (value == null) {
            if (type == TransactionType.DELEGATE) {
                value = kernel.getConfig().spec().minDelegateBurnAmount();
            } else if (type == TransactionType.CREATE || type == TransactionType.CALL) {
                value = Amount.ZERO;
            } else {
                throw new IllegalArgumentException("Parameter `value` is required");
            }
        }

        Amount fee = this.fee;
        if (fee == null) {
            if (type == TransactionType.CALL || type == TransactionType.CREATE) {
                fee = Amount.ZERO;
            } else {
                fee = kernel.getConfig().spec().minTransactionFee();
            }
        }

        Long nonce = this.nonce;
        if (nonce == null) {
            nonce = kernel.getPendingManager().getNonce(account.toAddress());
        }

        Long timestamp = this.timestamp;
        if (timestamp == null) {
            timestamp = TimeUtil.currentTimeMillis();
        }

        byte[] data = this.data;
        if (data == null) {
            data = Bytes.EMPTY_BYTES;
        }

        Long gas = this.gas;
        if (gas == null) {
            if (type != TransactionType.CALL && type != TransactionType.CREATE) {
                gas = 0L;
            } else {
                throw new IllegalArgumentException("Parameter `gas` is required");
            }
        }

        Amount gasPrice = this.gasPrice;
        if (gasPrice == null) {
            if (type != TransactionType.CALL && type != TransactionType.CREATE) {
                gasPrice = Amount.ZERO;
            } else {
                throw new IllegalArgumentException("Parameter `gasPrice` is required");
            }
        }

        return new Transaction(network, type, to, value, fee, nonce, timestamp, data, gas, gasPrice);
    }

    public Transaction buildSigned() {
        if (account == null) {
            throw new IllegalArgumentException("The sender is not specified");
        }

        return buildUnsigned().sign(account);
    }
}