/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import javax.swing.text.JTextComponent;

/**
 * Placeholder of a Swing text component based on ${@link TextPrompt}.
 */
public class PlaceHolder extends TextPrompt {

    private static final long serialVersionUID = -1350764114359129512L;

    public PlaceHolder(String text, JTextComponent component) {
        super(text, component);
        changeAlpha(0.5f);
    }
}