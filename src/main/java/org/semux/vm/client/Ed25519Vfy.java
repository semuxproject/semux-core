/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.ethereum.vm.util.Pair;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Implementation of
 * https://github.com/ethereum/EIPs/blob/master/EIPS/eip-665.md
 */
public class Ed25519Vfy implements PrecompiledContract {
    private static final Logger logger = LoggerFactory.getLogger(Ed25519Vfy.class);

    @Override
    public long getGasForData(byte[] bytes) {
        return 2000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(PrecompiledContractContext context) {

        byte[] data = context.getInternalTransaction().getData();

        if (data == null || data.length != 128) {
            return SemuxPrecompiledContracts.failure;
        }

        /**
         * ED25519VFY takes as input 128 octets:
         *
         * message: the 32-octet message that was signed public key: the 32-octet
         * Ed25519 public key of the signer signature: the 64-octet Ed25519 signature
         */
        byte[] message = Arrays.copyOfRange(data, 0, 32);
        byte[] publicKey = Arrays.copyOfRange(data, 32, 64);
        byte[] signature = Arrays.copyOfRange(data, 64, 128);

        byte[] semSignature = Bytes.merge(signature, publicKey);

        boolean isValidSignature = false;
        try {
            Key.Signature sig = Key.Signature.fromBytes(semSignature);
            if (sig != null) {
                if (Key.verify(message, sig)) {
                    isValidSignature = true;
                }
            }
        } catch (Exception e) {
            logger.info("Exception while verifying signature", e);
        }

        return isValidSignature ? SemuxPrecompiledContracts.success : SemuxPrecompiledContracts.failure;
    }
}
