/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLoader {

    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     * 
     * @param imageName
     *            image name
     * @return an image icon if exists, otherwise null
     */
    public static ImageIcon createIcon(String imageName, int width, int height) {
        String imgLocation = imageName + ".png";
        URL imageURL = ResourceLoader.class.getResource(imgLocation);

        if (imageURL == null) {
            logger.warn("Resource not found: " + imgLocation);
            return null;
        } else {
            ImageIcon icon = new ImageIcon(imageURL);
            Image img = icon.getImage();
            Image newimg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            icon = new ImageIcon(newimg);
            return icon;
        }
    }
}
