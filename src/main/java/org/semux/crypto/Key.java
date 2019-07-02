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
import java.util.Collection;

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
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
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
    private static final EdDSAParameterSpec ED25519SPEC = EdDSANamedCurveTable.getByName("ed25519");

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

    private Key(EdDSAPrivateKey sk, EdDSAPublicKey pk) {
        this.sk = sk;
        this.pk = pk;
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
     * Signs a message.
     * 
     * @param message
     *            message
     * @return
     */
    public Signature sign(byte[] message) {
        try {
            byte[] sig;
            if (Native.isEnabled()) {
                sig = Native.sign(message, Bytes.merge(sk.getSeed(), sk.getAbyte()));
            } else {
                EdDSAEngine engine = new EdDSAEngine();
                engine.initSign(sk);
                sig = engine.signOneShot(message);
            }

            return new Signature(sig, pk.getAbyte());
        } catch (InvalidKeyException | SignatureException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a signature.
     * 
     * @param message
     *            message
     * @param signature
     *            signature
     * @return True if the signature is valid, otherwise false
     */
    public static boolean verify(byte[] message, Signature signature) {
        if (message != null && signature != null) { // avoid null pointer exception
            try {
                if (Native.isEnabled()) {
                    return Native.verify(message, signature.getS(), signature.getA());
                } else {
                    EdDSAEngine engine = new EdDSAEngine();
                    engine.initVerify(PublicKeyCache.computeIfAbsent(signature.getPublicKey()));

                    return engine.verifyOneShot(message, signature.getS());
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        return false;
    }

    public static boolean isVerifyBatchSupported() {
        return Native.isEnabled();
    }

    public static boolean verifyBatch(Collection<byte[]> messages, Collection<Signature> signatures) {
        if (!isVerifyBatchSupported()) {
            throw new UnsupportedOperationException("Key#verifyBatch is only implemented in the native library.");
        }

        return Native.verifyBatch(
                messages.toArray(new byte[messages.size()][]),
                signatures.stream().map(Signature::getS).toArray(byte[][]::new),
                signatures.stream().map(Signature::getA).toArray(byte[][]::new));
    }

    /**
     * Verifies a signature.
     * 
     * @param message
     *            message hash
     * @param signature
     *            signature
     * @return True if the signature is valid, otherwise false
     */
    public static boolean verify(byte[] message, byte[] signature) {
        Signature sig = Signature.fromBytes(signature);

        return verify(message, sig);
    }

    public static Key fromRawPrivateKey(byte[] privateKey) {
        EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(privateKey, ED25519SPEC));
        EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
        return new Key(sk, pk);
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

            return Arrays.equals(toBytes(), ((Signature) o).toBytes());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(toBytes());
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

    public static void main(String[] args) {
        String[] pks = {
                "302a300506032b6570032100fdd012156d14623082633b18a4d342cd37f07af3c5696e11a0947ab0e0bf7e00",
                "302a300506032b6570032100dca9f23f1a1d24972697e7ae17b19557e6d8c21fd3a115757ad85a84293dd5de",
                "302a300506032b6570032100358fe04653e3364f7851d9d94d118e2e09a2add75722180017ecfef606ce5e68",
                "302a300506032b6570032100c91464ea6062350e56c980c4bd5b3f183e3b39179b16a84b123e6f077db1eb70",
        };
        for (String pk : pks) {
            byte[] address = Hash.h160(Hex.decode(pk));
            System.out.println(Hex.encode(address));
        }
    }
}
