/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.crypto.bip32;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.semux.crypto.CryptoException;
import org.semux.crypto.bip32.key.HdPrivateKey;
import org.semux.crypto.bip32.key.HdPublicKey;
import org.semux.crypto.bip32.key.KeyVersion;
import org.semux.crypto.bip32.util.HashUtil;
import org.semux.crypto.bip32.util.HdUtil;
import org.semux.crypto.bip32.util.Hmac;
import org.semux.crypto.bip32.util.Secp256k1;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class HdKeyGenerator {

    private static boolean trace = true;

    private static final EdDSAParameterSpec ED25519SPEC = EdDSANamedCurveTable.getByName("ed25519");

    public static final String MASTER_PATH = "m";

    public HdKeyPair getMasterKeyPairFromSeed(byte[] seed, KeyVersion keyVersion, CoinType coinType) {

        Scheme scheme = coinType.getScheme();
        HdPublicKey publicKey = new HdPublicKey();
        HdPrivateKey privateKey = new HdPrivateKey();
        HdKeyPair key = new HdKeyPair(privateKey, publicKey, coinType, MASTER_PATH);

        byte[] I = Hmac.hmac512(seed, scheme.getSeed().getBytes(StandardCharsets.UTF_8));
        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);

        switch (scheme) {
        case BIP32:
            BigInteger masterSecretKey = HdUtil.parse256(IL);
            // In case IL is 0 or >=n, the master key is invalid.
            if (masterSecretKey.compareTo(BigInteger.ZERO) == 0
                    || masterSecretKey.compareTo(Secp256k1.getN()) > 0) {
                throw new CryptoException("The master key is invalid");
            }
        case SLIP10_ED25519:
            // Any IL would be fine
            break;
        case BIP32_ED25519:
            // loop when the third highest bit of the last byte of IL is not zero
            while ((I[31] & 0b00100000) != 0) {
                I = Hmac.hmac512(I, scheme.getSeed().getBytes(StandardCharsets.UTF_8));
            }
            // the lowest 3 bits of the first byte of IL of are cleared
            I[0] = (byte) (I[0] & ~0b00000111);
            // the highest bit of the last byte is cleared
            I[31] = (byte) (I[31] & ~0b10000000);
            // the second highest bit of the last byte is set
            I[31] = (byte) (I[31] | 0b01000000);

            IL = Arrays.copyOfRange(I, 0, 32);
            IR = Arrays.copyOfRange(I, 32, 64);
            break;
        }

        privateKey.setVersion(keyVersion.getPrivateKeyVersion());
        privateKey.setDepth(0);
        privateKey.setFingerprint(new byte[] { 0, 0, 0, 0 });
        privateKey.setChildNumber(new byte[] { 0, 0, 0, 0 });
        privateKey.setChainCode(IR);

        publicKey.setVersion(keyVersion.getPublicKeyVersion());
        publicKey.setDepth(0);
        publicKey.setFingerprint(new byte[] { 0, 0, 0, 0 });
        publicKey.setChildNumber(new byte[] { 0, 0, 0, 0 });
        publicKey.setChainCode(IR);

        switch (scheme) {
        case BIP32:
            privateKey.setKeyData(HdUtil.merge(new byte[] { 0 }, IL));
            publicKey.setKeyData(Secp256k1.serP(Secp256k1.point(HdUtil.parse256(IL))));
            break;
        case SLIP10_ED25519:
            EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(IL, ED25519SPEC));
            EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
            privateKey.setKeyData(IL); // 32 bytes
            publicKey.setKeyData(HdUtil.merge(new byte[] { 0 }, pk.getAbyte())); // 33 bytes
            break;
        case BIP32_ED25519:
            // Attention: no-standard key format
            byte[] A = ED25519SPEC.getB().scalarMultiply(IL).toByteArray();
            privateKey.setKeyData(I); // 64 bytes
            publicKey.setKeyData(A); // 32 bytes?

            byte[] c = Hmac.hmac256(HdUtil.merge(new byte[] { 1 }, seed),
                    scheme.getSeed().getBytes(StandardCharsets.UTF_8));
            privateKey.setChainCode(c);
            publicKey.setChainCode(c);
            break;
        }

        return key;
    }

    /**
     * Derive the child public key from the parent public key. This is typically
     * used for calculating the public key (or address) without revealing the
     * private key.
     *
     * @param parent
     *            the parent key
     * @param child
     *            the child index
     * @param isHardened
     *            whether the child index is hardened
     * @param scheme
     *            the curve
     * @return
     */
    public HdPublicKey getChildPublicKey(HdPublicKey parent, long child, boolean isHardened, Scheme scheme) {
        if (isHardened) {
            throw new CryptoException("Cannot derive child public keys from hardened keys");
        }

        if (scheme == Scheme.SLIP10_ED25519) {
            throw new UnsupportedOperationException("Unable to derive ed25519 public key chaining");
        }

        if (scheme == Scheme.BIP32_ED25519) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        byte[] key = parent.getKeyData();
        byte[] data = HdUtil.merge(key, HdUtil.ser32(child));

        // I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i))
        byte[] I = Hmac.hmac512(data, parent.getChainCode());
        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);

        HdPublicKey publicKey = new HdPublicKey();

        publicKey.setVersion(parent.getVersion());
        publicKey.setDepth(parent.getDepth() + 1);

        byte[] pKd = parent.getKeyData();
        byte[] h160 = HashUtil.h160(pKd);
        byte[] childFingerprint = new byte[] { h160[0], h160[1], h160[2], h160[3] };

        BigInteger ILBigInt = HdUtil.parse256(IL);
        ECPoint point = Secp256k1.point(ILBigInt);
        point = point.add(Secp256k1.deserP(parent.getKeyData()));

        if (ILBigInt.compareTo(Secp256k1.getN()) > 0 || point.isInfinity()) {
            throw new CryptoException("This key is invalid, should proceed to next key");
            // return getChildPublicKey(parent, child+1, isHardened);
        }

        byte[] childKey = Secp256k1.serP(point);

        publicKey.setFingerprint(childFingerprint);
        publicKey.setChildNumber(HdUtil.ser32(child));
        publicKey.setChainCode(IR);
        publicKey.setKeyData(childKey);

        return publicKey;
    }

    /**
     * Derive the child key pair (public + private) from the parent key pair.
     *
     * @param parent
     *            the parent key
     * @param child
     *            the child index
     * @param isHardened
     *            whether is child index is hardened
     * @return
     */
    public HdKeyPair getChildKeyPair(HdKeyPair parent, long child, boolean isHardened) {
        HdPrivateKey privateKey = new HdPrivateKey();
        HdPublicKey publicKey = new HdPublicKey();
        HdKeyPair key = new HdKeyPair(privateKey, publicKey, parent.getCoinType(),
                getPath(parent.getPath(), child, isHardened));

        if (isHardened) {
            child += 0x80000000;
        } else if (parent.getCoinType().getScheme() == Scheme.SLIP10_ED25519) {
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
            byte[] data = HdUtil.merge(new byte[] { 0 }, HdUtil.ser256(kpar), HdUtil.ser32(child));
            I = Hmac.hmac512(data, xChain);
        } else {
            // I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i))
            // just use public key
            byte[] data = HdUtil.merge(parent.getPublicKey().getKeyData(), HdUtil.ser32(child));
            I = Hmac.hmac512(data, xChain);
        }
        // split into left/right
        byte[] IL = Arrays.copyOfRange(I, 0, 32);
        byte[] IR = Arrays.copyOfRange(I, 32, 64);

        byte[] childNumber = HdUtil.ser32(child);

        privateKey.setVersion(parent.getPrivateKey().getVersion());
        privateKey.setDepth(parent.getPrivateKey().getDepth() + 1);
        privateKey.setChildNumber(childNumber);
        privateKey.setChainCode(IR);

        publicKey.setVersion(parent.getPublicKey().getVersion());
        publicKey.setDepth(parent.getPublicKey().getDepth() + 1);
        publicKey.setChildNumber(childNumber);
        publicKey.setChainCode(IR);

        switch (parent.getCoinType().getScheme()) {
        case BIP32:
            // The returned child key ki is parse256(IL) + kpar (mod n).
            BigInteger parse256 = HdUtil.parse256(IL);
            BigInteger kpar = HdUtil.parse256(parent.getPrivateKey().getKeyData());
            BigInteger childSecretKey = parse256.add(kpar).mod(Secp256k1.getN());

            byte[] fingerprintSK = HdUtil.getFingerprint(parent.getPrivateKey().getKeyData());
            privateKey.setFingerprint(fingerprintSK);
            privateKey.setKeyData(HdUtil.merge(new byte[] { 0 }, HdUtil.ser256(childSecretKey)));

            byte[] pKd = parent.getPublicKey().getKeyData();
            byte[] h160 = HashUtil.h160(pKd);
            byte[] fingerprintPK = new byte[] { h160[0], h160[1], h160[2], h160[3] };
            publicKey.setFingerprint(fingerprintPK);
            publicKey.setKeyData(Secp256k1.serP(Secp256k1.point(childSecretKey)));
            break;
        case SLIP10_ED25519:
            // NOTE: fingerprints are the same for both HdPublicKey and HdPrivateKey?
            h160 = HashUtil.h160(parent.getPublicKey().getKeyData());
            fingerprintPK = new byte[] { h160[0], h160[1], h160[2], h160[3] };

            privateKey.setFingerprint(fingerprintPK);
            privateKey.setKeyData(IL);

            EdDSAPrivateKey sk = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(IL, ED25519SPEC));
            EdDSAPublicKey pk = new EdDSAPublicKey(new EdDSAPublicKeySpec(sk.getA(), sk.getParams()));
            publicKey.setFingerprint(fingerprintPK);
            publicKey.setKeyData(HdUtil.merge(new byte[] { 0 }, pk.getAbyte()));
            break;
        case BIP32_ED25519:
            byte[] kL = parent.getPrivateKey().getKeyData();
            byte[] kLP = Arrays.copyOfRange(kL, 0, 32);
            byte[] kRP = Arrays.copyOfRange(kL, 32, 64);
            byte[] AP = parent.getPublicKey().getKeyData();
            byte[] cP = parent.getPublicKey().getChainCode();

            byte[] Z, c;
            if (isHardened) {
                byte[] data = HdUtil.merge(new byte[] { 0 }, kLP, kRP, HdUtil.ser32LE(child));
                Z = Hmac.hmac512(data, cP);
                data[0] = 1;
                c = Hmac.hmac512(data, cP);
            } else {
                byte[] data = HdUtil.merge(new byte[] { 2 }, AP, HdUtil.ser32LE(child));
                Z = Hmac.hmac512(data, cP);
                data[0] = 3;
                c = Hmac.hmac512(data, cP);
            }
            c = Arrays.copyOfRange(c, 32, 64);
            byte[] ZL = Arrays.copyOfRange(Z, 0, 28);
            byte[] ZR = Arrays.copyOfRange(Z, 32, 64);

            if (trace) {
                System.out.println("parent, kLP = " + Hex.toHexString(kLP));
                System.out.println("parent, kRP = " + Hex.toHexString(kRP));
                System.out.println("parent,  AP = " + Hex.toHexString(AP));
                System.out.println("parent,  cP = " + Hex.toHexString(cP));
            }

            BigInteger kLiBI = parseUnsignedLE(ZL)
                    .multiply(BigInteger.valueOf(8))
                    .add(parseUnsignedLE(kLP));
            BigInteger order = BigInteger.valueOf(2).pow(252)
                    .add(new BigInteger("27742317777372353535851937790883648493"));
            if (kLiBI.mod(order).equals(BigInteger.ZERO)) {
                return null;
            }
            IL = serializeUnsignedLE256(kLiBI);

            BigInteger kRiBI = parseUnsignedLE(ZR)
                    .add(parseUnsignedLE(kRP))
                    .mod(BigInteger.valueOf(2).pow(256));
            IR = serializeUnsignedLE256(kRiBI);

            I = HdUtil.merge(IL, IR);
            byte[] A = ED25519SPEC.getB().scalarMultiply(IL).toByteArray();

            privateKey.setKeyData(I);
            publicKey.setKeyData(A);

            privateKey.setChainCode(c);
            publicKey.setChainCode(c);

            if (trace) {
                System.out.println("child, IL = " + Hex.toHexString(IL));
                System.out.println("child, IR = " + Hex.toHexString(IR));
                System.out.println("child,  A = " + Hex.toHexString(A));
                System.out.println("child,  c = " + Hex.toHexString(c));
            }
            break;
        }

        return key;
    }

    private String getPath(String parentPath, long child, boolean isHardened) {
        if (parentPath == null) {
            parentPath = MASTER_PATH;
        }
        return parentPath + "/" + child + (isHardened ? "'" : "");
    }

    private BigInteger parseUnsignedLE(byte[] bytes) {
        byte[] temp = bytes.clone();
        ArrayUtils.reverse(temp);
        return new BigInteger(1, temp);
    }

    private byte[] serializeUnsignedLE256(BigInteger bi) {
        byte[] temp = bi.toByteArray();
        if (temp.length > 32) {
            temp = Arrays.copyOfRange(temp, temp.length - 32, temp.length);
        }

        ArrayUtils.reverse(temp);

        if (temp.length < 32) {
            return Arrays.copyOf(temp, 32);
        } else {
            return temp;
        }
    }
}
