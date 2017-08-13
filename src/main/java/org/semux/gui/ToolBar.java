/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class ToolBar {
    public JPanel createToolbar() {
        int fontSize = 16;
        JButton sendButton;
        JButton receiveButton;
        JButton delegatesButton;

        JToolBar toolBar = new JToolBar();

        toolBar.setFloatable(false);

        // the space between buttons
        Dimension dimension = new Dimension(10, 0);

        // add send button
        sendButton = new JButton("Send");
        sendButton.setVerticalTextPosition(AbstractButton.CENTER);
        sendButton.setActionCommand("send");
        sendButton.setIcon(ResourceLoader.createIcon("send", 48, 48));
        sendButton.setMargin(new Insets(0, 0, 0, 10));
        sendButton.setFont(new Font(null, 0, fontSize));
        toolBar.add(sendButton);
        toolBar.add(Box.createRigidArea(dimension));

        // add receive button
        receiveButton = new JButton("Receive");
        receiveButton.setVerticalTextPosition(AbstractButton.CENTER);
        receiveButton.setActionCommand("receive");
        receiveButton.setIcon(ResourceLoader.createIcon("receive", 48, 48));
        receiveButton.setMargin(new Insets(0, 0, 0, 10));
        receiveButton.setFont(new Font(null, 0, fontSize));
        toolBar.add(receiveButton);
        toolBar.add(Box.createRigidArea(dimension));

        // add delegates button
        delegatesButton = new JButton("Delegates");
        delegatesButton.setVerticalTextPosition(AbstractButton.CENTER);
        delegatesButton.setActionCommand("delegates");
        delegatesButton.setIcon(ResourceLoader.createIcon("delegates", 48, 48));
        delegatesButton.setMargin(new Insets(0, 0, 0, 10));
        delegatesButton.setFont(new Font(null, 0, fontSize));
        toolBar.add(delegatesButton);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(toolBar);
        return panel;
    }

}
