/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.semux.core.bip32.extern.Hex;
import org.semux.core.bip32.wallet.HdAddress;

/**
 * Test vector from
 * https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
 */
public class VectorOneTest extends BaseVectorTest {

    public static final byte[] SEED = Hex.decode("000102030405060708090a0b0c0d0e0f");

    public VectorOneTest() throws UnsupportedEncodingException {
        super();
    }

    @Test
    public void testMasterNodePrivateKey() {
        String expected = "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi";
        assertEquals(expected, Base58.encode(masterNode.getPrivateKey().getKey()));
    }

    @Test
    public void testMasterNodePublicKey() {
        String expected = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8";
        assertEquals(expected, Base58.encode(masterNode.getPublicKey().getKey()));
    }

    @Test
    public void testChain0HPrivateKey() {
        // this is hardened
        String expected = "xprv9uHRZZhk6KAJC1avXpDAp4MDc3sQKNxDiPvvkX8Br5ngLNv1TxvUxt4cV1rGL5hj6KCesnDYUhd7oWgT11eZG7XnxHrnYeSvkzY7d2bhkJ7";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        assertEquals(expected, Base58.encode(chain.getPrivateKey().getKey()));
    }

    @Test
    public void testChain0HPublicKey() {
        String expected = "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        assertEquals(expected, Base58.encode(chain.getPublicKey().getKey()));
    }

    @Test
    public void testChain0H1PrivateKey() {
        String expected = "xprv9wTYmMFdV23N2TdNG573QoEsfRrWKQgWeibmLntzniatZvR9BmLnvSxqu53Kw1UmYPxLgboyZQaXwTCg8MSY3H2EU4pWcQDnRnrVA1xe8fs";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        chain = hdKeyGenerator.getAddress(chain, 1, false);
        assertEquals(expected, Base58.encode(chain.getPrivateKey().getKey()));
    }

    @Test
    public void testChain0H1PublicKey() {
        String expected = "xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        chain = hdKeyGenerator.getAddress(chain, 1, false);
        assertEquals(expected, Base58.encode(chain.getPublicKey().getKey()));
    }

    @Test
    public void testChain0H12HPrivateKey() {
        String expected = "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        chain = hdKeyGenerator.getAddress(chain, 1, false);
        chain = hdKeyGenerator.getAddress(chain, 2, true);
        assertEquals(expected, Base58.encode(chain.getPrivateKey().getKey()));
    }

    @Test
    public void testChain0H12HPublicKey() {
        String expected = "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, true);
        chain = hdKeyGenerator.getAddress(chain, 1, false);
        chain = hdKeyGenerator.getAddress(chain, 2, true);
        assertEquals(expected, Base58.encode(chain.getPublicKey().getKey()));
    }

    @Override
    protected byte[] getSeed() {
        return SEED;
    }
}
