/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Timeout;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.KernelMock;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.gui.WalletModelRule;
import org.semux.gui.model.WalletDelegate;
import org.semux.message.GUIMessages;
import org.semux.rules.KernelRule;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class DelegatePanelTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    public WalletModelRule walletRule = new WalletModelRule(10000, 0);

    @Captor
    ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

    DelegatePanelTestApplication application;

    FrameFixture window;

    List<WalletDelegate> walletDelegates;

    @Mock
    private Blockchain blockchain;

    @Mock
    private DelegateState delegateState;

    private EdDSA delegateAccount1;

    @Mock
    private WalletDelegate delegate1;

    @Mock
    private BlockchainImpl.ValidatorStats delegateStats1;

    private EdDSA delegateAccount2;

    @Mock
    private WalletDelegate delegate2;

    @Mock
    private BlockchainImpl.ValidatorStats delegateStats2;

    @Mock
    PendingManager pendingManager;

    KernelMock kernelMock;

    @Override
    public void onSetUp() {
        // mock delegates
        walletDelegates = new ArrayList<>();

        delegateAccount1 = new EdDSA();
        when(delegate1.getNameString()).thenReturn("delegate 1");
        when(delegate1.getAddressString()).thenReturn(delegateAccount1.toAddressString());
        when(delegate1.getAddress()).thenReturn(delegateAccount1.toAddress());
        walletDelegates.add(delegate1);

        delegateAccount2 = new EdDSA();
        when(delegate2.getNameString()).thenReturn("delegate 2");
        when(delegate2.getAddressString()).thenReturn(delegateAccount2.toAddressString());
        when(delegate2.getAddress()).thenReturn(delegateAccount2.toAddress());
        walletDelegates.add(delegate2);

        when(walletRule.walletModel.getDelegates()).thenReturn(walletDelegates);

        // mock kernel
        kernelMock = spy(kernelRule1.getKernel());
        when(delegateState.getVote(any(), any())).thenReturn(0L);
        when(blockchain.getDelegateState()).thenReturn(delegateState);
        when(blockchain.getValidatorStats(delegate1.getAddress())).thenReturn(delegateStats1);
        when(blockchain.getValidatorStats(delegate2.getAddress())).thenReturn(delegateStats2);
        when(kernelMock.getBlockchain()).thenReturn(blockchain);
    }

    @Override
    public void onTearDown() {
        Mockito.reset(kernelMock);
    }

    @Test
    public void testSelectDelegate() {
        when(kernelMock.getPendingManager()).thenReturn(pendingManager);
        application = GuiActionRunner
                .execute(() -> new DelegatePanelTestApplication(walletRule.walletModel, kernelMock));
        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        // the initial label of selected delegate should be PleaseSelectDelegate
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("PleaseSelectDelegate"));

        // click on the first delegate
        window.table("DelegatesTable").cell("delegate 1").requireNotEditable().click();

        // the label of selected delegate should display the first delegate's name
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("SelectedDelegate", "delegate 1"));

        // click on the second delegate
        window.table("DelegatesTable").cell("delegate 2").requireNotEditable().click();

        // the label of selected delegate should display the second delegate's name
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("SelectedDelegate", "delegate 2"));
    }

    @Test
    public void testVoteSuccess() {
        testVote(new PendingManager.ProcessTransactionResult(1));
        window.optionPane(Timeout.timeout(1000)).requireTitle(GUIMessages.get("SuccessDialogTitle")).requireVisible();
    }

    @Test
    public void testVoteFailure() {
        testVote(new PendingManager.ProcessTransactionResult(0, TransactionResult.Error.INSUFFICIENT_AVAILABLE));
        window.optionPane(Timeout.timeout(1000)).requireTitle(GUIMessages.get("ErrorDialogTitle")).requireVisible();
    }

    private void testVote(PendingManager.ProcessTransactionResult mockResult) {
        // mock pending manager
        when(pendingManager.getNonce(any())).thenReturn(RandomUtils.nextLong());
        when(pendingManager.addTransactionSync(any())).thenReturn(mockResult);
        when(kernelMock.getPendingManager()).thenReturn(pendingManager);
        application = GuiActionRunner
                .execute(() -> new DelegatePanelTestApplication(walletRule.walletModel, kernelMock));
        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        // the initial label of selected delegate should be PleaseSelectDelegate
        window.label("SelectedDelegateLabel").requireText(GUIMessages.get("PleaseSelectDelegate"));

        // click on the first delegate
        window.table("DelegatesTable").cell("delegate 1").requireNotEditable().click();

        // fills number of votes
        window.textBox("textVote").requireEditable().setText("10");

        // click vote button
        window.button("btnVote").requireVisible().click();
    }

    @Test
    public void testDelegateSuccess() {
        testDelegate(new PendingManager.ProcessTransactionResult(1));
        window.optionPane(Timeout.timeout(1000)).requireTitle(GUIMessages.get("SuccessDialogTitle")).requireVisible();

        // verify added transaction
        verify(pendingManager).addTransactionSync(transactionArgumentCaptor.capture());
        Transaction tx = transactionArgumentCaptor.getValue();
        TestCase.assertEquals(TransactionType.DELEGATE, tx.getType());
        assertArrayEquals(walletRule.key.toAddress(), tx.getTo());
        TestCase.assertEquals(kernelMock.getConfig().minDelegateFee(), tx.getValue());
        TestCase.assertEquals(kernelMock.getConfig().minTransactionFee(), tx.getFee());
    }

    @Test
    public void testDelegateFailure() {
        testDelegate(new PendingManager.ProcessTransactionResult(0, TransactionResult.Error.INSUFFICIENT_AVAILABLE));
        window.optionPane(Timeout.timeout(1000)).requireTitle(GUIMessages.get("ErrorDialogTitle")).requireVisible();
    }

    private void testDelegate(PendingManager.ProcessTransactionResult mockResult) {
        // mock pending manager
        when(pendingManager.getNonce(any())).thenReturn(RandomUtils.nextLong());
        when(pendingManager.addTransactionSync(any())).thenReturn(mockResult);
        when(kernelMock.getPendingManager()).thenReturn(pendingManager);
        application = GuiActionRunner
                .execute(() -> new DelegatePanelTestApplication(walletRule.walletModel, kernelMock));
        window = new FrameFixture(robot(), application);
        window.show().requireVisible().moveToFront();

        // fills delegate name
        window.textBox("textName").requireEditable().setText("test_delegate");

        // click register button
        window.button("btnDelegate").requireVisible().click();

        // confirm
        window.optionPane(Timeout.timeout(1000)).requireTitle(GUIMessages.get("ConfirmDelegateRegistration"))
                .requireVisible()
                .yesButton().requireVisible().click();
    }
}
