/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.semux.Kernel;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.SemuxGUI;
import org.semux.gui.SwingUtil;
import org.semux.gui.exception.QRCodeException;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceivePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ReceivePanel.class);

    private static String[] columnNames = { GUIMessages.get("Num"), GUIMessages.get("Address"),
            GUIMessages.get("Available"), GUIMessages.get("Locked") };

    private transient WalletModel model;

    private transient Kernel kernel;

    private JTable table;
    private ReceiveTableModel tableModel;
    private JLabel qr;

    public ReceivePanel(SemuxGUI gui) {
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();

        tableModel = new ReceiveTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 600, 0.05, 0.55, 0.2, 0.2);
        SwingUtil.setColumnAlignments(table, false, false, true, true);

        table.getSelectionModel().addListSelectionListener(
                ev -> actionPerformed(new ActionEvent(ReceivePanel.this, 0, Action.SELECT_ACCOUNT.name())));

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(0, SwingUtil.NUMBER_COMPARATOR);
        sorter.setComparator(2, SwingUtil.VALUE_COMPARATOR);
        sorter.setComparator(3, SwingUtil.VALUE_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        qr = new JLabel("");
        qr.setIcon(SwingUtil.emptyImage(200, 200));
        qr.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JButton btnCopyAddress = SwingUtil.createDefaultButton(GUIMessages.get("CopyAddress"), this,
                Action.COPY_ADDRESS);

        JButton buttonNewAccount = SwingUtil.createDefaultButton(GUIMessages.get("NewAccount"), this,
                Action.NEW_ACCOUNT);

        JButton btnDeleteAddress = SwingUtil.createDefaultButton(GUIMessages.get("DeleteAccount"), this,
                Action.DELETE_ACCOUNT);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(buttonNewAccount, GroupLayout.PREFERRED_SIZE, 121, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnCopyAddress)
                        .addComponent(qr)
                        .addComponent(btnDeleteAddress)
                        ))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(qr)
                    .addGap(18)
                    .addComponent(btnCopyAddress)
                    .addGap(18)
                    .addComponent(buttonNewAccount)
                    .addGap(18)
                    .addComponent(btnDeleteAddress)
                    .addContainerGap(249, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
        );
        groupLayout.linkSize(SwingConstants.HORIZONTAL, btnCopyAddress, buttonNewAccount, btnDeleteAddress);
        setLayout(groupLayout);
        // @formatter:on

        refresh();
    }

    class ReceiveTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private transient List<WalletAccount> data;

        public ReceiveTableModel() {
            this.data = Collections.emptyList();
        }

        public void setData(List<WalletAccount> data) {
            this.data = data;
            this.fireTableDataChanged();
        }

        public WalletAccount getRow(int row) {
            if (row >= 0 && row < data.size()) {
                return data.get(row);
            }

            return null;
        }

        @Override
        public int getRowCount() {
            return data.size();
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
            WalletAccount acc = data.get(row);

            switch (column) {
            case 0:
                return SwingUtil.formatNumber(row);
            case 1:
                return Hex.PREF + acc.getKey().toAddressString();
            case 2:
                return SwingUtil.formatValue(acc.getAvailable());
            case 3:
                return SwingUtil.formatValue(acc.getLocked());
            default:
                return null;
            }
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH: {
            refresh();
            break;
        }
        case SELECT_ACCOUNT: {
            try {
                WalletAccount acc = getSelectedAccount();
                if (acc != null) {
                    BufferedImage bi = SwingUtil.generateQR("semux://" + acc.getKey().toAddressString(), 200);
                    qr.setIcon(new ImageIcon(bi));
                }
            } catch (QRCodeException exception) {
                logger.error("Unable to generate QR code", exception);
            }
            break;
        }
        case COPY_ADDRESS: {
            WalletAccount acc = getSelectedAccount();
            if (acc == null) {
                JOptionPane.showMessageDialog(this, GUIMessages.get("SelectAccount"));
            } else {
                String address = Hex.PREF + acc.getKey().toAddressString();
                StringSelection stringSelection = new StringSelection(address);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                JOptionPane.showMessageDialog(this, GUIMessages.get("AddressCopied", address));
            }
            break;
        }
        case NEW_ACCOUNT: {
            EdDSA key = new EdDSA();

            Wallet wallet = kernel.getWallet();
            wallet.addAccount(key);
            wallet.flush();

            // fire update event
            model.fireUpdateEvent();

            JOptionPane.showMessageDialog(this, GUIMessages.get("NewAccountCreated"));
            break;
        }
        case DELETE_ACCOUNT: {
            WalletAccount acc = getSelectedAccount();
            if (acc == null) {
                JOptionPane.showMessageDialog(this, GUIMessages.get("SelectAccount"));
            } else {
                int ret = JOptionPane.showConfirmDialog(this, GUIMessages.get("ConfirmDeleteAccount"),
                        GUIMessages.get("DeleteAccount"), JOptionPane.YES_NO_OPTION);

                if (ret == JOptionPane.OK_OPTION) {
                    Wallet wallet = kernel.getWallet();
                    wallet.deleteAccount(acc.getKey());
                    wallet.flush();

                    // fire update event
                    model.fireUpdateEvent();

                    JOptionPane.showMessageDialog(this, GUIMessages.get("AccountDeleted"));
                }
            }
            break;
        }
        default:
            throw new UnreachableException();
        }
    }

    private WalletAccount getSelectedAccount() {
        int row = table.getSelectedRow();
        return (row != -1) ? tableModel.getRow(table.convertRowIndexToModel(row)) : null;
    }

    private void refresh() {
        List<WalletAccount> accounts = model.getAccounts();

        /*
         * update table model
         */
        WalletAccount acc = getSelectedAccount();
        tableModel.setData(accounts);

        if (acc != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (Arrays.equals(accounts.get(i).getKey().toAddress(), acc.getKey().toAddress())) {
                    table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
                    break;
                }
            }
        } else if (!accounts.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
    }
}
