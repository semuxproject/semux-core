/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

public class StatusBarTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    protected StatusBar statusBar;

    StatusBarTestApplication() {
        super();
        this.setMinimumSize(new Dimension(960, 600));
        statusBar = new StatusBar(this);
        this.add(statusBar, BorderLayout.SOUTH);
        getContentPane().add(statusBar);
    }
}