/*
 * Copyright (c) [2018] [ The Semux Developers ]
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ethereum.vm.crypto;

import static org.ethereum.vm.util.BigIntUtil.isLessThan;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.vm.util.HashUtil;

public class ECKey {

    public static final BigInteger SECP256K1N = new BigInteger(
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    public static final ECDomainParameters CURVE;

    static {
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
    }

    public static byte[] signatureToAddress(byte[] messageHash, ECDSASignature sig) throws SignatureException {
        check(messageHash.length == 32, "messageHash argument has length " + messageHash.length);

        int header = sig.v;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        // 0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34) {
            throw new SignatureException("Header byte out of range: " + header);
        }
        if (header >= 31) {
            header -= 4;
        }
        int recId = header - 27;

        byte[] pubBytes = ECKey.recoverPubBytesFromSignature(recId, sig, messageHash);
        if (pubBytes == null) {
            throw new SignatureException("Could not recover public key from signature");
        }

        return HashUtil.sha3omit12(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    /**
     * <p>
     * Given the components of a signature and a selector value, recover and return
     * the public key that generated the signature according to the algorithm in
     * SEC1v2 section 4.1.6.
     * </p>
     *
     * <p>
     * The recId is an index from 0 to 3 which indicates which of the 4 possible
     * keys is the correct one. Because the key recovery operation yields multiple
     * potential keys, the correct key must either be stored alongside the
     * signature, or you must be willing to try each recId in turn until you find
     * one that outputs the key you are expecting.
     * </p>
     *
     * <p>
     * If this method returns null it means recovery was not possible and recId
     * should be iterated.
     * </p>
     *
     * <p>
     * Given the above two points, a correct usage of this method is inside a for
     * loop from 0 to 3, and if the output is null OR a key that is not the one you
     * expect, you try again with the next recId.
     * </p>
     *
     * @param recId
     *            Which possible key to recover.
     * @param sig
     *            the R and S components of the signature, wrapped.
     * @param messageHash
     *            Hash of the data that was signed.
     * @return 65-byte encoded public key
     */
    public static byte[] recoverPubBytesFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
        check(recId >= 0, "recId must be positive");
        check(sig.r.signum() >= 0, "r must be positive");
        check(sig.s.signum() >= 0, "s must be positive");
        check(messageHash != null, "messageHash must not be null");
        // 1.0 For j from 0 to h (h == recId here and the loop is outside this function)
        // 1.1 Let x = r + jn
        BigInteger n = CURVE.getN(); // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        // 1.2. Convert the integer x to an octet string X of length mlen using the
        // conversion routine
        // specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        // 1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve
        // point R using the
        // conversion routine specified in Section 2.3.4. If this conversion routine
        // outputs “invalid”, then
        // do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public
        // key.
        ECCurve.Fp curve = (ECCurve.Fp) CURVE.getCurve();
        BigInteger prime = curve.getQ(); // Bouncy Castle is not consistent about the letter it uses for the prime.
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place
            // modulo Q.
            return null;
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as
        // there are two possibilities.
        // So it's encoded in the recId.
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        // 1.4. If nR != point at infinity, then do another iteration of Step 1 (callers
        // responsibility).
        if (!R.multiply(n).isInfinity())
            return null;
        // 1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = new BigInteger(1, messageHash);
        // 1.6. For k from 1 to 2 do the following. (loop is outside this function via
        // iterating recId)
        // 1.6.1. Compute a candidate public key as:
        // Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the
        // following:
        // Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0
        // (mod n). In the above equation
        // ** is point multiplication and + is point addition (the EC group operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the
        // mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
        // result sanity check: point must not be at infinity
        if (q.isInfinity())
            return null;
        return q.getEncoded(/* compressed */ false);
    }

    /**
     * Decompress a compressed public key (x co-ord and low-bit of y-coord).
     */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    private static void check(boolean test, String message) {
        if (!test) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Groups the two components that make up a signature, and provides a way to
     * encode to Base64 form, which is how ECDSA signatures are represented when
     * embedded in other data structures in the Ethereum protocol. The raw
     * components can be useful for doing further EC maths on them.
     */
    public static class ECDSASignature {
        /**
         * The two components of the signature.
         */
        public final BigInteger r, s;
        public byte v;

        public ECDSASignature(BigInteger r, BigInteger s) {
            this.r = r;
            this.s = s;
        }

        public static ECDSASignature fromComponents(byte[] r, byte[] s, byte v) {
            ECDSASignature sig = new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
            sig.v = v;
            return sig;
        }

        public boolean validateComponents() {
            if (v != 27 && v != 28)
                return false;

            if (isLessThan(r, BigInteger.ONE) || !isLessThan(r, SECP256K1N)) {
                return false;
            }

            if (isLessThan(s, BigInteger.ONE) || !isLessThan(s, SECP256K1N)) {
                return false;
            }

            return true;
        }
    }

    /**
     * This is the generic Signature exception.
     */
    public static class SignatureException extends GeneralSecurityException {

        public SignatureException(String msg) {
            super(msg);
        }
    }
}