/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.WriterException;

public class ReceivePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ReceivePanel.class);

    private static String[] columnNames = { GuiMessages.get("Num"), GuiMessages.get("Name"), GuiMessages.get("Address"),
            GuiMessages.get("Available"), GuiMessages.get("Locked") };

    private static final int QR_SIZE = 200;

    private transient WalletModel model;

    private transient Kernel kernel;

    private JTable table;
    private ReceiveTableModel tableModel;
    private JLabel qr;

    public ReceivePanel(SemuxGui gui) {
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();

        tableModel = new ReceiveTableModel();
        table = new JTable(tableModel);
        table.setName("accountsTable");
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 600, 0.05, 0.1, 0.55, 0.15, 0.15);
        SwingUtil.setColumnAlignments(table, false, false, false, true, true);

        table.getSelectionModel().addListSelectionListener(
                ev -> actionPerformed(new ActionEvent(ReceivePanel.this, 0, Action.SELECT_ACCOUNT.name())));

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(0, SwingUtil.NUMBER_COMPARATOR);
        sorter.setComparator(3, SwingUtil.VALUE_COMPARATOR);
        sorter.setComparator(4, SwingUtil.VALUE_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        qr = new JLabel("");
        qr.setIcon(SwingUtil.emptyImage(QR_SIZE, QR_SIZE));
        qr.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JButton btnCopyAddress = SwingUtil
                .createDefaultButton(GuiMessages.get("CopyAddress"), this, Action.COPY_ADDRESS);
        btnCopyAddress.setName("btnCopyAddress");

        JButton buttonNewAccount = SwingUtil
                .createDefaultButton(GuiMessages.get("NewAccount"), this, Action.NEW_ACCOUNT);
        buttonNewAccount.setName("buttonNewAccount");

        JButton btnRenameAddress = SwingUtil
                .createDefaultButton(GuiMessages.get("RenameAccount"), this, Action.RENAME_ACCOUNT);
        btnRenameAddress.setName("btnRenameAddress");

        JButton btnDeleteAddress = SwingUtil
                .createDefaultButton(GuiMessages.get("DeleteAccount"), this, Action.DELETE_ACCOUNT);
        btnDeleteAddress.setName("btnDeleteAddress");

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
                        .addComponent(btnRenameAddress)
                        .addComponent(qr)
                        .addComponent(btnDeleteAddress)))
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(qr)
                                .addGap(18)
                                .addComponent(btnCopyAddress)
                                .addGap(18)
                                .addComponent(btnRenameAddress)
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
                return acc.getName().orElse("");
            case 2:
                return Hex.PREF + acc.getKey().toAddressString();
            case 3:
                return SwingUtil.formatValue(acc.getAvailable());
            case 4:
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
        case REFRESH:
            refresh();
            break;
        case SELECT_ACCOUNT:
            selectAccount();
            break;
        case COPY_ADDRESS:
            copyAddress();
            break;
        case NEW_ACCOUNT:
            newAccount();
            break;
        case DELETE_ACCOUNT:
            deleteAccount();
            break;
        case RENAME_ACCOUNT:
            renameAccount();
            break;
        default:
            throw new UnreachableException();
        }
    }

    /**
     * Processes the REFRESH event.
     */
    protected void refresh() {
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

        selectAccount();
    }

    /**
     * Processes the SELECT_ACCOUNT event.
     */
    protected void selectAccount() {
        try {
            WalletAccount acc = getSelectedAccount();

            if (acc != null) {
                BufferedImage bi = SwingUtil.createQrImage("semux://" + Hex.PREF + acc.getKey().toAddressString(),
                        QR_SIZE, QR_SIZE);
                qr.setIcon(new ImageIcon(bi));
            } else {
                qr.setIcon(SwingUtil.emptyImage(QR_SIZE, QR_SIZE));
            }
        } catch (WriterException exception) {
            logger.error("Unable to generate QR code", exception);
        }
    }

    /**
     * Processes the COPY_ADDRESS event
     */
    protected void copyAddress() {
        WalletAccount acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, GuiMessages.get("SelectAccount"));
        } else {
            String address = Hex.PREF + acc.getKey().toAddressString();
            StringSelection stringSelection = new StringSelection(address);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            JOptionPane.showMessageDialog(this, GuiMessages.get("AddressCopied", address));
        }
    }

    /**
     * Process the RENAME_ACCOUNT event
     */
    private void renameAccount() {
        WalletAccount acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, GuiMessages.get("SelectAccount"));
        } else {
            String name = JOptionPane.showInputDialog(this, GuiMessages.get("Name"), GuiMessages.get("RenameAccount"),
                    JOptionPane.QUESTION_MESSAGE);
            if (name != null) {
                Wallet wallet = kernel.getWallet();
                wallet.setAddressAlias(acc.getKey().toAddress(), name);
                wallet.flush();
                // fire update event
                model.fireUpdateEvent();
            }
        }
    }

    /**
     * Processes the NEW_ACCOUNT event.
     */
    protected void newAccount() {
        Key key = new Key();

        Wallet wallet = kernel.getWallet();
        wallet.addAccount(key);
        boolean added = wallet.flush();

        if (added) {
            // fire update event
            model.fireUpdateEvent();

            JOptionPane.showMessageDialog(this, GuiMessages.get("NewAccountCreated"));
        } else {
            wallet.removeAccount(key);
            JOptionPane.showMessageDialog(this, GuiMessages.get("WalletSaveFailed"));
        }
    }

    /**
     * Processes the DELETE_ACCOUNT event.
     */
    protected void deleteAccount() {
        WalletAccount acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, GuiMessages.get("SelectAccount"));
        } else {
            int ret = JOptionPane
                    .showConfirmDialog(this, GuiMessages.get("ConfirmDeleteAccount"), GuiMessages.get("DeleteAccount"),
                            JOptionPane.YES_NO_OPTION);

            if (ret == JOptionPane.OK_OPTION) {
                Wallet wallet = kernel.getWallet();
                wallet.removeAccount(acc.getKey());
                wallet.removeAddressAlias(acc.getAddress());
                wallet.flush();

                // fire update event
                model.fireUpdateEvent();

                JOptionPane.showMessageDialog(this, GuiMessages.get("AccountDeleted"));
            }
        }
    }

    /**
     * Returns the selected account.
     *
     * @return
     */
    protected WalletAccount getSelectedAccount() {
        int row = table.getSelectedRow();
        return (row != -1) ? tableModel.getRow(table.convertRowIndexToModel(row)) : null;
    }

}
