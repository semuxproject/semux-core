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

import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.dialog.ChangePasswordDialog;
import org.semux.gui.dialog.ExportPrivateKeyDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.message.GUIMessages;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MenuBar.class);

    private transient SemuxGUI gui;
    private JFrame frame;

    public MenuBar(SemuxGUI gui, JFrame frame) {
        this.gui = gui;
        this.frame = frame;

        JMenu menuFile = new JMenu(GUIMessages.get("File"));
        this.add(menuFile);

        JMenuItem itemExit = new JMenuItem(GUIMessages.get("Exit"));
        itemExit.setActionCommand(Action.EXIT.name());
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu menuWallet = new JMenu(GUIMessages.get("Wallet"));
        this.add(menuWallet);

        JMenuItem itemChangePwd = new JMenuItem(GUIMessages.get("ChangePassword"));
        itemChangePwd.setActionCommand(Action.CHANGE_PASSWORD.name());
        itemChangePwd.addActionListener(this);
        menuWallet.add(itemChangePwd);

        menuWallet.addSeparator();

        JMenuItem itemImport = new JMenuItem(GUIMessages.get("RecoverWallet"));
        itemImport.setActionCommand(Action.RECOVER_ACCOUNTS.name());
        itemImport.addActionListener(this);
        menuWallet.add(itemImport);

        JMenuItem itemBackup = new JMenuItem(GUIMessages.get("BackupWallet"));
        itemBackup.setActionCommand(Action.BACKUP_WALLET.name());
        itemBackup.addActionListener(this);
        menuWallet.add(itemBackup);

        menuWallet.addSeparator();

        JMenuItem itemImportPrivKey = new JMenuItem(GUIMessages.get("ImportPrivateKey"));
        itemImportPrivKey.setActionCommand(Action.IMPORT_PRIVATE_KEY.name());
        itemImportPrivKey.addActionListener(this);
        menuWallet.add(itemImportPrivKey);

        JMenuItem itemExportPrivKey = new JMenuItem(GUIMessages.get("ExportPrivateKey"));
        itemExportPrivKey.setActionCommand(Action.EXPORT_PRIVATE_KEY.name());
        itemExportPrivKey.addActionListener(this);
        menuWallet.add(itemExportPrivKey);

        JMenu menuHelp = new JMenu(GUIMessages.get("Help"));
        this.add(menuHelp);

        JMenuItem itemAbout = new JMenuItem(GUIMessages.get("About"));
        itemAbout.setActionCommand(Action.ABOUT.name());
        itemAbout.addActionListener(this);
        menuHelp.add(itemAbout);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case RECOVER_ACCOUNTS: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter(GUIMessages.get("WalletBinaryFormat"), "data"));

            int ret = chooser.showOpenDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String password = new InputDialog(frame, GUIMessages.get("EnterPassword"), true).getInput();

                if (password != null) {
                    Wallet w = new Wallet(file);
                    if (!w.unlock(password)) {
                        JOptionPane.showMessageDialog(frame, GUIMessages.get("UnlockFailed"));
                        break;
                    }

                    Wallet wallet = gui.getKernel().getWallet();
                    int n = wallet.addAccounts(w.getAccounts());
                    wallet.flush();
                    JOptionPane.showMessageDialog(frame, GUIMessages.get("ImportSuccess", n));
                    gui.getModel().fireUpdateEvent();
                }
            }

            break;
        }
        case BACKUP_WALLET: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setSelectedFile(new File("wallet.data"));
            chooser.setFileFilter(new FileNameExtensionFilter(GUIMessages.get("WalletBinaryFormat"), "data"));

            int ret = chooser.showSaveDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File dst = chooser.getSelectedFile();
                if (dst.exists()) {
                    int answer = JOptionPane.showConfirmDialog(frame,
                            GUIMessages.get("BackupFileExists", dst.getName()));
                    if (answer != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                File src = gui.getKernel().getWallet().getFile();
                try {
                    Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(frame, GUIMessages.get("WalletSavedAt", dst.getAbsolutePath()));
                } catch (IOException ex) {
                    logger.warn("Failed to save backup file", ex);
                    JOptionPane.showMessageDialog(frame, GUIMessages.get("SaveBackupFailed"));
                }
            }
            break;
        }
        case IMPORT_PRIVATE_KEY: {
            InputDialog dialog = new InputDialog(frame, GUIMessages.get("EnterPrivateKey"), false);
            String pk = dialog.getInput();
            if (pk != null) {
                try {
                    Wallet wallet = gui.getKernel().getWallet();
                    EdDSA account = new EdDSA(Hex.parse(pk));
                    if (wallet.addAccount(account)) {
                        wallet.flush();
                        JOptionPane.showMessageDialog(frame, GUIMessages.get("PrivateKeyImportSuccess"));
                        gui.getModel().fireUpdateEvent();
                    } else {
                        JOptionPane.showMessageDialog(frame, GUIMessages.get("PrivateKeyAlreadyExists"));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, GUIMessages.get("PrivateKeyImportFailed"));
                }
            }
            break;
        }
        case EXPORT_PRIVATE_KEY: {
            ExportPrivateKeyDialog d = new ExportPrivateKeyDialog(gui, frame);
            d.setVisible(true);
            break;
        }
        case CHANGE_PASSWORD: {
            ChangePasswordDialog d = new ChangePasswordDialog(gui, frame);
            d.setVisible(true);
            break;
        }
        case EXIT: {
            SystemUtil.exitAsync(0);
            break;
        }
        case ABOUT: {
            JOptionPane.showMessageDialog(frame, gui.getKernel().getConfig().getClientId());
            break;
        }
        default:
            break;
        }
    }
}
