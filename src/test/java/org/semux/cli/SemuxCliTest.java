/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.cli;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.semux.TestLoggingAppender.err;
import static org.semux.TestLoggingAppender.info;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.Kernel;
import org.semux.TestLoggingAppender;
import org.semux.config.DevnetConfig;
import org.semux.config.MainnetConfig;
import org.semux.config.TestnetConfig;
import org.semux.core.Wallet;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.message.CliMessages;
import org.semux.util.ConsoleUtil;
import org.semux.util.SystemUtil;
import org.semux.util.SystemUtil.OsName;

import net.i2p.crypto.eddsa.KeyPairGenerator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SystemUtil.class, ConsoleUtil.class, Kernel.class, SemuxCli.class })
@PowerMockIgnore({ "jdk.internal.*", "javax.management.*" })
public class SemuxCliTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @BeforeClass
    public static void beforeClass() {
        TestLoggingAppender.prepare(Level.INFO);
        Logger logger = (Logger) LogManager.getLogger(SemuxCli.class);
        logger.addAppender(TestLoggingAppender.createAppender("TestLoggingAppender", null, null, null));
    }

    @After
    public void tearDown() {
        TestLoggingAppender.clear();
    }

    @Test
    public void testMain() throws Exception {
        String[] args = { "arg1", "arg2" };

        SemuxCli semuxCLI = mock(SemuxCli.class);
        whenNew(SemuxCli.class).withAnyArguments().thenReturn(semuxCLI);

        SemuxCli.main(args);

        verify(semuxCLI).start(args);
    }

    @Test
    public void testLoadAndUnlockWalletWithWrongPassword() {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock(any())).thenReturn(false);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock password
        when(semuxCLI.getPassword()).thenReturn("password");

        // execution
        exit.expectSystemExitWithStatus(SystemUtil.Code.FAILED_TO_UNLOCK_WALLET);
        semuxCLI.loadAndUnlockWallet();

        // assertion
        assertThat(TestLoggingAppender.events(), hasItem(err(CliMessages.get("WrongPassword"))));
    }

    @Test
    public void testHelp() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());
        semuxCLI.start(new String[] { "--help" });
        verify(semuxCLI).printHelp();
    }

    @Test
    public void testVersion() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());
        semuxCLI.start(new String[] { "--version" });
        verify(semuxCLI).printVersion();
    }

    @Test
    public void testMainNetwork() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock accounts
        List<Key> accounts = new ArrayList<>();
        Key account = new Key();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword(any())).thenReturn("password");
        when(SystemUtil.getOsName()).thenReturn(OsName.LINUX);
        when(SystemUtil.getOsArch()).thenReturn("amd64");
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());
        semuxCLI.start(new String[] { "--network", "mainnet" });

        assertTrue(semuxCLI.getConfig() instanceof MainnetConfig);
    }

    @Test
    public void testMainNetworkNotSpecified() throws ParseException {

        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock accounts
        List<Key> accounts = new ArrayList<>();
        Key account = new Key();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        when(SystemUtil.getOsName()).thenReturn(OsName.LINUX);
        when(SystemUtil.getOsArch()).thenReturn("amd64");
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());
        semuxCLI.start();

        assertTrue(semuxCLI.getConfig() instanceof MainnetConfig);

    }

    @Test
    public void testTestNetwork() throws ParseException {

        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock accounts
        List<Key> accounts = new ArrayList<>();
        Key account = new Key();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        when(SystemUtil.getOsName()).thenReturn(OsName.LINUX);
        when(SystemUtil.getOsArch()).thenReturn("amd64");
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());
        semuxCLI.start(new String[] { "--network", "testnet" });

        assertTrue(semuxCLI.getConfig() instanceof TestnetConfig);

    }

    @Test
    public void testDevNetwork() throws ParseException {

        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock accounts
        List<Key> accounts = new ArrayList<>();
        Key account = new Key();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        when(SystemUtil.getOsName()).thenReturn(OsName.LINUX);
        when(SystemUtil.getOsArch()).thenReturn("amd64");
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());
        semuxCLI.start(new String[] { "--network", "devnet" });

        assertTrue(semuxCLI.getConfig() instanceof DevnetConfig);

    }

    @Test
    public void testStartKernelWithEmptyWallet() throws Exception {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.exists()).thenReturn(false);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        doReturn(new ArrayList<Key>(), // returns empty wallet
                Collections.singletonList(new Key()) // returns wallet with a newly created account
        ).when(wallet).getAccounts();
        when(wallet.addAccount(any(Key.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);

        // mock CLI
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());

        // mock new account
        Key newAccount = new Key();
        whenNew(Key.class).withAnyArguments().thenReturn(newAccount);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword(any())).thenReturn("oldpassword");
        when(SystemUtil.getOsName()).thenReturn(OsName.LINUX);
        when(SystemUtil.getOsArch()).thenReturn("amd64");

        // execution
        semuxCLI.start();

        // verifies that a new account is added the empty wallet
        verify(wallet).unlock("oldpassword");
        verify(wallet, times(2)).getAccounts();
        verify(wallet).addAccount(any(Key.class));
        verify(wallet, atLeastOnce()).flush();

        // verifies that kernel starts
        verify(semuxCLI).startKernel(any(), any(), any());

        // assert outputs
        List<LogEvent> logs = TestLoggingAppender.events();
        assertThat(logs, hasItem(info(CliMessages.get("NewAccountCreatedForAddress", newAccount.toAddressString()))));
    }

    @Test
    public void testStartKernelWithEmptyWalletInvalidNewPassword() {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.exists()).thenReturn(false);

        // mock CLI
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        doReturn(null).when(semuxCLI).startKernel(any(), any(), any());

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword(any())).thenReturn("a password").thenReturn("b password");

        // execution
        semuxCLI.start();

        // the kernel should not be started
        verify(semuxCLI, never()).startKernel(any(), any(), any());

        // the wallet should not be saved
        verify(wallet, never()).flush();
    }

    @Test
    public void testAccountActionList() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());
        Mockito.doNothing().when(semuxCLI).listAccounts();
        semuxCLI.start(new String[] { "--account", "list" });
        verify(semuxCLI).listAccounts();
    }

    @Test
    public void testAccountActionCreate() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());
        Mockito.doNothing().when(semuxCLI).createAccount();
        semuxCLI.start(new String[] { "--account", "create" });
        verify(semuxCLI).createAccount();
    }

    @Test
    public void testCreateAccount() throws Exception {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.addAccount(any(Key.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock account
        Key newAccount = new Key();
        whenNew(Key.class).withAnyArguments().thenReturn(newAccount);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.createAccount();

        // verification
        verify(wallet).addAccount(any(Key.class));
        verify(wallet).flush();

        // assert outputs
        List<LogEvent> logs = TestLoggingAppender.events();
        assertThat(logs, hasItem(info(CliMessages.get("NewAccountCreatedForAddress", newAccount.toAddressString()))));
        assertThat(logs, hasItem(info(CliMessages.get("PublicKey", Hex.encode(newAccount.getPublicKey())))));
        assertThat(logs, hasItem(info(CliMessages.get("PrivateKey", Hex.encode(newAccount.getPrivateKey())))));
    }

    @Test
    public void testListAccounts() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock accounts
        List<Key> accounts = new ArrayList<>();
        Key account = new Key();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.listAccounts();

        // verification
        verify(wallet).getAccounts();

        // assert outputs
        List<LogEvent> logs = TestLoggingAppender.events();
        assertThat(logs, hasItem(info(CliMessages.get("ListAccountItem", 0, account.toAddressString()))));
    }

    @Test
    public void testChangePassword() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        Mockito.when(ConsoleUtil.readPassword(anyString())).thenReturn("newpassword");

        // execution
        semuxCLI.changePassword();

        // verification
        verify(wallet).changePassword("newpassword");
        verify(wallet).flush();
    }

    @Test
    public void testChangePasswordIncorrectConfirmation() throws ParseException {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        Mockito.when(ConsoleUtil.readPassword(anyString())).thenReturn("newpassword").thenReturn("newpasswordconfirm");

        // execution
        semuxCLI.changePassword();

        // verification
        verify(wallet, never()).changePassword("newpassword");
        verify(wallet, never()).flush();
    }

    @Test
    public void testDumpPrivateKey() {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock account
        Key account = spy(new Key());
        String address = account.toAddressString();
        byte[] addressBytes = account.toAddress();

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(account);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.dumpPrivateKey(address);

        // verification
        verify(wallet).getAccount(addressBytes);
        verify(account).getPrivateKey();
        assertEquals(CliMessages.get("PrivateKeyIs", Hex.encode(account.getPrivateKey())),
                systemOutRule.getLog().trim());
    }

    @Test
    public void testDumpPrivateKeyNotFound() throws Exception {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock address
        String address = "c583b6ad1d1cccfc00ae9113db6408f022822b20";
        byte[] addressBytes = Hex.decode0x(address);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(null);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doCallRealMethod().when(SystemUtil.class, "exit", any(Integer.class));

        // expect System.exit(1)
        exit.expectSystemExitWithStatus(SystemUtil.Code.ACCOUNT_NOT_EXIST);

        // execution
        semuxCLI.dumpPrivateKey(address);
    }

    @Test
    public void testImportPrivateKeyExisted() throws Exception {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock private key
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        String key = Hex.encode(keypair.getPrivate().getEncoded());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(Key.class))).thenReturn(false);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doCallRealMethod().when(SystemUtil.class, "exit", any(Integer.class));

        // expectation
        exit.expectSystemExitWithStatus(SystemUtil.Code.ACCOUNT_ALREADY_EXISTS);

        // execution
        semuxCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKeyFailedToFlushWalletFile() throws Exception {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock private key
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        String key = Hex.encode(keypair.getPrivate().getEncoded());

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(Key.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(false);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");
        doCallRealMethod().when(SystemUtil.class, "exit", any(Integer.class));

        // expectation
        exit.expectSystemExitWithStatus(SystemUtil.Code.FAILED_TO_WRITE_WALLET_FILE);

        // execution
        semuxCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKey() {
        SemuxCli semuxCLI = spy(new SemuxCli());

        // mock private key
        final String key = "302e020100300506032b657004220420bd2f24b259aac4bfce3792c31d0f62a7f28b439c3e4feb97050efe5fe254f2af";

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(semuxCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(Key.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);

        // mock SystemUtil
        mockStatic(SystemUtil.class, ConsoleUtil.class);
        when(ConsoleUtil.readPassword()).thenReturn("oldpassword");

        // execution
        semuxCLI.importPrivateKey(key);

        // assertions
        List<LogEvent> logs = TestLoggingAppender.events();
        assertThat(logs, hasItem(info(CliMessages.get("PrivateKeyImportedSuccessfully"))));
        assertThat(logs, hasItem(info(CliMessages.get("Address", "0680a919c78faa59b127014b6181979ae0a62dbd"))));
        assertThat(logs, hasItem(info(CliMessages.get("PrivateKey", key))));
    }
}