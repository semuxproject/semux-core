package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Delegate;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;
import org.semux.utils.Bytes;

public class DelegatesPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "Status", "Name", "Address", "Votes", "Votes from Me" };

    private Model model;

    private JTable table;
    private DelegatesTableModel tableModel;

    JComboBox<String> from;

    private JTextField textVote;
    private JTextField textUnvote;
    private JTextField textName;

    public DelegatesPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        tableModel = new DelegatesTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(24);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 500, 0.08, 0.2, 0.42, 0.15, 0.15);
        SwingUtil.setColumnAlignments(table, false, false, false, true, true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JPanel panel2 = new JPanel();
        panel2.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel label = new JLabel(
                "<html>NOTE: A minimum transaction fee (5 mSEM) will apply when you vote/unvote/register a delegate.</p></html>");
        label.setForeground(Color.DARK_GRAY);

        from = new JComboBox<>();
        from.setActionCommand(Action.SELECT_ACCOUNT.name());
        from.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(from, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panel, 0, 200, Short.MAX_VALUE)
                        .addComponent(panel2, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addComponent(label, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(5)
                    .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel2, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(label)
                    .addContainerGap(88, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
        );
        setLayout(groupLayout); 
        // @formatter:on

        textVote = new JTextField();
        textVote.setToolTipText("# of votes");
        textVote.setColumns(10);
        textVote.setActionCommand(Action.VOTE.name());
        textVote.addActionListener(this);

        JButton btnVote = new JButton("Vote");
        btnVote.setActionCommand(Action.VOTE.name());
        btnVote.addActionListener(this);

        textUnvote = new JTextField();
        textUnvote.setToolTipText("# of votes");
        textUnvote.setColumns(10);
        textUnvote.setActionCommand(Action.UNVOTE.name());
        textUnvote.addActionListener(this);

        JButton btnUnvote = new JButton("Unvote");
        btnUnvote.setActionCommand(Action.UNVOTE.name());
        btnUnvote.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout2 = new GroupLayout(panel);
        groupLayout2.setHorizontalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout2.createParallelGroup(Alignment.LEADING)
                        .addComponent(textUnvote, 0, 0, Short.MAX_VALUE)
                        .addComponent(textVote, GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(btnVote, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnUnvote, GroupLayout.PREFERRED_SIZE, 75, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout2.setVerticalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addGap(16)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnVote)
                        .addComponent(textVote, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(textUnvote, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnUnvote)))
        );
        panel.setLayout(groupLayout2);
        // @formatter:on

        JButton btnDelegate = new JButton("Register as delegate");
        btnDelegate.addActionListener(this);
        btnDelegate.setActionCommand(Action.DELEGATE.name());

        textName = new JTextField();
        textName.setToolTipText("Name");
        textName.setColumns(10);
        textName.setActionCommand(Action.DELEGATE.name());
        textName.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout3 = new GroupLayout(panel2);
        groupLayout3.setHorizontalGroup(
            groupLayout3.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout3.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout3.createParallelGroup(Alignment.LEADING)
                        .addComponent(textName, GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                        .addComponent(btnDelegate, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout3.setVerticalGroup(
            groupLayout3.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout3.createSequentialGroup()
                    .addGap(10)
                    .addComponent(textName, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(btnDelegate)
                    .addContainerGap(15, Short.MAX_VALUE))
        );
        panel2.setLayout(groupLayout3);
        // @formatter:on

        refreshAccounts();
    }

    class DelegatesTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<Delegate> delegates;
        private List<Long> votesFromMe;

        public DelegatesTableModel() {
            this.delegates = Collections.emptyList();
            this.votesFromMe = Collections.emptyList();
        }

        public void setData(List<Delegate> delegates, List<Long> votesFromMe) {
            assert (delegates.size() == votesFromMe.size());

            this.delegates = delegates;
            this.votesFromMe = votesFromMe;
            this.fireTableDataChanged();
        }

        public Delegate getRow(int row) {
            if (row >= 0 && row < delegates.size()) {
                return delegates.get(row);
            }

            return null;
        }

        @Override
        public int getRowCount() {
            return delegates.size();
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
            Delegate d = delegates.get(row);

            switch (column) {
            case 0:
                return row >= Config.getNumberOfValidators(model.getLatestBlockNumber()) ? "S" : "V";
            case 1:
                return Bytes.toString(d.getName());
            case 2:
                return "0x" + Hex.encode(d.getAddress());
            case 3:
                return d.getVotes() / Unit.SEM;
            case 4:
                return votesFromMe.get(row) / Unit.SEM;
            default:
                return null;
            }
        }
    }

    public int getFrom() {
        return from.getSelectedIndex();
    }

    public void setFromItems(List<? extends Object> items) {
        int n = from.getSelectedIndex();

        from.removeAllItems();
        for (Object item : items) {
            from.addItem(item.toString());
        }

        if (!items.isEmpty()) {
            from.setSelectedIndex(n >= 0 && n < items.size() ? n : 0);
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH: {
            refreshAccounts();
            refreshDelegates();
            break;
        }
        case SELECT_ACCOUNT: {
            refreshDelegates();
            break;
        }
        case VOTE:
        case UNVOTE: {
            Account a = getSelectedAccount();
            Delegate d = getSelectedDelegate();
            String v = action.equals(Action.VOTE) ? textVote.getText() : textUnvote.getText();
            if (a == null) {
                JOptionPane.showMessageDialog(this, "Please select an account!");
            } else if (d == null) {
                JOptionPane.showMessageDialog(this, "Please select a delegate");
            } else if (!v.matches("[\\d]+")) {
                JOptionPane.showMessageDialog(this, "Please enter the number of votes");
            } else {
                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = action.equals(Action.VOTE) ? TransactionType.VOTE : TransactionType.UNVOTE;
                byte[] from = a.getKey().toAddress();
                byte[] to = d.getAddress();
                long value = Long.parseLong(v) * Unit.SEM;
                long fee = Config.MIN_TRANSACTION_FEE;
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = {};
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(a.getKey());

                pendingMgr.addTransaction(tx);
                JOptionPane.showMessageDialog(this, "Transaction sent. It takes at least 20s to get processed!");
                clear();
            }
            break;
        }
        case DELEGATE: {
            Account a = getSelectedAccount();
            String name = textName.getText();
            if (a == null) {
                JOptionPane.showMessageDialog(this, "Please select an account!");
            } else if (!name.matches("[_a-z0-9]{4,16}")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid delegate name!");
            } else if (a.getBalance() < Config.MIN_DELEGATE_FEE + Config.MIN_TRANSACTION_FEE) {
                JOptionPane.showMessageDialog(this, "Insufficient funds! Delegate registration fee = "
                        + Config.MIN_DELEGATE_FEE / Unit.SEM + " SEM");
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        "Delegate registration will cost you " + Config.MIN_DELEGATE_FEE / Unit.SEM + " SEM, continue?",
                        "Confirm delegate registration", JOptionPane.YES_NO_OPTION);
                if (ret != JOptionPane.YES_OPTION) {
                    break;
                }

                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = TransactionType.DELEGATE;
                byte[] from = a.getKey().toAddress();
                byte[] to = from;
                long value = Config.MIN_DELEGATE_FEE;
                long fee = Config.MIN_TRANSACTION_FEE;
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = Bytes.of(name);
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(a.getKey());

                pendingMgr.addTransaction(tx);
                JOptionPane.showMessageDialog(this, "Transaction sent. It takes at least 20s to get processed!");
                clear();
            }
            break;
        }
        default:
            break;
        }
    }

    private Account getSelectedAccount() {
        int idx = from.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    private Delegate getSelectedDelegate() {
        int idx = table.getSelectedRow();
        return (idx == -1) ? null : tableModel.getRow(idx);
    }

    private void refreshAccounts() {
        List<String> accounts = new ArrayList<>();
        List<Account> list = model.getAccounts();
        for (int i = 0; i < list.size(); i++) {
            accounts.add("Account #" + i);
        }
        setFromItems(accounts);
    }

    private void refreshDelegates() {
        List<Delegate> delegates = model.getDelegates();
        delegates.sort((d1, d2) -> {
            int c = Long.compare(d2.getVotes(), d1.getVotes());
            return c != 0 ? c : new String(d1.getName()).compareTo(new String(d2.getName()));
        });

        List<Long> votesFromMe = new ArrayList<>();
        Account acc = getSelectedAccount();
        if (acc == null) {
            for (int i = 0; i < delegates.size(); i++) {
                votesFromMe.add(0L);
            }
        } else {
            byte[] a = acc.getKey().toAddress();
            DelegateState ds = Kernel.getInstance().getBlockchain().getDeleteState();
            for (Delegate d : delegates) {
                long vote = ds.getVote(a, d.getAddress());
                votesFromMe.add(vote);
            }
        }

        tableModel.setData(delegates, votesFromMe);
    }

    private void clear() {
        textVote.setText("");
        textUnvote.setText("");
        textName.setText("");
    }
}
