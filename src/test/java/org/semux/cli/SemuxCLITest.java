/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static uk.org.lidalia.slf4jtest.LoggingEvent.info;

import java.security.KeyPair;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.util.SystemUtil;

import com.google.common.collect.ImmutableList;

import net.i2p.crypto.eddsa.KeyPairGenerator;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SystemUtil.class, Kernel.class, SemuxCLI.class })
public class SemuxCLITest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    TestLogger logger = TestLoggerFactory.getTestLogger(SemuxCLI.class);

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
        semuxCLI.start(new String[] { "--help" });
        verify(semuxCLI).printHelp();
    }

    @Test
    public void testVersion() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        semuxCLI.start(new String[] { "--version" });
        verify(semuxCLI).printVersion();
    }

    @Test
    public void testStartKernelWithEmptyWallet() throws Exception {
        logger.setEnabledLevels(Level.INFO);
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

        // mock new account
        EdDSA newAccount = new EdDSA();
        PowerMockito.whenNew(EdDSA.class).withAnyArguments().thenReturn(newAccount);

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

        // assert outputs
        ImmutableList<LoggingEvent> logs = logger.getLoggingEvents();
        assertThat(logs, hasItem(info(SemuxCLI.MSG_START_KERNEL_NEW_ACCOUNT_CREATED, newAccount.toAddressString())));
    }

    @Test
    public void testAccountActionList() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        Mockito.doNothing().when(semuxCLI).listAccounts();
        semuxCLI.start(new String[] { "--account", "list" });
        verify(semuxCLI).listAccounts();
    }

    @Test
    public void testAccountActionCreate() throws ParseException {
        SemuxCLI semuxCLI = spy(new SemuxCLI());
        Mockito.doNothing().when(semuxCLI).createAccount();
        semuxCLI.start(new String[] { "--account", "create" });
        verify(semuxCLI).createAccount();
    }

    @Test
    public void testCreateAccount() throws Exception {
        logger.setEnabledLevels(Level.INFO);
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock account
        EdDSA newAccount = new EdDSA();
        PowerMockito.whenNew(EdDSA.class).withAnyArguments().thenReturn(newAccount);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.createAccount();

        // verification
        verify(wallet).addAccount(any(EdDSA.class));
        verify(wallet).flush();

        // assert outputs
        ImmutableList<LoggingEvent> logs = logger.getLoggingEvents();
        assertThat(logs, hasItem(info(SemuxCLI.MSG_NEW_ACCOUNT_CREATED)));
        assertThat(logs, hasItem(info(SemuxCLI.MSG_ADDRESS, newAccount.toAddressString())));
        assertThat(logs, hasItem(info(SemuxCLI.MSG_PUBLIC_KEY, Hex.encode(newAccount.getPublicKey()))));
        assertThat(logs, hasItem(info(SemuxCLI.MSG_PRIVATE_KEY, Hex.encode(newAccount.getPrivateKey()))));
    }

    @Test
    public void testListAccounts() throws ParseException {
        logger.setEnabledLevels(Level.INFO);
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock accounts
        List<EdDSA> accounts = new ArrayList<>();
        EdDSA account = new EdDSA();
        accounts.add(account);

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

        // assert outputs
        ImmutableList<LoggingEvent> logs = logger.getLoggingEvents();
        assertThat(logs, hasItem(info(SemuxCLI.MSG_ACCOUNT, 0, account.toAddressString())));
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
    public void testDumpPrivateKeyNotFound() throws Exception {
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
        doCallRealMethod().when(SystemUtil.class, "exit", Mockito.any(Integer.class));

        // expect System.exit(1)
        exit.expectSystemExitWithStatus(1);

        // execution
        semuxCLI.dumpPrivateKey(address);
    }

    @Test
    public void testImportPrivateKeyExisted() throws Exception {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock private key
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        String key = Hex.encode(keypair.getPrivate().getEncoded());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(false);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");
        doCallRealMethod().when(SystemUtil.class, "exit", Mockito.any(Integer.class));

        // expectation
        exit.expectSystemExitWithStatus(1);

        // execution
        semuxCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKeyFailedToFlushWalletFile() throws Exception {
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock private key
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        String key = Hex.encode(keypair.getPrivate().getEncoded());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(false);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");
        doCallRealMethod().when(SystemUtil.class, "exit", Mockito.any(Integer.class));

        // expectation
        exit.expectSystemExitWithStatus(2);

        // execution
        semuxCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKey() {
        logger.setEnabledLevels(Level.INFO);
        SemuxCLI semuxCLI = spy(new SemuxCLI());

        // mock private key
        final String key = "302e020100300506032b657004220420bd2f24b259aac4bfce3792c31d0f62a7f28b439c3e4feb97050efe5fe254f2af";

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(EdDSA.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);

        // mock SystemUtil
        mockStatic(SystemUtil.class);
        when(SystemUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.importPrivateKey(key);

        // assertions
        ImmutableList<LoggingEvent> logs = logger.getLoggingEvents();
        assertThat(logs, hasItem(info(SemuxCLI.MSG_PRIVATE_KEY_IMPORTED)));
        assertThat(logs, hasItem(info(SemuxCLI.MSG_ADDRESS, "0680a919c78faa59b127014b6181979ae0a62dbd")));
        assertThat(logs, hasItem(info(SemuxCLI.MSG_PRIVATE_KEY, key)));
    }
}