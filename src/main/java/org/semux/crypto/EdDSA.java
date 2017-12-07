/**
 * Copyright (c) 2017 The Semux Developers
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
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import org.semux.crypto.cache.EdDSAPublicKeyCache;
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
 * Edwards-curve Digital Signature Algorithm (EdDSA), specifically ED25519.
 * Public key is encoded in "X.509"; private key is encoded in "PKCS#8".
 *
 */
public class EdDSA {

    private static final Logger logger = LoggerFactory.getLogger(EdDSA.class);

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
            logger.error("Failed to initialize keygen", e);
            SystemUtil.exit(-1);
        }
    }

    protected EdDSAPrivateKey sk;
    protected EdDSAPublicKey pk;

    /**
     * Creates a random ED25519 key pair.
     */
    public EdDSA() {
        KeyPair keypair = gen.generateKeyPair();
        sk = (EdDSAPrivateKey) keypair.getPrivate();
        pk = (EdDSAPublicKey) keypair.getPublic();
    }

    /**
     * Creates an ED25519 key pair with the specified public and private keys.
     * 
     * @param privateKey
     * @param publicKey
     * 
     * @throws InvalidKeySpecException
     */
    public EdDSA(byte[] privateKey, byte[] publicKey) throws InvalidKeySpecException {
        this.sk = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKey));
        this.pk = new EdDSAPublicKey(new X509EncodedKeySpec(publicKey));
    }

    /**
     * Creates an ED25519 key pair with a specified private key
     *
     * @param privateKey
     * @throws InvalidKeySpecException
     */
    public EdDSA(byte[] privateKey) throws InvalidKeySpecException {
        this.sk = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKey));
        this.pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
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

            return new Signature(sig, pk.getEncoded());
        } catch (InvalidKeyException | SignatureException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifys a signature.
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
                engine.initVerify(EdDSAPublicKeyCache.computeIfAbsent(signature.getPublicKey()));

                return engine.verifyOneShot(msgHash, signature.getSignature());
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
        private static final int SIG_LENGTH = 64;
        private static final int PUB_LENGTH = 44;

        private byte[] sig;
        private byte[] pub;

        /**
         * Creates a Signature instance.
         * 
         * @param sig
         * @param pub
         */
        public Signature(byte[] sig, byte[] pub) {
            this.sig = sig;
            this.pub = pub;
        }

        /**
         * Returns the raw signature byte array.
         * 
         * @return
         */
        public byte[] getSignature() {
            return sig;
        }

        /**
         * Returns the public key.
         * 
         * @return
         */
        public byte[] getPublicKey() {
            return pub;
        }

        /**
         * Returns the address of signer.
         * 
         * @return
         */
        public byte[] getAddress() {
            return Hash.h160(pub);
        }

        /**
         * Converts into a byte array.
         * 
         * @return
         */
        public byte[] toBytes() {
            byte[] result = new byte[sig.length + pub.length];
            System.arraycopy(sig, 0, result, 0, sig.length);
            System.arraycopy(pub, 0, result, sig.length, pub.length);

            return result;
        }

        /**
         * Parses from byte array.
         * 
         * @param bytes
         * @return a signature if success,or null
         */
        public static Signature fromBytes(byte[] bytes) {
            if (bytes == null || bytes.length != SIG_LENGTH + PUB_LENGTH) {
                return null;
            }

            byte[] sig = Arrays.copyOf(bytes, SIG_LENGTH);
            byte[] pub = Arrays.copyOfRange(bytes, SIG_LENGTH, bytes.length);

            return new Signature(sig, pub);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPrivateKey());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof EdDSA) && Arrays.equals(getPrivateKey(), ((EdDSA) obj).getPrivateKey());
    }

}
