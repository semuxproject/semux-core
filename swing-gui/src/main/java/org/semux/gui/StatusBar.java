/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.semux.core.SyncManager;
import org.semux.message.GuiMessages;

/**
 * StatusBar represents a UI component displaying current wallet status like
 * sync progress and number of peers.
 */
public class StatusBar extends JPanel {

    private static final long serialVersionUID = 2676757102891632156L;

    private final JLabel peers = new JLabel();

    private final JProgressBar syncProgressBar = SwingUtil.createMetalProgressBar();

    public StatusBar(Frame parent) {
        super();
        init(parent);
    }

    /**
     * Initialize UI components.
     *
     * @param parent
     *            parent frame.
     */
    private void init(Frame parent) {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(parent.getWidth(), getFontMetrics(getFont()).getHeight() + 10));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        addGap(10);

        // add number of peers
        JLabel peersLabel = new JLabel(GuiMessages.get("Peers") + ":");
        peersLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(peersLabel);
        addGap(5);

        peers.setName("peers");
        peers.setHorizontalAlignment(SwingConstants.LEFT);
        add(peers);

        addSeparator();

        // add progress bar
        JLabel syncProgressLabel = new JLabel(GuiMessages.get("SyncProgress") + ":");
        syncProgressLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(syncProgressLabel);
        addGap(5);

        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.X_AXIS));
        progressBarPanel.setMaximumSize(new Dimension(500, getFontMetrics(getFont()).getHeight()));
        syncProgressBar.setMaximum(10000);
        syncProgressBar.setAlignmentY(CENTER_ALIGNMENT);
        syncProgressBar.setStringPainted(true);
        progressBarPanel.add(syncProgressBar);
        add(progressBarPanel);

        addSeparator();
    }

    /**
     * Update progress bar and estimated time.
     *
     * @param progress
     *            current progress.
     */
    public void setProgress(SyncManager.Progress progress) {
        syncProgressBar.setValue(
                (int) Math.round((double) progress.getCurrentHeight() / (double) progress.getTargetHeight() * 10000d));

        Duration estimation = progress.getSyncEstimation();
        if (estimation != null && estimation.getSeconds() > 0L) {
            syncProgressBar.setString(String.format(
                    "%s (%s)",
                    SyncProgressFormatter.format(progress),
                    // TODO: localize estimation
                    estimation.toDays() >= 30 ? ">= 1 month"
                            : DurationFormatUtils.formatDurationWords(
                                    estimation.toMillis(), true, true)));
        } else {
            syncProgressBar.setString(SyncProgressFormatter.format(progress));
        }
    }

    /**
     * Update the number of peers.
     *
     * @param peersNumber
     *            current number of peers.
     */
    public void setPeersNumber(int peersNumber) {
        peers.setText(SwingUtil.formatNumber(peersNumber));
    }

    private void addGap(int width) {
        add(Box.createRigidArea(new Dimension(width, 0)));
    }

    private void addSeparator() {
        VerticalSeparator separator = new VerticalSeparator();
        separator.setMaximumSize(new Dimension(1, getFontMetrics(getFont()).getHeight()));
        addGap(20);
        add(separator);
        addGap(20);
    }

    /**
     * Syncing progress formatter.
     */
    protected static class SyncProgressFormatter {

        private SyncProgressFormatter() {
        }

        public static String format(SyncManager.Progress progress) {
            if (progress == null) {
                return GuiMessages.get("SyncStopped");
            } else if (progress.getCurrentHeight() > 0 && progress.getCurrentHeight() == progress.getTargetHeight()) {
                return GuiMessages.get("SyncFinished");
            } else if (progress.getTargetHeight() > 0) {
                return SwingUtil.formatPercentage(
                        (double) progress.getCurrentHeight() / (double) progress.getTargetHeight() * 100d, 2);
            } else {
                return GuiMessages.get("SyncStopped");
            }
        }
    }
}
