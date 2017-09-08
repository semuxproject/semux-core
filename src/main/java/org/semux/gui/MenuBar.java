/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.utils.IOUtil;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;

    public MenuBar(JFrame frame) {
        this.frame = frame;

        JMenu menuFile = new JMenu("File");
        this.add(menuFile);

        JMenuItem itemBackup = new JMenuItem("Backup wallet");
        itemBackup.setActionCommand(Action.BACKUP_WALLET.name());
        itemBackup.addActionListener(this);
        menuFile.add(itemBackup);

        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.setActionCommand(Action.EXIT.name());
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu menuHelp = new JMenu("Help");
        this.add(menuHelp);

        JMenuItem itemAbout = new JMenuItem("About");
        itemAbout.setActionCommand(Action.ABOUT.name());
        itemAbout.addActionListener(this);
        menuHelp.add(itemAbout);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case BACKUP_WALLET: {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Please select a folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = chooser.showOpenDialog(this);

            if (ret == JFileChooser.APPROVE_OPTION) {
                File dst = new File(chooser.getSelectedFile(), "wallet.data");
                File src = Kernel.getInstance().getWallet().getFile();
                try {
                    IOUtil.copyFile(src, dst);
                    JOptionPane.showMessageDialog(frame, "Backup file is saved at " + dst.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to save backup file");
                }
            }
            break;
        }
        case EXIT: {
            System.exit(0);
            break;
        }
        case ABOUT: {
            JOptionPane.showMessageDialog(frame, Config.CLIENT_FULL_NAME);
            break;
        }
        default:
            break;
        }
    }
}
