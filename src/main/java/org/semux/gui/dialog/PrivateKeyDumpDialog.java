/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.util.UnreachableException;

public class PrivateKeyDumpDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { MessagesUtil.get("Account"), MessagesUtil.get("PublicKey"),
            MessagesUtil.get("PrivateKey") };

    private JTable table;
    private PrivateKeysTableModel tableModel;

    public PrivateKeyDumpDialog(JFrame owner, List<EdDSA> list) {
        tableModel = new PrivateKeysTableModel(list);

        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 600, 0.2, 0.4, 0.4);
        SwingUtil.setColumnAlignments(table, false, false, false);

        JButton dumpButton = SwingUtil.createDefaultButton(MessagesUtil.get("Export"), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Action action = Action.valueOf(e.getActionCommand());

                switch (action) {
                case DUMP_PRIVATE_KEY: {
                    try {

                        StringBuffer sb = new StringBuffer();
                        sb.append(MessagesUtil.get("Account") + ";" + MessagesUtil.get("PublicKey") + ";"
                                + MessagesUtil.get("PrivateKey") + "\n");
                        for (EdDSA acc : list) {
                            sb.append("0x" + Hex.encode(acc.toAddress()) + ";" + Hex.encode(acc.getPublicKey()) + ";"
                                    + Hex.encode(acc.getPrivateKey()) + "\n");
                        }
                        JFileChooser choser = new JFileChooser();
                        choser.setSelectedFile(new File("semuxprivatekeys.txt"));
                        choser.showSaveDialog(owner);
                        Files.write(Paths.get(choser.getSelectedFile().toURI()), sb.toString().getBytes());
                        PrivateKeyDumpDialog.this.dispose();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
                default:
                    throw new UnreachableException();
                }

            }
        }, Action.DUMP_PRIVATE_KEY);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
        add(scrollPane);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup().addGap(20)
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addComponent(scrollPane,
                                GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE))
                        .addGap(18))
                .addGroup(groupLayout.createSequentialGroup().addGap(20)
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addComponent(dumpButton))
                        .addGap(18)));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup().addGap(32)
                        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(scrollPane,
                                GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))
                        .addGap(18)
                        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(dumpButton))
                        .addGap(18)));
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(600, 240));
        this.setLocationRelativeTo(owner);
    }

    class PrivateKeysTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<EdDSA> accounts;

        public PrivateKeysTableModel(List<EdDSA> list) {
            this.accounts = list;
        }

        public EdDSA getRow(int row) {
            if (row >= 0 && row < accounts.size()) {
                return accounts.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return accounts.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            EdDSA acc = accounts.get(row);

            switch (column) {
            case 0:
                return "0x" + Hex.encode(acc.toAddress());
            case 1:
                return Hex.encode(acc.getPublicKey());
            case 2:
                return Hex.encode(acc.getPrivateKey());
            default:
                return null;
            }
        }
    }
}
