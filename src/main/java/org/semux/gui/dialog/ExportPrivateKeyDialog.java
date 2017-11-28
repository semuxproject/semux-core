/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.message.GUIMessages;
import org.semux.gui.SwingUtil;
import org.semux.util.UnreachableException;

public class ExportPrivateKeyDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "#", GUIMessages.get("Address"), GUIMessages.get("PrivateKey") };
    private JTable table;

    public ExportPrivateKeyDialog(JFrame parent) {
        Wallet wallet = Kernel.getInstance().getWallet();
        wallet.size();

        Object[][] data = new Object[wallet.size()][];
        for (int i = 0; i < wallet.size(); i++) {
            data[i] = new Object[3];
            data[i][0] = i;
            data[i][1] = Hex.encodeWithPrefix(wallet.getAccount(i).toAddress());
            data[i][2] = Hex.encodeWithPrefix(wallet.getAccount(i).getPrivateKey());
        }

        table = new JTable(data, columnNames);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 600, 0.1, 0.3, 0.6);
        SwingUtil.setColumnAlignments(table, false, false, false);

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.SOUTH);
        JButton btnCopy = SwingUtil.createDefaultButton(GUIMessages.get("CopyPrivateKey"), this,
                Action.COPY_PRIVATE_KEY);
        panel.add(btnCopy);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(600, 300));
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(table);

        this.setTitle(GUIMessages.get("ExportPrivateKey"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
        this.setModal(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case COPY_PRIVATE_KEY:
            int row = table.getSelectedRow();
            if (row != -1) {
                String privateKey = table.getModel().getValueAt(row, 2).toString();
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                cb.setContents(new StringSelection(privateKey), null);

                JOptionPane.showMessageDialog(this, GUIMessages.get("PrivateKeyCopied", privateKey));
            } else {
                JOptionPane.showMessageDialog(this, GUIMessages.get("SelectAccount"));
            }
            break;
        default:
            throw new UnreachableException();
        }
    }
}
