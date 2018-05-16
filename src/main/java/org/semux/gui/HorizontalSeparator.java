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

public class HorizontalSeparator extends JComponent {

    private static final long serialVersionUID = -5330205221592957711L;

    private final Color leftColor;
    private final Color rightColor;

    public HorizontalSeparator() {
        this.leftColor = Color.GRAY;
        this.rightColor = Color.WHITE;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(leftColor);
        g.drawLine(0, 0, getWidth(), 0);
        g.setColor(rightColor);
        g.drawLine(0, 1, getWidth(), 1);
    }
}
