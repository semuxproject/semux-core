package org.semux.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.TransactionDialog;
import org.semux.utils.ByteArray;
import org.semux.utils.StringUtil;
import org.semux.utils.UnreachableException;

public class TransactionsPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "Type", "From/To", "Value", "Time" };

    private Model model;

    private JTable table;
    private TransactionsTableModel tableModel;

    public TransactionsPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        setLayout(new BorderLayout(0, 0));

        tableModel = new TransactionsTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 800, 0.1, 0.55, 0.15, 0.2);
        SwingUtil.setColumnAlignments(table, false, false, true, true);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    Transaction tx = tableModel.getRow(table.convertRowIndexToModel(row));
                    if (tx != null) {
                        TransactionDialog dialog = new TransactionDialog(TransactionsPanel.this, tx);
                        dialog.setVisible(true);
                    }
                }
            }
        });

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        sorter.setComparator(2, SwingUtil.BALANCE_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        add(scrollPane);

        refresh();
    }

    class TransactionsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<Transaction> transactions;
        private Map<String, Integer> accounts;

        public TransactionsTableModel() {
            this.transactions = Collections.emptyList();
            this.accounts = Collections.emptyMap();
        }

        public void setData(List<Transaction> transactions, Map<String, Integer> accounts) {
            this.transactions = transactions;
            this.accounts = accounts;
            this.fireTableDataChanged();
        }

        public Transaction getRow(int row) {
            if (row >= 0 && row < transactions.size()) {
                return transactions.get(row);
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
            Transaction tx = transactions.get(row);

            switch (column) {
            case 0:
                return StringUtil.toLowercaseExceptFirst(tx.getType().name());
            case 1:
                String from = "Block reward";
                if (tx.getType() != TransactionType.COINBASE) {
                    from = Hex.encode(tx.getFrom());
                    from = accounts.containsKey(from) ? "Account #" + accounts.get(from) : "0x" + from;
                }
                String to = Hex.encode(tx.getTo());
                to = accounts.containsKey(to) ? "Account #" + accounts.get(to) : "0x" + to;
                return from + " => " + to;
            case 2:
                return String.format("%.3f SEM", tx.getValue() / (double) Unit.SEM);
            case 3:
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return df.format(new Date(tx.getTimestamp()));
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

    private Transaction getSelectedTransaction() {
        int row = table.getSelectedRow();
        return (row != -1) ? tableModel.getRow(table.convertRowIndexToModel(row)) : null;
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

        Map<String, Integer> accounts = new HashMap<>();
        int n = 0;
        for (Account a : model.getAccounts()) {
            accounts.put(Hex.encode(a.getKey().toAddress()), n++);
        }

        /*
         * update table model
         */
        Transaction tx = getSelectedTransaction();
        tableModel.setData(transactions, accounts);

        if (tx != null) {
            for (int i = 0; i < transactions.size(); i++) {
                if (Arrays.equals(tx.getHash(), transactions.get(i).getHash())) {
                    table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
                    break;
                }
            }
        }
    }
}
