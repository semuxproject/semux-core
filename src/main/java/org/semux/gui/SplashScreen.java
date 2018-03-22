/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This splash screen fills the empty between startup dialog and {@link MainFrame}.
 */
public class SplashScreen extends JFrame implements SemuxEventListener {

    private static final long serialVersionUID = 5729665012428583185L;

    private static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);

    private transient WalletModel walletModel;

    private JProgressBar progressBar;

    public SplashScreen(WalletModel walletModel) {
        this.walletModel = walletModel;
        walletModel.addSemuxEventListener(this);

        setUndecorated(true);
        setContentPane(new ContentPane());
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new ImagePane());

        progressBar = new JProgressBar();
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(true);
        progressBar.setMaximum(100);
        progressBar.setString(GuiMessages.get("SplashLoading"));
        getContentPane().add(progressBar, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public synchronized void onSemuxEvent(SemuxEvent event) {
        switch (event) {
        case WALLET_LOADING:
            progressBar.setValue(33);
            progressBar.setString(GuiMessages.get("SplashLoadingWallet"));
            break;

        case KERNEL_STARTING:
            progressBar.setValue(66);
            progressBar.setString(GuiMessages.get("SplashStartingKernel"));
            break;

        case GUI_MAINFRAME_STARTED:
            progressBar.setValue(100);
            walletModel.removeSemuxEventListener(this);
            setVisible(false);
            dispose();
            break;
        }
    }

    private class ContentPane extends JPanel {

        private static final long serialVersionUID = -5929022627261315407L;

        private ContentPane() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 0));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    private class ImagePane extends JPanel {

        private static final long serialVersionUID = -2425357267526029507L;

        private transient BufferedImage backgroundImage;

        private ImagePane() {
            setOpaque(false);
            try {
                backgroundImage = ImageIO.read(SplashScreen.class.getResource("splash.png"));
            } catch (IOException e) {
                logger.error("Unable to load splash.png", e);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int x = (getWidth() - backgroundImage.getWidth()) / 2;
                int y = (getHeight() - backgroundImage.getHeight()) / 2;
                g2d.drawImage(backgroundImage, x, y, this);
                g2d.dispose();
            }
        }
    }
}