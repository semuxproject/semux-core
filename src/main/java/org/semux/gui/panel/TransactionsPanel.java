package org.semux.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.semux.core.Transaction;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;
import org.semux.utils.ByteArray;
import org.semux.utils.StringUtil;

public class TransactionsPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "Hash", "Type", "From", "To", "Value", "Time" };

    private Model model;

    private JTable table;
    private TransactionsTableModel tableModel;

    public TransactionsPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBackground(Color.WHITE);
        add(scrollPane);

        tableModel = new TransactionsTableModel();
        table = new JTable(tableModel);
        SwingUtil.setColumnWidths(table, 600, 0.15, 0.08, 0.25, 0.25, 0.12, 0.15);
        SwingUtil.setColumnAlignments(table, false, false, false, false, true, true);
        scrollPane.setViewportView(table);

        refresh();
    }

    class TransactionsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

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
                return "0x" + Hex.encode(tx.getHash());
            case 1:
                return StringUtil.toLowercaseExceptFirst(tx.getType().name());
            case 2:
                return "0x" + Hex.encode(tx.getFrom());
            case 3:
                return "0x" + Hex.encode(tx.getTo());
            case 4:
                return String.format("%.3f SEM", tx.getValue() / (double) Unit.SEM);
            case 5:
                SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm:ss");
                return df.format(new Date(tx.getTimestamp()));
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
        List<Transaction> transactions = new ArrayList<>();
        Set<ByteArray> hashes = new HashSet<>();
        for (Account acc : model.getAccounts()) {
            for (Transaction tx : acc.getTransactions()) {
                ByteArray key = ByteArray.of(tx.getHash());
                if (!hashes.contains(key)) {
                    transactions.add(tx);
                    hashes.add(key);
                }
            }
        }
        transactions.sort((tx1, tx2) -> {
            return Long.compare(tx2.getTimestamp(), tx1.getTimestamp());
        });
        tableModel.setData(transactions);
    }
}
