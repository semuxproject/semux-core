/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;

/**
 * Edwards-curve Digital Signature Algorithm (EdDSA), specifically ED25519.
 * Public key is encoded in "X.509"; private key is encoded in "PKCS#8".
 *
 */
public class EdDSA {

    private PublicKey pub;
    private PrivateKey priv;

    /**
     * Create a random ED25519 key pair.
     */
    public EdDSA() {
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        pub = keypair.getPublic();
        priv = keypair.getPrivate();
    }

    /**
     * Create an ED25519 key pair with the specified public and private keys.
     * 
     * @param publicKey
     * @param privateKey
     * @throws InvalidKeySpecException
     */
    public EdDSA(byte[] publicKey, byte[] privateKey) throws InvalidKeySpecException {
        this.pub = new EdDSAPublicKey(new X509EncodedKeySpec(publicKey));
        this.priv = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKey));
    }

    /**
     * Get the public key.
     * 
     * @return
     */
    public byte[] getPublicKey() {
        return pub.getEncoded();
    }

    /**
     * Get the private key.
     */
    public byte[] getPrivateKey() {
        return priv.getEncoded();
    }

    /**
     * Convert this key to an address.
     */
    public byte[] toAddress() {
        return Hash.h160(getPublicKey());
    }

    /**
     * Convert this key to an address, in string.
     */
    public String toAddressString() {
        return Hex.encode(toAddress());
    }

    /**
     * Sign a message hash.
     * 
     * @param msgHash
     *            message hash
     * @return
     */
    public Signature sign(byte[] msgHash) {
        try {
            EdDSAEngine engine = new EdDSAEngine();
            engine.initSign(priv);
            byte[] sig = engine.signOneShot(msgHash);

            return new Signature(sig, pub.getEncoded());
        } catch (InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify a signature.
     * 
     * @param msgHash
     *            message hash
     * @param signature
     *            signature
     * @return
     */
    public static boolean verify(byte[] msgHash, Signature signature) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(signature.getPublicKey());
            EdDSAPublicKey publicKey = new EdDSAPublicKey(spec);

            EdDSAEngine engine = new EdDSAEngine();
            engine.initVerify(publicKey);

            // TODO: reject non-canonical signature

            return engine.verifyOneShot(msgHash, signature.getSignature());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify a signature.
     * 
     * @param msgHash
     *            message hash
     * @param signature
     *            signature
     * @return
     */
    public static boolean verify(byte[] msgHash, byte[] signature) {
        Signature sig = Signature.fromBytes(signature);

        if (sig == null) {
            return false;
        } else {
            return verify(msgHash, Signature.fromBytes(signature));
        }
    }

    /**
     * Get a string representation of this key.
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
         * Create a Signature instance.
         * 
         * @param sig
         * @param pub
         */
        public Signature(byte[] sig, byte[] pub) {
            this.sig = sig;
            this.pub = pub;
        }

        /**
         * Get the raw signature.
         * 
         * @return
         */
        public byte[] getSignature() {
            return sig;
        }

        /**
         * Get the public key.
         * 
         * @return
         */
        public byte[] getPublicKey() {
            return pub;
        }

        /**
         * Convert into a byte array.
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
         * Parse from byte array.
         * 
         * @param bytes
         * @return a signature if success,or null
         * @throws IndexOutOfBoundsException
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
}
