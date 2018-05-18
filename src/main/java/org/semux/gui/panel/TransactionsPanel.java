/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.TransactionDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.util.ByteArray;
import org.semux.util.exception.UnreachableException;

/**
 * Transactions panel displays all transaction from/to accounts of the wallet.
 */
public class TransactionsPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String[] columnNames = { GuiMessages.get("Type"), GuiMessages.get("FromTo"),
            GuiMessages.get("Value"), GuiMessages.get("Time"), GuiMessages.get("Status") };

    private transient final SemuxGui gui;
    private transient final WalletModel model;
    private transient final List<StatusTransaction> transactions = new ArrayList<>();

    private final JTable table;
    private final TransactionsTableModel tableModel;

    private final JComboBox<ComboBoxItem<TransactionType>> selectType;
    private final JComboBox<ComboBoxItem<ByteArray>> selectFrom;
    private final JComboBox<ComboBoxItem<ByteArray>> selectTo;

    private final TransactionsComboBoxModel<ByteArray> fromModel;
    private final TransactionsComboBoxModel<ByteArray> toModel;
    private final TransactionsComboBoxModel<TransactionType> typeModel;

    public TransactionsPanel(SemuxGui gui, JFrame frame) {
        this.gui = gui;
        this.model = gui.getModel();
        this.model.addListener(this);

        setLayout(new BorderLayout(0, 0));

        tableModel = new TransactionsTableModel();
        table = new JTable(tableModel);
        table.setName("transactionsTable");
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 800, 0.1, 0.4, 0.15, 0.2, 0.15);
        SwingUtil.setColumnAlignments(table, false, false, true, true, true);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JTable sourceTable = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = sourceTable.rowAtPoint(p);
                if (me.getClickCount() == 2 && row != -1) {
                    Transaction tx = tableModel.getRow(sourceTable.convertRowIndexToModel(row));
                    if (tx != null) {
                        TransactionDialog dialog = new TransactionDialog(frame, tx);
                        dialog.setVisible(true);
                    }
                }
            }
        });

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(2, SwingUtil.VALUE_COMPARATOR);
        sorter.setComparator(3, SwingUtil.TIMESTAMP_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // add filters
        toModel = new TransactionsComboBoxModel<>();
        fromModel = new TransactionsComboBoxModel<>();
        typeModel = new TransactionsComboBoxModel<>();
        typeModel.setData(Arrays.stream(TransactionType.values()).map(it -> new ComboBoxItem<>(it.toString(), it))
                .collect(Collectors.toSet()));

        selectType = new JComboBox<>(typeModel);
        selectFrom = new JComboBox<>(fromModel);
        selectTo = new JComboBox<>(toModel);

        JLabel to = new JLabel(GuiMessages.get("To"));
        JLabel from = new JLabel(GuiMessages.get("From"));
        JLabel type = new JLabel(GuiMessages.get("Type"));

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING).addGroup(
                        groupLayout.createSequentialGroup()
                                .addComponent(type)
                                .addComponent(selectType)
                                .addComponent(from)
                                .addComponent(selectFrom)
                                .addComponent(to)
                                .addComponent(selectTo))
                        .addComponent(scrollPane));
        groupLayout.setVerticalGroup(
                groupLayout.createSequentialGroup()
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(type)
                                        .addComponent(selectType)
                                        .addComponent(from)
                                        .addComponent(selectFrom)
                                        .addComponent(to)
                                        .addComponent(selectTo)))
                        .addComponent(scrollPane));
        setLayout(groupLayout);

        refresh();
    }

    class TransactionsComboBoxModel<T> extends DefaultComboBoxModel<ComboBoxItem<T>> {

        private final ComboBoxItem<T> defaultItem;
        private final Set<ComboBoxItem<T>> currentValues = new TreeSet<>();

        public TransactionsComboBoxModel() {
            this.defaultItem = new ComboBoxItem<>("", null);
        }

        public void setData(Set<ComboBoxItem<T>> values) {

            // don't re-render the list if there are no changes
            // avoids undesirable refreshing of a list user might be interacting with
            if (currentValues.containsAll(values) && values.containsAll(currentValues)) {
                return;
            }

            currentValues.clear();
            currentValues.addAll(values);

            Object selected = getSelectedItem();
            removeAllElements();
            addElement(defaultItem);
            for (ComboBoxItem<T> value : currentValues) {
                addElement(value);
            }
            setSelectedItem(selected);
        }

        public void setSelectedItem(Object anObject) {
            super.setSelectedItem(anObject);
            List<StatusTransaction> filteredTransactions = filterTransactions(transactions);
            tableModel.setData(filteredTransactions);
        }

        T getSelectedValue() {
            ComboBoxItem<T> selected = (ComboBoxItem<T>) getSelectedItem();
            return selected != null ? selected.getValue() : null;
        }
    }

    class TransactionsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private transient List<StatusTransaction> transactions;

        TransactionsTableModel() {
            this.transactions = Collections.emptyList();
        }

        public void setData(List<StatusTransaction> transactions) {
            this.transactions = transactions;
            this.fireTableDataChanged();
        }

        Transaction getRow(int row) {
            if (row >= 0 && row < transactions.size()) {
                return transactions.get(row).getTransaction();
            }

            return null;
        }

        @Override
        public int getRowCount() {
            return transactions.size();
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
            StatusTransaction tx = transactions.get(row);

            switch (column) {
            case 0:
                return tx.getTransaction().getType().name();
            case 1:
                return SwingUtil.getTransactionDescription(gui, tx.getTransaction());
            case 2:
                return SwingUtil.formatAmount(tx.getTransaction().getValue());
            case 3:
                return SwingUtil.formatTimestamp(tx.getTransaction().getTimestamp());
            case 4:
                return tx.getStatus();
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
        default:
            throw new UnreachableException();
        }
    }

    /**
     * Refreshes this panel.
     */
    protected void refresh() {
        transactions.clear();
        Set<ByteArray> to = new HashSet<>();
        Set<ByteArray> from = new HashSet<>();

        // add pending transactions
        transactions.addAll(gui.getKernel().getPendingManager().getPendingTransactions()
                .parallelStream()
                .filter(pendingTx -> {
                    for (WalletAccount acc : model.getAccounts()) {
                        if (Arrays.equals(acc.getAddress(), pendingTx.transaction.getFrom()) ||
                                Arrays.equals(acc.getAddress(), pendingTx.transaction.getTo())) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(pendingTx -> new StatusTransaction(pendingTx.transaction, GuiMessages.get("Pending")))
                .collect(Collectors.toList()));

        // add completed transactions
        Set<ByteArray> hashes = new HashSet<>();
        for (WalletAccount acc : model.getAccounts()) {
            for (Transaction tx : acc.getTransactions()) {
                ByteArray key = ByteArray.of(tx.getHash());
                if (!hashes.contains(key)) {
                    transactions.add(new StatusTransaction(tx, GuiMessages.get("Completed")));
                    hashes.add(key);
                }
            }
        }
        transactions.sort(
                (tx1, tx2) -> Long.compare(tx2.getTransaction().getTimestamp(), tx1.getTransaction().getTimestamp()));

        /*
         * update model
         */
        Transaction tx = getSelectedTransaction();

        // track all used addresses
        for (StatusTransaction statusTransaction : transactions) {
            Transaction transaction = statusTransaction.getTransaction();
            to.add(new ByteArray(transaction.getFrom()));
            from.add(new ByteArray(transaction.getFrom()));
        }

        // filter transactions
        List<StatusTransaction> filteredTransactions = filterTransactions(transactions);

        tableModel.setData(filteredTransactions);

        fromModel.setData(from.stream()
                .map(it -> new ComboBoxItem<>(SwingUtil.describeAddress(gui, it.getData()), it))
                .collect(Collectors.toCollection(TreeSet::new)));

        toModel.setData(to.stream()
                .map(it -> new ComboBoxItem<>(SwingUtil.describeAddress(gui, it.getData()), it))
                .collect(Collectors.toCollection(TreeSet::new)));

        if (tx != null) {
            for (int i = 0; i < transactions.size(); i++) {
                if (Arrays.equals(tx.getHash(), transactions.get(i).getTransaction().getHash())) {
                    table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
                    break;
                }
            }
        }
    }

    /**
     * Filter transactions if filters are selected
     *
     * @param transactions
     *            transactions
     * @return filtered transactions
     */
    private List<StatusTransaction> filterTransactions(List<StatusTransaction> transactions) {
        List<StatusTransaction> filtered = new ArrayList<>();
        TransactionType type = typeModel.getSelectedValue();
        ByteArray to = toModel.getSelectedValue();
        ByteArray from = fromModel.getSelectedValue();

        // add if not filtered out
        for (StatusTransaction transaction : transactions) {
            if (type != null && !transaction.getTransaction().getType().equals(type)) {
                continue;
            }

            if (to != null && !Arrays.equals(to.getData(), transaction.getTransaction().getTo())) {
                continue;
            }

            if (from != null && !Arrays.equals(from.getData(), transaction.getTransaction().getFrom())) {
                continue;
            }

            filtered.add(transaction);
        }
        return filtered;
    }

    /**
     * Returns the selected transaction.
     *
     * @return
     */
    protected Transaction getSelectedTransaction() {
        int row = table.getSelectedRow();
        return (row != -1) ? tableModel.getRow(table.convertRowIndexToModel(row)) : null;
    }

    private static class StatusTransaction {
        private final String status;
        private final Transaction transaction;

        public StatusTransaction(Transaction transaction, String status) {

            this.transaction = transaction;
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }

    private static class ComboBoxItem<T> implements Comparable<ComboBoxItem<T>> {
        private final String displayName;
        private final T value;

        public ComboBoxItem(String displayName, T value) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ComboBoxItem<?> that = (ComboBoxItem<?>) o;

            return displayName != null ? displayName.equals(that.displayName) : that.displayName == null;
        }

        @Override
        public int hashCode() {
            return displayName != null ? displayName.hashCode() : 0;
        }

        @Override
        public int compareTo(ComboBoxItem<T> o) {
            return displayName.compareTo(o.displayName);
        }
    }
}
