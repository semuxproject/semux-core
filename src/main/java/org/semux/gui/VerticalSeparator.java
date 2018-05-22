/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

public class VerticalSeparator extends JComponent {

    private static final long serialVersionUID = -5802537037684892071L;

    private final Color leftColor;
    private final Color rightColor;

    public VerticalSeparator() {
        this.leftColor = Color.GRAY;
        this.rightColor = Color.WHITE;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(leftColor);
        g.drawLine(0, 0, 0, getHeight());
        g.setColor(rightColor);
        g.drawLine(1, 0, 1, getHeight());
    }
}
