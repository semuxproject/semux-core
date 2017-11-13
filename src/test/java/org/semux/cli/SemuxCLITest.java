/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.SystemUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SystemUtil.class, Kernel.class, SemuxCLI.class })
public class SemuxCLITest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Test
    public void testMain() throws Exception {
        String[] args = { "arg1", "arg2" };

        SemuxCLI semuxCLI = mock(SemuxCLI.class);
        whenNew(SemuxCLI.class).withAnyArguments().thenReturn(semuxCLI);

        SemuxCLI.main(args);

        verify(semuxCLI).start(args);
    }

    @Test
    public void testHelp() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        semuxCLI.start(new String[] { "--cli", "--help" });
        verify(semuxCLI).printHelp();
    }

    @Test
    public void testVersion() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        semuxCLI.start(new String[] { "--cli", "--version" });
        verify(semuxCLI).printVersion();
    }

    @Test
    public void testStartKernelWithEmptyWallet() {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        doReturn(new ArrayList<EdDSA>(), // returns empty wallet
                Collections.singletonList(new EdDSA()) // returns wallet with a newly created account
        ).when(wallet).getAccounts();
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // mock Kernel
        mockStatic(Kernel.class);
        Kernel kernelMock = mock(Kernel.class);
        when(Kernel.getInstance()).thenReturn(kernelMock);

        // execution
        semuxCLI.startKernel();

        // verifies that a new account is added the empty wallet
        verify(wallet).unlock("oldpassword");
        verify(wallet, times(2)).getAccounts();
        verify(wallet).addAccount(any(EdDSA.class));
        verify(wallet).flush();

        // verifies that Kernel calls init and start
        verify(kernelMock).init(SemuxCLI.DEFAULT_DATA_DIR, wallet, 0);
        verify(kernelMock).start();
    }

    @Test
    public void testAccountActionList() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        Mockito.doNothing().when(semuxCLI).listAccounts();
        semuxCLI.start(new String[] { "--cli", "--account", "list" });
        verify(semuxCLI).listAccounts();
    }

    @Test
    public void testAccountActionCreate() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        Mockito.doNothing().when(semuxCLI).createAccount();
        semuxCLI.start(new String[] { "--cli", "--account", "create" });
        verify(semuxCLI).createAccount();
    }

    @Test
    public void testCreateAccount() throws Exception {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.createAccount();

        // verification
        verify(wallet).addAccount(any(EdDSA.class));
        verify(wallet).flush();
    }

    @Test
    public void testListAccounts() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock accounts
        List<EdDSA> accounts = new ArrayList<>();
        accounts.add(new EdDSA());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.listAccounts();

        // verification
        verify(wallet).getAccounts();
    }

    @Test
    public void testChangePassword() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");
        Mockito.when(SystemUtil.readPassword(SemuxCLI.MSG_ENTER_NEW_PASSWORD)).thenReturn("newpassword");

        // execution
        semuxCLI.changePassword();

        // verification
        verify(wallet).changePassword("newpassword");
        verify(wallet).flush();
    }

    @Test
    public void testDumpPrivateKey() {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock account
        EdDSA account = spy(new EdDSA());
        String address = account.toAddressString();
        byte[] addressBytes = account.toAddress();

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(account);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.dumpPrivateKey(address);

        // verification
        verify(wallet).getAccount(addressBytes);
        verify(account).getPrivateKey();
        assertEquals(Hex.encode(account.getPrivateKey()), systemOutRule.getLog().trim());
    }

    @Test
    public void testDumpPrivateKeyNotFound() {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock address
        String address = "c583b6ad1d1cccfc00ae9113db6408f022822b20";
        byte[] addressBytes = Hex.decode(address);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(null);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // expect System.exit(1)
        exit.expectSystemExitWithStatus(1);

        // execution
        semuxCLI.dumpPrivateKey(address);
    }
}
