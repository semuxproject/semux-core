/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwingUtil {

    private static final Logger logger = LoggerFactory.getLogger(SwingUtil.class);

    /**
     * Put a JFrame in the center of screen.
     * 
     * @param frame
     * @param width
     * @param height
     */
    public static void centerizeFrame(JFrame frame, int width, int height) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = ((int) d.getWidth() - width) / 2;
        int y = ((int) d.getHeight() - height) / 2;
        frame.setLocation((int) x, (int) y);
        frame.setBounds(x, y, width, height);
    }

    /**
     * Load an ImageIcon from resource, and rescale it.
     * 
     * @param imageName
     *            image name
     * @return an image icon if exists, otherwise null
     */
    public static ImageIcon loadImage(String imageName, int width, int height) {
        String imgLocation = imageName + ".png";
        URL imageURL = SwingUtil.class.getResource(imgLocation);

        if (imageURL == null) {
            logger.warn("Resource not found: " + imgLocation);
            return null;
        } else {
            ImageIcon icon = new ImageIcon(imageURL);
            Image img = icon.getImage();
            Image img2 = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img2);
        }
    }

    /**
     * Set the preferred width of table columns.
     * 
     * @param table
     * @param total
     * @param widths
     */
    public static void setColumnWidths(JTable table, int total, double... widths) {
        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < widths.length; i++) {
            model.getColumn(i).setPreferredWidth((int) (total * widths[i]));
        }
    }

    /**
     * Set the alignments of table columns.
     * 
     * @param table
     * @param right
     */
    public static void setColumnAlignments(JTable table, boolean... right) {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < right.length; i++) {
            if (right[i]) {
                model.getColumn(i).setCellRenderer(rightRenderer);
            }
        }
    }
}
