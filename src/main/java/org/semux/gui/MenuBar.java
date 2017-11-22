/**
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.dialog.ChangePasswordDialog;
import org.semux.gui.dialog.ExportPrivateKeyDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MenuBar.class);

    private JFrame frame;

    public MenuBar(JFrame frame) {
        this.frame = frame;

        JMenu menuFile = new JMenu(MessagesUtil.get("File"));
        this.add(menuFile);

        JMenuItem itemExit = new JMenuItem(MessagesUtil.get("Exit"));
        itemExit.setActionCommand(Action.EXIT.name());
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu menuWallet = new JMenu(MessagesUtil.get("Wallet"));
        this.add(menuWallet);

        JMenuItem itemImport = new JMenuItem(MessagesUtil.get("ImportAccountsFromFile"));
        itemImport.setActionCommand(Action.IMPORT_ACCOUNTS.name());
        itemImport.addActionListener(this);
        menuWallet.add(itemImport);

        JMenuItem itemBackup = new JMenuItem(MessagesUtil.get("BackupWallet"));
        itemBackup.setActionCommand(Action.BACKUP_WALLET.name());
        itemBackup.addActionListener(this);
        menuWallet.add(itemBackup);

        JMenuItem itemChangePwd = new JMenuItem(MessagesUtil.get("ChangePassword"));
        itemChangePwd.setActionCommand(Action.CHANGE_PASSWORD.name());
        itemChangePwd.addActionListener(this);
        menuWallet.add(itemChangePwd);

        JMenuItem itemExportPrivKey = new JMenuItem(MessagesUtil.get("ExportPrivateKey"));
        itemExportPrivKey.setActionCommand(Action.EXPORT_PRIVATE_KEY.name());
        itemExportPrivKey.addActionListener(this);
        menuWallet.add(itemExportPrivKey);

        JMenuItem itemImportPrivKey = new JMenuItem(MessagesUtil.get("ImportPrivateKey"));
        itemImportPrivKey.setActionCommand(Action.IMPORT_PRIVATE_KEY.name());
        itemImportPrivKey.addActionListener(this);
        menuWallet.add(itemImportPrivKey);

        JMenu menuHelp = new JMenu(MessagesUtil.get("Help"));
        this.add(menuHelp);

        JMenuItem itemAbout = new JMenuItem(MessagesUtil.get("About"));
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
            chooser.setFileFilter(new FileNameExtensionFilter(MessagesUtil.get("WalletBinaryFormat"), "data"));

            int ret = chooser.showOpenDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String password = new InputDialog(frame, MessagesUtil.get("EnterPassword"), true).getInput();

                if (password != null) {
                    Wallet w = new Wallet(file);
                    if (!w.unlock(password)) {
                        JOptionPane.showMessageDialog(frame, MessagesUtil.get("UnlockFailed"));
                        break;
                    }

                    Wallet wallet = Kernel.getInstance().getWallet();
                    int n = wallet.addAccounts(w.getAccounts());
                    wallet.flush();
                    JOptionPane.showMessageDialog(frame, MessagesUtil.get("ImportSuccess", n));
                    SemuxGUI.fireUpdateEvent();
                }
            }

            break;
        }
        case BACKUP_WALLET: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setSelectedFile(new File("wallet.data"));
            chooser.setFileFilter(new FileNameExtensionFilter(MessagesUtil.get("WalletBinaryFormat"), "data"));

            int ret = chooser.showSaveDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File dst = chooser.getSelectedFile();
                if (dst.exists()) {
                    int answer = JOptionPane.showConfirmDialog(frame,
                            MessagesUtil.get("BackupFileExists", dst.getName()));
                    if (answer != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                File src = Kernel.getInstance().getWallet().getFile();
                try {
                    Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(frame, MessagesUtil.get("WalletSavedAt", dst.getAbsolutePath()));
                } catch (IOException ex) {
                    logger.warn("Failed to save backup file", ex);
                    JOptionPane.showMessageDialog(frame, MessagesUtil.get("SaveBackupFailed"));
                }
            }
            break;
        }
        case EXPORT_PRIVATE_KEY: {
            ExportPrivateKeyDialog d = new ExportPrivateKeyDialog(frame);
            d.setVisible(true);
            break;
        }
        case IMPORT_PRIVATE_KEY: {
            InputDialog dialog = new InputDialog(frame, MessagesUtil.get("EnterPrivateKey"), false);
            String pk = dialog.getInput();
            if (pk != null) {
                try {
                    Wallet wallet = Kernel.getInstance().getWallet();
                    EdDSA account = new EdDSA(Hex.parse(pk));
                    if (wallet.addAccount(account)) {
                        wallet.flush();
                        JOptionPane.showMessageDialog(frame, MessagesUtil.get("PrivateKeyImportSuccess"));
                        SemuxGUI.fireUpdateEvent();
                    } else {
                        JOptionPane.showMessageDialog(frame, MessagesUtil.get("PrivateKeyAlreadyExists"));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, MessagesUtil.get("PrivateKeyImportFailed"));
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
            SystemUtil.exitAsync(0);
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
