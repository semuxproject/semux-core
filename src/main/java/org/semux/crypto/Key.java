/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.semux.crypto.cache.PublicKeyCache;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Represents a key pair for the ED25519 signature algorithm.
 * <p>
 * Public key is encoded in "X.509"; private key is encoded in "PKCS#8".
 */
public class Key {

    public static final int PUBLIC_KEY_LEN = 44;
    public static final int PRIVATE_KEY_LEN = 48;
    public static final int ADDRESS_LEN = 20;

    private static final Logger logger = LoggerFactory.getLogger(Key.class);

    private static final KeyPairGenerator gen = new KeyPairGenerator();
    static {
        /*
         * Algorithm specifications
         * 
         * Name: Ed25519
         * 
         * Curve: ed25519curve
         * 
         * H: SHA-512
         * 
         * l: $q = 2^{252} + 27742317777372353535851937790883648493$
         * 
         * B: 0x5866666666666666666666666666666666666666666666666666666666666666
         */
        try {
            EdDSANamedCurveSpec params = EdDSANamedCurveTable.getByName("Ed25519");
            gen.initialize(params, new SecureRandom());
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Failed to initialize Ed25519 engine", e);
            SystemUtil.exit(SystemUtil.Code.FAILED_TO_INIT_ED25519);
        }
    }

    protected EdDSAPrivateKey sk;
    protected EdDSAPublicKey pk;

    /**
     * Creates a random ED25519 key pair.
     */
    public Key() {
        KeyPair keypair = gen.generateKeyPair();
        sk = (EdDSAPrivateKey) keypair.getPrivate();
        pk = (EdDSAPublicKey) keypair.getPublic();
    }

    /**
     * Creates an ED25519 key pair with a specified private key
     *
     * @param privateKey
     *            the private key in "PKCS#8" format
     * @throws InvalidKeySpecException
     */
    public Key(byte[] privateKey) throws InvalidKeySpecException {
        this.sk = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKey));
        this.pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
    }

    /**
     * Creates an ED25519 key pair with the specified public and private keys.
     * 
     * @param privateKey
     *            the private key in "PKCS#8" format
     * @param publicKey
     *            the public key in "X.509" format, for verification purpose only
     * 
     * @throws InvalidKeySpecException
     */
    public Key(byte[] privateKey, byte[] publicKey) throws InvalidKeySpecException {
        this(privateKey);

        if (!Arrays.equals(getPublicKey(), publicKey)) {
            throw new InvalidKeySpecException("Public key and private key do not match!");
        }
    }

    /**
     * Returns the private key, encoded in "PKCS#8".
     */
    public byte[] getPrivateKey() {
        return sk.getEncoded();
    }

    /**
     * Returns the public key, encoded in "X.509".
     * 
     * @return
     */
    public byte[] getPublicKey() {
        return pk.getEncoded();
    }

    /**
     * Returns the Semux address.
     */
    public byte[] toAddress() {
        return Hash.h160(getPublicKey());
    }

    /**
     * Returns the Semux address in {@link String}.
     */
    public String toAddressString() {
        return Hex.encode(toAddress());
    }

    /**
     * Signs a message hash.
     * 
     * @param msgHash
     *            message hash
     * @return
     */
    public Signature sign(byte[] msgHash) {
        try {
            EdDSAEngine engine = new EdDSAEngine();
            engine.initSign(sk);
            byte[] sig = engine.signOneShot(msgHash);

            return new Signature(sig, pk.getAbyte());
        } catch (InvalidKeyException | SignatureException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a signature.
     * 
     * @param msgHash
     *            message hash
     * @param signature
     *            signature
     * @return True if the signature is valid, otherwise false
     */
    public static boolean verify(byte[] msgHash, Signature signature) {
        if (msgHash != null && signature != null) { // avoid null pointer exception
            try {
                EdDSAEngine engine = new EdDSAEngine();
                engine.initVerify(PublicKeyCache.computeIfAbsent(signature.getPublicKey()));

                return engine.verifyOneShot(msgHash, signature.getS());
            } catch (Exception e) {
                // do nothing
            }
        }

        return false;
    }

    /**
     * Verifies a signature.
     * 
     * @param msgHash
     *            message hash
     * @param signature
     *            signature
     * @return True if the signature is valid, otherwise false
     */
    public static boolean verify(byte[] msgHash, byte[] signature) {
        Signature sig = Signature.fromBytes(signature);

        return verify(msgHash, sig);
    }

    /**
     * Returns a string representation of this key.
     * 
     * @return the address of this EdDSA.
     */
    @Override
    public String toString() {
        return toAddressString();
    }

    /**
     * Represents an EdDSA signature, wrapping the raw signature and public key.
     * 
     */
    public static class Signature {
        public static final int LENGTH = 96;

        private static final byte[] X509 = Hex.decode("302a300506032b6570032100");
        private static final int S_LEN = 64;
        private static final int A_LEN = 32;

        private byte[] s;
        private byte[] a;

        /**
         * Creates a Signature instance.
         * 
         * @param s
         * @param a
         */
        public Signature(byte[] s, byte[] a) {
            if (s == null || s.length != S_LEN || a == null || a.length != A_LEN) {
                throw new IllegalArgumentException("Invalid S or A");
            }
            this.s = s;
            this.a = a;
        }

        /**
         * Returns the S byte array.
         * 
         * @return
         */
        public byte[] getS() {
            return s;
        }

        /**
         * Returns the A byte array.
         * 
         * @return
         */
        public byte[] getA() {
            return a;
        }

        /**
         * Returns the public key of the signer.
         * 
         * @return
         */
        public byte[] getPublicKey() {
            return Bytes.merge(X509, a);
        }

        /**
         * Returns the address of signer.
         * 
         * @return
         */
        public byte[] getAddress() {
            return Hash.h160(getPublicKey());
        }

        /**
         * Converts into a byte array.
         * 
         * @return
         */
        public byte[] toBytes() {
            return Bytes.merge(s, a);
        }

        /**
         * Parses from byte array.
         * 
         * @param bytes
         * @return a {@link Signature} if success,or null
         */
        public static Signature fromBytes(byte[] bytes) {
            if (bytes == null || bytes.length != LENGTH) {
                return null;
            }

            byte[] s = Arrays.copyOfRange(bytes, 0, S_LEN);
            byte[] a = Arrays.copyOfRange(bytes, LENGTH - A_LEN, LENGTH);

            return new Signature(s, a);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            Signature signature = (Signature) o;

            return new EqualsBuilder()
                    .append(s, signature.s)
                    .append(a, signature.a)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(s)
                    .append(a)
                    .toHashCode();
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPrivateKey());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Key) && Arrays.equals(getPrivateKey(), ((Key) obj).getPrivateKey());
    }

}
