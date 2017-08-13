/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SemuxPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JFrame frame;

    public SemuxPanel(JFrame frame) {
        this.frame = frame;

        MenuBar menuBar = new MenuBar();
        ToolBar toolBar = new ToolBar();

        this.frame.setJMenuBar(menuBar.createMenuBar(frame));
        this.frame.add(toolBar.createToolbar(), BorderLayout.NORTH);
    }

}
