/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.util.ArrayList;
import java.util.List;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.crypto.Key;
import org.semux.gui.AddressBookEntry;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class AddressBookDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testListAddressBook() {
        Key account1 = new Key(), account2 = new Key();

        List<AddressBookEntry> entries = new ArrayList<>();
        entries.add(new AddressBookEntry("address1", account1.toAddressString()));
        entries.add(new AddressBookEntry("address2", account2.toAddressString()));

        AddressBookDialogTestApplication application = GuiActionRunner
                .execute(() -> new AddressBookDialogTestApplication(walletModel, kernelRule1.getKernel(), entries));

        FrameFixture window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();
        window.dialog("AddressBookDialog").requireVisible()
                .table().requireVisible().requireRowCount(2).requireContents(new String[][] {
                        { "address1", account1.toAddressString() },
                        { "address2", account2.toAddressString() }
                });
    }

    @Override
    protected void onSetUp() {

    }
}
