/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.util.Bytes.EMPTY_ADDRESS;

import java.util.Arrays;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.semux.Network;
import org.semux.config.Constants;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class Transaction {

    private final byte networkId;

    private final TransactionType type;

    private final byte[] to;

    private final Amount value;

    private final Amount fee;

    private final long nonce;

    private final long timestamp;

    private final byte[] data;

    private final byte[] encoded;

    private final byte[] hash;

    private Signature signature;

    private final Amount gas;

    private final Amount gasLimit;

    /**
     * Create a new transaction.
     * 
     * @param network
     * @param type
     * @param to
     * @param value
     * @param fee
     * @param nonce
     * @param timestamp
     * @param data
     * @param gas
     * @param gasLimit
     */
    public Transaction(Network network, TransactionType type, byte[] to, Amount value, Amount fee, long nonce,
            long timestamp, byte[] data, Amount gas, Amount gasLimit) {
        this.networkId = network.id();
        this.type = type;
        this.to = to;
        this.value = value;
        this.fee = fee;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.data = data;
        this.gas = gas;
        this.gasLimit = gasLimit;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(networkId);
        enc.writeByte(type.toByte());
        enc.writeBytes(to);
        enc.writeAmount(value);
        enc.writeAmount(fee);
        enc.writeLong(nonce);
        enc.writeLong(timestamp);
        enc.writeBytes(data);

        if (TransactionType.CALL == type || TransactionType.CREATE == type) {
            enc.writeAmount(gas);
            enc.writeAmount(gasLimit);
        }
        this.encoded = enc.toBytes();
        this.hash = Hash.h256(encoded);
    }

    /**
     * Create a signed transaction from raw bytes
     *
     * @param hash
     * @param encoded
     * @param signature
     */
    private Transaction(byte[] hash, byte[] encoded, byte[] signature) {
        this.hash = hash;

        Transaction decodedTx = fromEncoded(encoded);
        this.networkId = decodedTx.networkId;
        this.type = decodedTx.type;
        this.to = decodedTx.to;
        this.value = decodedTx.value;
        this.fee = decodedTx.fee;
        this.nonce = decodedTx.nonce;
        this.timestamp = decodedTx.timestamp;
        this.data = decodedTx.data;

        this.gas = decodedTx.gas;
        this.gasLimit = decodedTx.gasLimit;

        this.encoded = encoded;
        this.signature = Signature.fromBytes(signature);
    }

    /**
     * Sign this transaction.
     *
     * @param key
     * @return
     */
    public Transaction sign(Key key) {
        this.signature = key.sign(this.hash);
        return this;
    }

    /**
     * <p>
     * Validate transaction format and signature. </>
     *
     * <p>
     * NOTE: this method does not check transaction validity over the state. Use
     * {@link TransactionExecutor} for that purpose
     * </p>
     *
     * @param network
     * @return true if success, otherwise false
     */
    public boolean validate(Network network) {
        return hash != null && hash.length == Hash.HASH_LEN
                && networkId == network.id()
                && type != null
                && to != null && to.length == Key.ADDRESS_LEN
                && value.gte0()
                && fee.gte0()
                && nonce >= 0
                && timestamp > 0
                && data != null
                && encoded != null
                && signature != null && !Arrays.equals(signature.getAddress(), EMPTY_ADDRESS)

                && Arrays.equals(Hash.h256(encoded), hash)
                && Key.verify(hash, signature)

                // The coinbase key is publicly available. People can use it for transactions.
                // It won't introduce any fundamental loss to the system but could potentially
                // cause confusion for block explorer, and thus are prohibited.
                && (type == TransactionType.COINBASE
                        || (!Arrays.equals(signature.getAddress(), Constants.COINBASE_ADDRESS) &&
                                !Arrays.equals(to, Constants.COINBASE_ADDRESS)));
    }

    /**
     * Returns the transaction network id.
     *
     * @return
     */
    public byte getNetworkId() {
        return networkId;
    }

    /**
     * Returns the transaction hash.
     *
     * @return
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * Returns the transaction type.
     *
     * @return
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Parses the from address from signature.
     *
     * @return an address if the signature is valid, otherwise null
     */
    public byte[] getFrom() {
        return (signature == null) ? null : signature.getAddress();
    }

    /**
     * Returns the recipient address.
     *
     * @return
     */
    public byte[] getTo() {
        return to;
    }

    /**
     * Returns the value.
     *
     * @return
     */
    public Amount getValue() {
        return value;
    }

    /**
     * Returns the transaction fee.
     *
     * @return
     */
    public Amount getFee() {
        return fee;
    }

    /**
     * Returns the nonce.
     *
     * @return
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Returns the timestamp.
     *
     * @return
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the extra data.
     *
     * @return
     */
    public byte[] getData() {
        return data;
    }

    public byte[] getEncoded() {
        return encoded;
    }

    public Amount getGas() {
        return gas;
    }

    public Amount getGasLimit() {
        return gasLimit;
    }

    /**
     * Decodes an byte-encoded transaction that is not yet signed by a private key.
     *
     * @param encoded
     *            the bytes of encoded transaction
     * @return the decoded transaction
     */
    public static Transaction fromEncoded(byte[] encoded) {
        SimpleDecoder decoder = new SimpleDecoder(encoded);

        byte networkId = decoder.readByte();
        byte type = decoder.readByte();
        byte[] to = decoder.readBytes();
        Amount value = decoder.readAmount();
        Amount fee = decoder.readAmount();
        long nonce = decoder.readLong();
        long timestamp = decoder.readLong();
        byte[] data = decoder.readBytes();

        Amount gas = Amount.ZERO;
        Amount gasLimit = Amount.ZERO;

        TransactionType transactionType = TransactionType.of(type);
        if (TransactionType.CALL == transactionType || TransactionType.CREATE == transactionType) {
            try {
                gas = decoder.readAmount();
                gasLimit = decoder.readAmount();
            } catch (Exception e) {
                // old transactions will not have these fields, so need to handle backwards
                // compatibility
            }
        }

        return new Transaction(Network.of(networkId), transactionType, to, value, fee, nonce, timestamp, data,
                gas, gasLimit);
    }

    /**
     * Returns the signature.
     *
     * @return
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * Converts into a byte array.
     *
     * @return
     */
    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(hash);
        enc.writeBytes(encoded);
        enc.writeBytes(signature.toBytes());

        return enc.toBytes();
    }

    /**
     * Parses from a byte array.
     *
     * @param bytes
     * @return
     */
    public static Transaction fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new Transaction(hash, encoded, signature);
    }

    /**
     * Returns size of the transaction in bytes
     *
     * @return size in bytes
     */
    public int size() {
        return toBytes().length;
    }

    @Override
    public String toString() {
        return "Transaction [type=" + type + ", from=" + Hex.encode(getFrom()) + ", to=" + Hex.encode(to) + ", value="
                + value + ", fee=" + fee + ", nonce=" + nonce + ", timestamp=" + timestamp + ", data="
                + Hex.encode(data) + ", hash=" + Hex.encode(hash) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Transaction that = (Transaction) o;

        return new EqualsBuilder()
                .append(encoded, that.encoded)
                .append(hash, that.hash)
                .append(signature, that.signature)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(encoded)
                .append(hash)
                .append(signature)
                .toHashCode();
    }
}
