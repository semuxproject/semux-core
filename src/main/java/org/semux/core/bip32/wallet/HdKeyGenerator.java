/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32.wallet;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import org.bouncycastle.math.ec.ECPoint;
import org.semux.core.bip32.Network;
import org.semux.core.bip32.crypto.Hash;
import org.semux.core.bip32.crypto.HdUtil;
import org.semux.core.bip32.crypto.HmacSha512;
import org.semux.core.bip32.crypto.Secp256k1;
import org.semux.core.bip32.wallet.key.Curve;
import org.semux.core.bip32.wallet.key.HdPrivateKey;
import org.semux.core.bip32.wallet.key.HdPublicKey;
import org.semux.crypto.CryptoException;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class HdKeyGenerator {

    private static final EdDSAParameterSpec ED25519SPEC = EdDSANamedCurveTable.getByName("ed25519");
    public static final String MASTER_PATH = "m";

    public HdAddress getAddressFromSeed(byte[] seed, Network network, CoinType coinType) {

        Curve curve = coinType.getCurve();
        HdPublicKey publicKey = new HdPublicKey();
        HdPrivateKey privateKey = new HdPrivateKey();
        HdAddress address = new HdAddress(privateKey, publicKey, coinType, MASTER_PATH);

        byte[] I;
        try {
            I = HmacSha512.hmac512(seed, curve.getSeed().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("Unable to decode seed.");
        }

        // split into left/right
        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);

        BigInteger masterSecretKey = HdUtil.parse256(IL);

        // In case IL is 0 or >=n, the master key is invalid.
        if (curve != Curve.ed25519 && masterSecretKey.compareTo(BigInteger.ZERO) == 0
                || masterSecretKey.compareTo(Secp256k1.getN()) > 0) {
            throw new CryptoException("The master key is invalid");
        }

        privateKey.setVersion(network.getPrivateKeyVersion());
        privateKey.setDepth(0);
        privateKey.setFingerprint(new byte[] { 0, 0, 0, 0 });
        privateKey.setChildNumber(new byte[] { 0, 0, 0, 0 });
        privateKey.setChainCode(IR);
        privateKey.setKeyData(HdUtil.append(new byte[] { 0 }, IL));

        ECPoint point = Secp256k1.point(masterSecretKey);

        publicKey.setVersion(network.getPublicKeyVersion());
        publicKey.setDepth(0);
        publicKey.setFingerprint(new byte[] { 0, 0, 0, 0 });
        publicKey.setChildNumber(new byte[] { 0, 0, 0, 0 });
        publicKey.setChainCode(IR);
        publicKey.setKeyData(Secp256k1.serP(point));

        switch (curve) {
        case bitcoin:
            privateKey.setPrivateKey(privateKey.getKeyData());
            publicKey.setPublicKey(publicKey.getKeyData());
            break;
        case ed25519:
            privateKey.setPrivateKey(IL);
            EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(IL, ED25519SPEC));
            EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
            publicKey.setPublicKey(HdUtil.append(new byte[] { 0 }, pk.getAbyte()));
            break;
        }

        return address;
    }

    public HdPublicKey getPublicKey(HdPublicKey parent, long child, boolean isHardened, Curve curve) {
        if (isHardened) {
            throw new CryptoException("Cannot derive child public keys from hardened keys");
        }

        if (curve == Curve.ed25519) {
            throw new UnsupportedOperationException("Unable to derive ed25519 public key chaining");
        }

        byte[] key = parent.getKeyData();
        byte[] data = HdUtil.append(key, HdUtil.ser32(child));
        // I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i))
        byte[] I = HmacSha512.hmac512(data, parent.getChainCode());

        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);

        HdPublicKey publicKey = new HdPublicKey();

        publicKey.setVersion(parent.getVersion());
        publicKey.setDepth(parent.getDepth() + 1);

        byte[] pKd = parent.getKeyData();
        byte[] h160 = Hash.h160(pKd);
        byte[] childFingerprint = new byte[] { h160[0], h160[1], h160[2], h160[3] };

        BigInteger ILBigInt = HdUtil.parse256(IL);
        ECPoint point = Secp256k1.point(ILBigInt);
        point = point.add(Secp256k1.deserP(parent.getKeyData()));

        if (ILBigInt.compareTo(Secp256k1.getN()) > 0 || point.isInfinity()) {
            throw new CryptoException("This key is invalid, should proceed to next key");
            // return getPublicKey(parent, child+1, isHardened);
        }

        byte[] childKey = Secp256k1.serP(point);

        publicKey.setFingerprint(childFingerprint);
        publicKey.setChildNumber(HdUtil.ser32(child));
        publicKey.setChainCode(IR);
        publicKey.setKeyData(childKey);

        return publicKey;
    }

    public HdAddress getAddress(HdAddress parent, long child, boolean isHardened) {
        HdPrivateKey privateKey = new HdPrivateKey();
        HdPublicKey publicKey = new HdPublicKey();
        HdAddress address = new HdAddress(privateKey, publicKey, parent.getCoinType(),
                getPath(parent.getPath(), child, isHardened));

        if (isHardened) {
            child += 0x80000000;
        } else if (parent.getCoinType().getCurve() == Curve.ed25519) {
            throw new CryptoException("ed25519 only supports hardened keys");
        }

        byte[] xChain = parent.getPrivateKey().getChainCode();
        /// backwards hmac order in method?
        byte[] I;
        if (isHardened) {
            // If so (hardened child): let I = HMAC-SHA512(Key = cpar, Data = 0x00 ||
            // ser256(kpar) || ser32(i)). (Note: The 0x00 pads the private key to make it 33
            // bytes long.)
            BigInteger kpar = HdUtil.parse256(parent.getPrivateKey().getKeyData());
            byte[] data = HdUtil.append(new byte[] { 0 }, HdUtil.ser256(kpar));
            data = HdUtil.append(data, HdUtil.ser32(child));
            I = HmacSha512.hmac512(data, xChain);
        } else {
            // I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i))
            // just use public key
            byte[] key = parent.getPublicKey().getKeyData();
            byte[] xPubKey = HdUtil.append(key, HdUtil.ser32(child));
            I = HmacSha512.hmac512(xPubKey, xChain);
        }

        // split into left/right
        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);
        // The returned child key ki is parse256(IL) + kpar (mod n).
        BigInteger parse256 = HdUtil.parse256(IL);
        BigInteger kpar = HdUtil.parse256(parent.getPrivateKey().getKeyData());
        BigInteger childSecretKey = parse256.add(kpar).mod(Secp256k1.getN());

        byte[] childNumber = HdUtil.ser32(child);
        byte[] fingerprint = HdUtil.getFingerprint(parent.getPrivateKey().getKeyData());

        privateKey.setVersion(parent.getPrivateKey().getVersion());
        privateKey.setDepth(parent.getPrivateKey().getDepth() + 1);
        privateKey.setFingerprint(fingerprint);
        privateKey.setChildNumber(childNumber);
        privateKey.setChainCode(IR);
        privateKey.setKeyData(HdUtil.append(new byte[] { 0 }, HdUtil.ser256(childSecretKey)));

        ECPoint point = Secp256k1.point(childSecretKey);

        publicKey.setVersion(parent.getPublicKey().getVersion());
        publicKey.setDepth(parent.getPublicKey().getDepth() + 1);

        // can just use fingerprint, but let's use data from parent public key
        byte[] pKd = parent.getPublicKey().getKeyData();
        byte[] h160 = Hash.h160(pKd);
        byte[] childFingerprint = new byte[] { h160[0], h160[1], h160[2], h160[3] };

        publicKey.setFingerprint(childFingerprint);
        publicKey.setChildNumber(childNumber);
        publicKey.setChainCode(IR);
        publicKey.setKeyData(Secp256k1.serP(point));

        switch (parent.getCoinType().getCurve()) {
        case bitcoin:
            privateKey.setPrivateKey(privateKey.getKeyData());
            publicKey.setPublicKey(publicKey.getKeyData());
            break;
        case ed25519:
            privateKey.setPrivateKey(IL);
            h160 = Hash.h160(parent.getPublicKey().getPublicKey());
            childFingerprint = new byte[] { h160[0], h160[1], h160[2], h160[3] };
            publicKey.setFingerprint(childFingerprint);
            privateKey.setFingerprint(childFingerprint);
            privateKey.setKeyData(HdUtil.append(new byte[] { 0 }, IL));

            EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(IL, ED25519SPEC));
            EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
            publicKey.setPublicKey(HdUtil.append(new byte[] { 0 }, pk.getAbyte()));
            break;
        }

        return address;
    }

    private String getPath(String parentPath, long child, boolean isHardened) {
        if (parentPath == null) {
            parentPath = MASTER_PATH;
        }
        return parentPath + "/" + child + (isHardened ? "'" : "");
    }
}
