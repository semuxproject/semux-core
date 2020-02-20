/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;

public class SplashScreenTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 7961392121592436000L;

    protected SplashScreen splashScreen;

    SplashScreenTestApplication() {
        super();
        this.setMinimumSize(new Dimension(960, 600));
        splashScreen = new SplashScreen();
    }
}