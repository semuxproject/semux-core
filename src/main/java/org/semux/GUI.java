/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Wallet;
import org.semux.gui.MainFrame;
import org.semux.gui.WelcomeFrame;

/**
 * Graphic user interface.
 */
public class GUI {

    public static void setupLookAndFeel() {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // do nothing
        }
    }

    public static void main(String[] args) {
        setupLookAndFeel();

        Wallet wallet = Wallet.getInstance();

        if (!wallet.exists()) {
            // start welcome frame
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    WelcomeFrame frame = new WelcomeFrame();
                    frame.setVisible(true);
                }
            });
        } else {
            for (int i = 0;; i++) {
                String msg = (i == 0) ? "Please enter your password" : "Wrong password, please try again";
                String pwd = JOptionPane.showInputDialog(msg);

                if (wallet.unlock(pwd)) {
                    break;
                }
            }

            // start main frame
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                }
            });

            // start kernel
            CLI.main(args);
        }
    }
}