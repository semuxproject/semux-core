package org.semux.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.semux.Config;

public class MenuBar {
    protected Action backupAction;
    protected Action exitAction;
    protected Action aboutAction;
    protected JFrame frame;

    public JMenuBar createMenuBar(JFrame frame) {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        this.frame = frame;

        // Create the actions shared by the toolbar and menu.
        backupAction = new BackupAction("Backup Wallet", null, "Backup your wallet", new Integer(KeyEvent.VK_B));
        exitAction = new ExitAction("Exit", null, "Exit the wallet", new Integer(KeyEvent.VK_E));
        aboutAction = new AboutAction("About", null, "About", new Integer(KeyEvent.VK_A));

        // Create the menu bar.
        menuBar = new JMenuBar();

        // Build the first menu.
        menu = new JMenu("File");
        menu.getAccessibleContext().setAccessibleDescription("Menu File");
        menuBar.add(menu);

        // Add the file menu items
        menuItem = new JMenuItem(backupAction);
        menuItem.getAccessibleContext().setAccessibleDescription("Backup Wallet");
        menu.add(menuItem);

        menuItem = new JMenuItem(exitAction);
        menuItem.getAccessibleContext().setAccessibleDescription("Exit");
        menu.add(menuItem);

        // Build the second menu
        menu = new JMenu("Help");
        menu.getAccessibleContext().setAccessibleDescription("Help");
        menuBar.add(menu);

        menuItem = new JMenuItem(aboutAction);
        menuItem.getAccessibleContext().setAccessibleDescription("About");
        menu.add(menuItem);

        return menuBar;
    }

    public class BackupAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public BackupAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            // TODO hook up wallet backup procedure
        }
    }

    public class ExitAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ExitAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            // TODO shutdown kernel
            System.exit(0);
        }
    }

    public class AboutAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public AboutAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            ImageIcon icon = ResourceLoader.createIcon("semux", 64, 64);
            JOptionPane.showMessageDialog(frame, Config.CLIENT_FULL_NAME, "About Semux",
                    JOptionPane.INFORMATION_MESSAGE, icon);
        }
    }
}
