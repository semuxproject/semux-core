/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import org.semux.core.TransactionType;
import org.semux.gui.ComboBoxItem;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.util.ByteArray;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TransactionsPanelFilter {

    private transient final SemuxGui gui;

    private final JComboBox<ComboBoxItem<TransactionType>> selectType;
    private final JComboBox<ComboBoxItem<byte[]>> selectFrom;
    private final JComboBox<ComboBoxItem<byte[]>> selectTo;

    private final TransactionsComboBoxModel<byte[]> fromModel;
    private final TransactionsComboBoxModel<byte[]> toModel;
    private final TransactionsComboBoxModel<TransactionType> typeModel;

    private final TransactionsPanel.TransactionsTableModel tableModel;

    private transient List<TransactionsPanel.StatusTransaction> transactions = new ArrayList<>();

    public TransactionsPanelFilter(SemuxGui gui, TransactionsPanel.TransactionsTableModel tableModel) {
        this.gui = gui;
        this.tableModel = tableModel;

        toModel = new TransactionsComboBoxModel<>();
        fromModel = new TransactionsComboBoxModel<>();
        typeModel = new TransactionsComboBoxModel<>();
        typeModel.setData(Arrays.stream(TransactionType.values()).map(it -> new ComboBoxItem<>(it.toString(), it))
                .collect(Collectors.toSet()));

        selectType = new JComboBox<>(typeModel);
        selectFrom = new JComboBox<>(fromModel);
        selectTo = new JComboBox<>(toModel);
    }

    /**
     * Filter transactions if filters are selected
     *
     * @return filtered transactions
     */
    public List<TransactionsPanel.StatusTransaction> getFilteredTransactions() {
        List<TransactionsPanel.StatusTransaction> filtered = new ArrayList<>();
        TransactionType type = typeModel.getSelectedValue();
        byte[] to = toModel.getSelectedValue();
        byte[] from = fromModel.getSelectedValue();
        TransactionType transactionType = typeModel.getSelectedValue();

        Set<ByteArray> allTo = new HashSet<>();
        Set<ByteArray> allFrom = new HashSet<>();
        Set<TransactionType> allTransactionType = new HashSet<>();

        // add if not filtered out
        for (TransactionsPanel.StatusTransaction transaction : transactions) {
            if (type != null && !transaction.getTransaction().getType().equals(type)) {
                continue;
            }

            if (to != null && !Arrays.equals(to, transaction.getTransaction().getTo())) {
                continue;
            }

            if (from != null && !Arrays.equals(from, transaction.getTransaction().getFrom())) {
                continue;
            }

            filtered.add(transaction);
            allTo.add(new ByteArray(transaction.getTransaction().getTo()));
            allFrom.add(new ByteArray(transaction.getTransaction().getFrom()));
            allTransactionType.add(transaction.getTransaction().getType());
        }

        // update filters that are not set for reduced filter set if filter not already
        // set on them for further filtering
        if (to == null) {
            toModel.setData(allTo.stream()
                    .map(it -> new ComboBoxItem<>(SwingUtil.describeAddress(gui, it.getData()), it.getData()))
                    .collect(Collectors.toCollection(TreeSet::new)));
        }

        if (from == null) {
            fromModel.setData(allFrom.stream()
                    .map(it -> new ComboBoxItem<>(SwingUtil.describeAddress(gui, it.getData()), it.getData()))
                    .collect(Collectors.toCollection(TreeSet::new)));
        }

        if (transactionType == null) {
            typeModel.setData(allTransactionType.stream()
                    .map(it -> new ComboBoxItem<>(it.toString(), it))
                    .collect(Collectors.toCollection(TreeSet::new)));
        }

        return filtered;
    }

    public JComboBox<ComboBoxItem<TransactionType>> getSelectType() {
        return selectType;
    }

    public JComboBox<ComboBoxItem<byte[]>> getSelectFrom() {
        return selectFrom;
    }

    public JComboBox<ComboBoxItem<byte[]>> getSelectTo() {
        return selectTo;
    }

    public void setTransactions(List<TransactionsPanel.StatusTransaction> transactions) {
        this.transactions = transactions;
    }

    class TransactionsComboBoxModel<T> extends DefaultComboBoxModel<ComboBoxItem<T>> {

        private final ComboBoxItem<T> defaultItem;
        private final Set<ComboBoxItem<T>> currentValues = new TreeSet<>();

        public TransactionsComboBoxModel() {
            this.defaultItem = new ComboBoxItem<>("", null);
        }

        public synchronized void setData(Set<ComboBoxItem<T>> values) {

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

        @Override
        public void setSelectedItem(Object anObject) {
            // only refresh if new object selected
            T existing = getSelectedValue();
            T newValue = (T) (anObject instanceof ComboBoxItem ? ((ComboBoxItem) anObject).getValue() : anObject);
            if (existing != newValue || existing == null || !existing.equals(newValue)) {
                super.setSelectedItem(anObject);
                List<TransactionsPanel.StatusTransaction> filteredTransactions = getFilteredTransactions();
                tableModel.setData(filteredTransactions);
            }

        }

        T getSelectedValue() {
            ComboBoxItem<T> selected = (ComboBoxItem<T>) getSelectedItem();
            return selected != null ? selected.getValue() : null;
        }
    }

}
