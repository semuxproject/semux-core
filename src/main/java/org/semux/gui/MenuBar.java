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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.gui.dialog.ChangePasswordDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.utils.IOUtil;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;

    public MenuBar(JFrame frame) {
        this.frame = frame;

        JMenu menuFile = new JMenu("File");
        this.add(menuFile);

        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.setActionCommand(Action.EXIT.name());
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu menuWallet = new JMenu("Wallet");
        this.add(menuWallet);

        JMenuItem itemImport = new JMenuItem("Import accounts from file");
        itemImport.setActionCommand(Action.IMPORT_ACCOUNTS.name());
        itemImport.addActionListener(this);
        menuWallet.add(itemImport);

        JMenuItem itemBackup = new JMenuItem("Backup wallet");
        itemBackup.setActionCommand(Action.BACKUP_WALLET.name());
        itemBackup.addActionListener(this);
        menuWallet.add(itemBackup);

        JMenuItem itemChangePwd = new JMenuItem("Change password");
        itemChangePwd.setActionCommand(Action.CHANGE_PASSWORD.name());
        itemChangePwd.addActionListener(this);
        menuWallet.add(itemChangePwd);

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
        case IMPORT_ACCOUNTS: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter("Wallet binary format", "data"));

            int ret = chooser.showOpenDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String password = new InputDialog(frame, "Please enter the password", true).getInput();

                if (password != null) {
                    Wallet w = new Wallet(file);
                    if (!w.unlock(password)) {
                        JOptionPane.showMessageDialog(frame, "Failed to unlock the wallet file!");
                        break;
                    }

                    Wallet wallet = Kernel.getInstance().getWallet();
                    int n = wallet.addAccounts(w.getAccounts());
                    wallet.flush();
                    JOptionPane.showMessageDialog(frame, "Success! " + n + " accounts were imported.");
                }
            }

            break;
        }
        case BACKUP_WALLET: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setSelectedFile(new File("wallet.data"));
            chooser.setFileFilter(new FileNameExtensionFilter("Wallet binary format", "data"));

            int ret = chooser.showSaveDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File dst = chooser.getSelectedFile();
                File src = Kernel.getInstance().getWallet().getFile();
                try {
                    IOUtil.copyFile(src, dst);
                    JOptionPane.showMessageDialog(frame, "Your wallet has been saved at " + dst.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to save backup file");
                }
            }
            break;
        }
        case CHANGE_PASSWORD: {
            ChangePasswordDialog d = new ChangePasswordDialog(frame);
            d.setVisible(true);
            break;
        }
        case EXIT: {
            System.exit(0);
            break;
        }
        case ABOUT: {
            JOptionPane.showMessageDialog(frame, Config.getClientId(true));
            break;
        }
        default:
            break;
        }
    }
}
