/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32;

import org.semux.core.bip32.extern.Hex;
import org.semux.core.bip32.wallet.HdAddress;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

public class VectorTwoTest extends BaseVectorTest {
    public static final byte[] SEED = Hex.decode(
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542");

    public VectorTwoTest() throws UnsupportedEncodingException {
        super();
    }

    @Test
    public void testMasterNodePrivateKey() {
        String expected = "xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U";
        assertEquals(expected, Base58.encode(masterNode.getPrivateKey().getKey()));
    }

    @Test
    public void testMasterNodePublicKey() {
        String expected = "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB";
        assertEquals(expected, Base58.encode(masterNode.getPublicKey().getKey()));
    }

    @Test
    public void testChain0PrivateKey() {
        String expected = "xprv9vHkqa6EV4sPZHYqZznhT2NPtPCjKuDKGY38FBWLvgaDx45zo9WQRUT3dKYnjwih2yJD9mkrocEZXo1ex8G81dwSM1fwqWpWkeS3v86pgKt";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, false);
        assertEquals(expected, Base58.encode(chain.getPrivateKey().getKey()));
    }

    @Test
    public void testChain0PublicKey() {
        String expected = "xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, false);
        assertEquals(expected, Base58.encode(chain.getPublicKey().getKey()));
    }

    @Test
    public void testChain0_2147483647HPrivateKey() {
        String expected = "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9";
        HdAddress chain = hdKeyGenerator.getAddress(masterNode, 0, false);
        chain = hdKeyGenerator.getAddress(chain, 2147483647, true);
        assertEquals(expected, Base58.encode(chain.getPrivateKey().getKey()));
    }

    @Override
    protected byte[] getSeed() {
        return SEED;
    }
}
