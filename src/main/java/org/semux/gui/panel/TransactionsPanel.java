package org.semux.gui.panel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.bouncycastle.util.encoders.Hex;
import org.semux.core.Transaction;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;

public class TransactionsPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Model model;
    private JTable table;
    private TransactionsTableModel tableModel;

    public TransactionsPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane);

        tableModel = new TransactionsTableModel();
        table = new JTable(tableModel);
        scrollPane.setViewportView(table);

        refresh();
    }

    class TransactionsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private String[] columnNames = { "Hash", "Type", "From", "To", "Value", "Time" };
        private List<Transaction> data;

        public TransactionsTableModel() {
            this.data = Collections.emptyList();
        }

        public void setData(List<Transaction> data) {
            this.data = data;
            this.fireTableDataChanged();
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            Transaction tx = data.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return Hex.encode(tx.getHash());
            case 1:
                return tx.getType().name();
            case 2:
                return Hex.encode(tx.getFrom());
            case 3:
                return Hex.encode(tx.getTo());
            case 4:
                return tx.getValue();
            case 5:
                return new Date(tx.getTimestamp());
            default:
                return null;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        default:
            break;
        }
    }

    private void refresh() {
        List<Transaction> list = new ArrayList<>();

        for (Account acc : model.getAccounts()) {
            list.addAll(acc.getIncomingTransactions());
            list.addAll(acc.getOutgoingTransactions());
        }

        list.sort((tx1, tx2) -> {
            return Long.compare(tx1.getTimestamp(), tx2.getTimestamp());
        });

        tableModel.setData(list);
    }
}
