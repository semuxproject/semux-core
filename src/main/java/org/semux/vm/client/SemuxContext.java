/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;

public class SemuxContext implements PrecompiledContractContext {
    private DelegateState delegateState;
    private AccountState accountState;

    public SemuxContext(DelegateState delegateState, AccountState accountState) {
        this.delegateState = delegateState;
        this.accountState = accountState;
    }

    public DelegateState getDelegateState() {
        return delegateState;
    }

    public AccountState getAccountState() {
        return accountState;
    }

    public void commit() {
        delegateState.commit();
        accountState.commit();
    }

}
