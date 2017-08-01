/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.semux.gui.MenuBar;
import org.semux.gui.SemuxPannel;
import org.semux.gui.ToolBar;

/**
 * Graphic user interface.
 *
 */
public class GUI {

    private static String TITLE = "Semux Wallet";
    private static int WIDTH = 800;
    private static int HEIGHT = 600;

    private static void show() {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        double x = (d.getWidth() - WIDTH) / 2;
        double y = (d.getHeight() - HEIGHT) / 2;
        frame.setLocation((int) x, (int) y);

        MenuBar menuBar = new MenuBar();
        ToolBar toolBar = new ToolBar();

        frame.setJMenuBar(menuBar.createMenuBar(frame));
        frame.add(toolBar.createToolbar(), BorderLayout.NORTH);
        frame.add(new SemuxPannel(), BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            show();
        });
    }
}