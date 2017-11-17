/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.adressbook;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.semux.gui.model.AddressBook;
import org.semux.gui.model.SemuxAddress;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdressBookTest {

    private static final String TESTNAME = "testname";
    private static final String TESTADDRESS = "0fc1ea0571e639a8f83a9434d65326ea43b2fd00";
    private static final String TESTNAME2 = "two";
    private static final String TESTADDRESS2 = "0f1111111111111111111111111111111111111";

    @Test
    public void test1SemuxAddressInserting() {
        SemuxAddress sa = new SemuxAddress(TESTNAME, TESTADDRESS);
        System.out.println(sa.getAddress() + " + " + sa.getAddress());
        AddressBook.put(sa);

        SemuxAddress sa2 = new SemuxAddress(TESTNAME2, TESTADDRESS2);
        System.out.println(sa2.getAddress() + " + " + sa2.getAddress());
        AddressBook.put(sa2);

        SemuxAddress sr = AddressBook.getAddress(TESTNAME);
        Assert.assertEquals(TESTADDRESS, sr.getAddress());
        SemuxAddress sr2 = AddressBook.getAddress(TESTNAME2);
        Assert.assertEquals(TESTADDRESS2, sr2.getAddress());

        List<SemuxAddress> all = AddressBook.getAllAddresses();
        Optional<SemuxAddress> first = all.stream().filter(a -> a.getName().equals(TESTNAME)).findAny();
        Optional<SemuxAddress> second = all.stream().filter(a -> a.getName().equals(TESTNAME2)).findAny();

        Assert.assertTrue(first.isPresent());
        Assert.assertTrue(second.isPresent());

        Assert.assertEquals(TESTADDRESS, first.get().getAddress());
        Assert.assertEquals(TESTADDRESS2, second.get().getAddress());
    }

    @Test
    public void test2SemuxAddressDeletion() {
        SemuxAddress sr = AddressBook.getAddress(TESTNAME);
        Assert.assertEquals(TESTADDRESS, sr.getAddress());
        AddressBook.delete(TESTNAME);
        SemuxAddress notfound = AddressBook.getAddress(TESTNAME);
        Assert.assertNull(notfound);

        SemuxAddress sr2 = AddressBook.getAddress(TESTNAME2);
        Assert.assertEquals(TESTADDRESS2, sr2.getAddress());
        AddressBook.delete(TESTNAME2);
        SemuxAddress notfound2 = AddressBook.getAddress(TESTNAME2);
        Assert.assertNull(notfound2);
    }

    @Test
    public void test3SemuxAddressUpdate() {

        SemuxAddress sa = new SemuxAddress(TESTNAME, TESTADDRESS);
        System.out.println(sa.getAddress() + " + " + sa.getAddress());
        AddressBook.put(sa);

        SemuxAddress sr = AddressBook.getAddress(TESTNAME);
        Assert.assertEquals(TESTADDRESS, sr.getAddress());

        SemuxAddress su = new SemuxAddress(TESTNAME, TESTADDRESS2);
        AddressBook.put(su);
        Assert.assertEquals(TESTADDRESS2, su.getAddress());
        SemuxAddress su2 = AddressBook.getAddress(TESTNAME);
        Assert.assertEquals(TESTADDRESS2, su2.getAddress());

        AddressBook.delete(TESTNAME);
        SemuxAddress notfound = AddressBook.getAddress(TESTNAME);
        Assert.assertNull(notfound);
    }

    @Test
    public void testDeleteNotExistant() {
        AddressBook.delete("unknownEntry");
        SemuxAddress notfound = AddressBook.getAddress("unknownEntry");
        Assert.assertNull(notfound);
    }

    @Test
    public void testPutWithStrings() {
        String username = "TestMan";
        String address = "0fc1ea0571e639a8f83a9434d65326ea43b2fd00";
        AddressBook.put(username, address);
        SemuxAddress found = AddressBook.getAddress(username);
        Assert.assertEquals(username, found.getName());
        Assert.assertEquals(address, found.getAddress());
        AddressBook.delete(username);
        SemuxAddress notfound = AddressBook.getAddress(username);
        Assert.assertNull(notfound);
    }
}
