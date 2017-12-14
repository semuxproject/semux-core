/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import javax.swing.JFrame;

public class BaseTestApplication extends JFrame {

    private static final long serialVersionUID = 1L;

    public BaseTestApplication() {
        super();
        setTitle(getClass().getCanonicalName());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public BaseTestApplication(String name) {
        this();
        setName(name);
    }
}
