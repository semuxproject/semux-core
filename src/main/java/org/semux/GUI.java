/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.semux.core.Wallet;
import org.semux.gui.MainFrame;
import org.semux.gui.PasswordFrame;
import org.semux.gui.WelcomeFrame;

/**
 * Graphic user interface.
 */
public class GUI {

    private String[] args;

    public GUI(String[] args) {
        this.args = args;
    }

    public void setupLookAndFeel() {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Semux");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // do nothing
        }
    }

    public void showWelcome() {
        // start welcome frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                WelcomeFrame frame = new WelcomeFrame(GUI.this);
                frame.setVisible(true);
            }
        });
    }

    public void showMain() {
        // start kernel
        CLI.main(args);

        // start main frame
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        GUI gui = new GUI(args);
        gui.setupLookAndFeel();

        Wallet wallet = Wallet.getInstance();
        if (!wallet.exists()) {
            gui.showWelcome();
        } else {
            for (int i = 0;; i++) {
                PasswordFrame frame = new PasswordFrame(i == 0 ? null : "Wrong password, please try again");
                frame.setVisible(true);
                String pw = frame.getPassword();

                if (pw == null) {
                    System.exit(-1);
                } else if (wallet.unlock(pw)) {
                    break;
                }
            }
            gui.showMain();
        }
    }
}