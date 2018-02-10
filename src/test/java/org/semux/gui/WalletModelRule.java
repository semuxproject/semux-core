/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomUtils;
import org.junit.rules.ExternalResource;
import org.mockito.Mockito;
import org.semux.core.Unit;
import org.semux.core.state.Account;
import org.semux.crypto.Key;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;

public class WalletModelRule extends ExternalResource {

    public final Key key;

    public Account account;

    public WalletAccount walletAccount;

    public WalletModel walletModel;

    final int availableSEM;

    final int lockedSEM;

    public WalletModelRule(int availableSEM, int lockedSEM) {
        this.key = new Key();
        this.availableSEM = availableSEM;
        this.lockedSEM = lockedSEM;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        account = new Account(
                key.toAddress(),
                availableSEM * Unit.SEM,
                lockedSEM * Unit.SEM,
                RandomUtils.nextInt(1, 100));

        walletAccount = new WalletAccount(key, account, Optional.of("test account"));
        List<WalletAccount> accountList = Collections.singletonList(walletAccount);
        walletModel = mock(WalletModel.class);
        when(walletModel.getAccounts()).thenReturn(accountList);
    }

    @Override
    protected void after() {
        super.after();
        Mockito.reset(walletModel);
    }
}
