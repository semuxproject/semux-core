/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;

import org.semux.gui.model.WalletModel;

public class SplashScreenTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 7961392121592436000L;

    protected SplashScreen splashScreen;

    protected WalletModel walletModel;

    SplashScreenTestApplication() {
        super();
        this.setMinimumSize(new Dimension(960, 600));
        splashScreen = new SplashScreen(walletModel = new WalletModel());
    }
}