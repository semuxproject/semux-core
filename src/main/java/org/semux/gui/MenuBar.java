/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.semux.core.Wallet;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.dialog.ChangePasswordDialog;
import org.semux.gui.dialog.ConsoleDialog;
import org.semux.gui.dialog.ExportPrivateKeyDialog;
import org.semux.gui.dialog.InputDialog;
import org.semux.message.GuiMessages;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuBar extends JMenuBar implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MenuBar.class);
    public static final String HELP_URL = "https://github.com/semuxproject/semux/wiki";

    private transient SemuxGui gui;
    private JFrame frame;

    public MenuBar(SemuxGui gui, JFrame frame) {
        this.gui = gui;
        this.frame = frame;

        JMenu menuFile = new JMenu(GuiMessages.get("File"));
        this.add(menuFile);

        JMenuItem itemExit = new JMenuItem(GuiMessages.get("Exit"));
        itemExit.setName("itemExit");
        itemExit.setMnemonic(KeyEvent.VK_X);
        itemExit.setActionCommand(Action.EXIT.name());
        itemExit.addActionListener(this);
        menuFile.add(itemExit);

        JMenu menuWallet = new JMenu(GuiMessages.get("Wallet"));
        this.add(menuWallet);

        JMenuItem itemChangePassword = new JMenuItem(GuiMessages.get("ChangePassword"));
        itemChangePassword.setName("itemChangePassword");
        itemChangePassword.setActionCommand(Action.CHANGE_PASSWORD.name());
        itemChangePassword.addActionListener(this);
        menuWallet.add(itemChangePassword);

        menuWallet.addSeparator();

        JMenuItem itemRecover = new JMenuItem(GuiMessages.get("RecoverWallet"));
        itemRecover.setName("itemRecover");
        itemRecover.setActionCommand(Action.RECOVER_ACCOUNTS.name());
        itemRecover.addActionListener(this);
        menuWallet.add(itemRecover);

        JMenuItem itemBackupWallet = new JMenuItem(GuiMessages.get("BackupWallet"));
        itemBackupWallet.setName("itemBackupWallet");
        itemBackupWallet.setActionCommand(Action.BACKUP_WALLET.name());
        itemBackupWallet.addActionListener(this);
        menuWallet.add(itemBackupWallet);

        menuWallet.addSeparator();

        JMenuItem itemImportPrivateKey = new JMenuItem(GuiMessages.get("ImportPrivateKey"));
        itemImportPrivateKey.setName("itemImportPrivateKey");
        itemImportPrivateKey.setActionCommand(Action.IMPORT_PRIVATE_KEY.name());
        itemImportPrivateKey.addActionListener(this);
        menuWallet.add(itemImportPrivateKey);

        JMenuItem itemExportPrivateKey = new JMenuItem(GuiMessages.get("ExportPrivateKey"));
        itemExportPrivateKey.setName("itemExportPrivateKey");
        itemExportPrivateKey.setActionCommand(Action.EXPORT_PRIVATE_KEY.name());
        itemExportPrivateKey.addActionListener(this);
        menuWallet.add(itemExportPrivateKey);

        JMenu menuHelp = new JMenu(GuiMessages.get("Help"));
        this.add(menuHelp);

        JMenuItem itemAbout = new JMenuItem(GuiMessages.get("About"));
        itemAbout.setName("itemAbout");
        itemAbout.setActionCommand(Action.ABOUT.name());
        itemAbout.addActionListener(this);
        menuHelp.add(itemAbout);

        JMenuItem itemConsole = new JMenuItem(GuiMessages.get("Console"));
        itemConsole.setName("itemConsole");
        itemConsole.setActionCommand(Action.CONSOLE.name());
        itemConsole.addActionListener(this);
        menuHelp.add(itemConsole);

        JMenuItem itemHelp = new JMenuItem(GuiMessages.get("Help"));
        itemHelp.setName("itemHelp");
        itemHelp.setActionCommand(Action.HELP.name());
        itemHelp.addActionListener(this);
        menuHelp.add(itemHelp);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case EXIT:
            SystemUtil.exitAsync(SystemUtil.Code.OK);
            break;
        case RECOVER_ACCOUNTS:
            recoverAccounts();
            break;
        case BACKUP_WALLET:
            backupWallet();
            break;
        case IMPORT_PRIVATE_KEY:
            importPrivateKey();
            break;
        case EXPORT_PRIVATE_KEY:
            exportPrivateKey();
            break;
        case CHANGE_PASSWORD:
            changePassword();
            break;
        case ABOUT:
            about();
            break;
        case CONSOLE:
            console();
            break;
        case HELP:
            help();
            break;
        default:
            break;
        }
    }

    /**
     * Shows the change password dialog.
     */
    protected void changePassword() {
        if (showErrorIfLocked()) {
            return;
        }

        ChangePasswordDialog d = new ChangePasswordDialog(gui, frame);
        d.setVisible(true);
    }

    /**
     * Recovers accounts from backup file.
     */
    protected void recoverAccounts() {
        if (showErrorIfLocked()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(GuiMessages.get("WalletBinaryFormat"), "data"));

        int ret = chooser.showOpenDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String pwd = new InputDialog(frame, GuiMessages.get("EnterPassword"), true).showAndGet();

            if (pwd != null) {
                Wallet w = new Wallet(file);
                if (!w.unlock(pwd)) {
                    JOptionPane.showMessageDialog(frame, GuiMessages.get("UnlockFailed"));
                    return;
                }

                Wallet wallet = gui.getKernel().getWallet();

                int n = wallet.addWallet(w);
                if (!wallet.flush()) {
                    JOptionPane.showMessageDialog(frame, GuiMessages.get("WalletSaveFailed"));
                } else {
                    JOptionPane.showMessageDialog(frame, GuiMessages.get("ImportSuccess", n));
                }

                gui.updateModel();
            }
        }
    }

    /**
     * Backup the wallet.
     */
    protected void backupWallet() {
        if (showErrorIfLocked()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File("wallet.data"));
        chooser.setFileFilter(new FileNameExtensionFilter(GuiMessages.get("WalletBinaryFormat"), "data"));

        int ret = chooser.showSaveDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File dst = chooser.getSelectedFile();
            if (dst.exists()) {
                int answer = JOptionPane.showConfirmDialog(frame, GuiMessages.get("BackupFileExists", dst.getName()));
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            File src = gui.getKernel().getWallet().getFile();
            try {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(frame, GuiMessages.get("WalletSavedAt", dst.getAbsolutePath()));
            } catch (IOException ex) {
                logger.warn("Failed to save backup file", ex);
                JOptionPane.showMessageDialog(frame, GuiMessages.get("SaveBackupFailed"));
            }
        }
    }

    /**
     * Imports private key into this wallet.
     */
    protected void importPrivateKey() {
        if (showErrorIfLocked()) {
            return;
        }

        InputDialog inputDialog = new InputDialog(frame, GuiMessages.get("EnterPrivateKey"), false);
        inputDialog.setTitle(GuiMessages.get("ImportPrivateKey"));
        String pk = inputDialog.showAndGet();
        if (pk != null) {
            try {
                Wallet wallet = gui.getKernel().getWallet();
                Key account = new Key(Hex.decode0x(pk));
                if (wallet.addAccount(account)) {
                    wallet.flush();
                    JOptionPane.showMessageDialog(frame, GuiMessages.get("PrivateKeyImportSuccess"));
                    gui.updateModel();
                } else {
                    JOptionPane.showMessageDialog(frame, GuiMessages.get("PrivateKeyAlreadyExists"));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, GuiMessages.get("PrivateKeyImportFailed"));
            }
        }
    }

    /**
     * Shows the export private key dialog.
     */
    protected void exportPrivateKey() {
        if (showErrorIfLocked()) {
            return;
        }

        ExportPrivateKeyDialog d = new ExportPrivateKeyDialog(gui, frame);
        d.setVisible(true);
    }

    /**
     * Shows the about dialog.
     */
    protected void about() {
        Icon icon = SwingUtil.loadImage("logo", 64, 64);
        JOptionPane.showMessageDialog(frame, gui.getKernel().getConfig().getClientId(),
                GuiMessages.get("About"), JOptionPane.INFORMATION_MESSAGE, icon);
    }

    private void help() {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(HELP_URL));
            } catch (IOException | URISyntaxException e) {
                logger.error("Unable to parse help url " + HELP_URL);
            }
        }
    }

    /**
     * Shows the console
     */
    private void console() {
        if (showErrorIfLocked()) {
            return;
        }
        ConsoleDialog d = new ConsoleDialog(gui, frame);
        d.setVisible(true);
    }

    /**
     * Displays an error message if the wallet is locked.
     * 
     * @return whether the wallet is locked
     */
    protected boolean showErrorIfLocked() {
        Wallet wallet = gui.getKernel().getWallet();

        if (wallet.isLocked()) {
            JOptionPane.showMessageDialog(frame, GuiMessages.get("WalletLocked"));
            return true;
        }

        return false;
    }
}
