/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.crypto.EdDSA;
import org.semux.gui.AddressBook;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.class)
public class AddressBookDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Mock
    AddressBook addressBook;

    @Test
    public void testListAddressBook() {
        EdDSA account1 = new EdDSA(), account2 = new EdDSA();
        when(addressBook.list()).thenReturn(Arrays.asList(
                new AddressBook.Entry("address1", account1.toAddressString()),
                new AddressBook.Entry("address2", account2.toAddressString())));
        when(walletModel.getAddressBook()).thenReturn(addressBook);

        AddressBookDialogTestApplication application = GuiActionRunner
                .execute(() -> new AddressBookDialogTestApplication(walletModel, kernelRule1.getKernel()));

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
