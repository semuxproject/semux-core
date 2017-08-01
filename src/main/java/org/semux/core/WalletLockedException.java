package org.semux.core;

public class WalletLockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WalletLockedException() {
        super("Wallet is locked");
    }
}
