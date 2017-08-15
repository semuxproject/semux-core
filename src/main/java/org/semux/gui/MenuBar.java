/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected JMenu menuFile;
    protected JMenu menuHelp;

    public MenuBar() {

        menuFile = new JMenu("File");
        this.add(menuFile);

        JMenuItem itemBackup = new JMenuItem("Backup wallet");
        itemBackup.addActionListener(this);
        menuFile.add(itemBackup);

        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu itemHelp = new JMenu("Help");
        this.add(itemHelp);

        JMenuItem itemAbout = new JMenuItem("About");
        itemAbout.addActionListener(this);
        itemHelp.add(itemAbout);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
