package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.Blockchain;
import org.semux.core.Delegate;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.MessagesUtil;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.DelegateDialog;
import org.semux.utils.Bytes;
import org.semux.utils.UnreachableException;

public class DelegatesPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { MessagesUtil.get("Num"), MessagesUtil.get("Name"),
            MessagesUtil.get("Address"), MessagesUtil.get("Votes"), MessagesUtil.get("VotesFromMe"),
            MessagesUtil.get("Status"), MessagesUtil.get("Rate") };

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
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 600, 0.05, 0.2, 0.25, 0.15, 0.15, 0.1, 0.1);
        SwingUtil.setColumnAlignments(table, false, false, false, true, true, true, true);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    Delegate d = tableModel.getRow(table.convertRowIndexToModel(row));
                    if (d != null) {
                        DelegateDialog dialog = new DelegateDialog(DelegatesPanel.this, d);
                        dialog.setVisible(true);
                    }
                }
            }
        });

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        sorter.setComparator(0, SwingUtil.NUMBER_COMPARATOR);
        sorter.setComparator(3, SwingUtil.NUMBER_COMPARATOR);
        sorter.setComparator(4, SwingUtil.NUMBER_COMPARATOR);
        sorter.setComparator(6, SwingUtil.PERCENTAGE_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JPanel panel2 = new JPanel();
        panel2.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel label = new JLabel(
                MessagesUtil.get("DelegateRegistrationNoteHtml", SwingUtil.formatValue(Config.DELEGATE_BURN_AMOUNT),
                        SwingUtil.formatValue(Config.MIN_TRANSACTION_FEE_SOFT)));
        label.setForeground(Color.DARK_GRAY);

        from = new JComboBox<>();
        from.setActionCommand(Action.SELECT_ACCOUNT.name());
        from.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                        .addComponent(from, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                        .addComponent(label, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                        .addComponent(panel2, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel2, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(label)
                    .addContainerGap(174, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
        );
        setLayout(groupLayout); 
        // @formatter:on

        textVote = SwingUtil.textFieldWithPopup();
        textVote.setToolTipText(MessagesUtil.get("NumVotes"));
        textVote.setColumns(10);
        textVote.setActionCommand(Action.VOTE.name());
        textVote.addActionListener(this);

        JButton btnVote = new JButton(MessagesUtil.get("Vote"));
        btnVote.setActionCommand(Action.VOTE.name());
        btnVote.addActionListener(this);

        textUnvote = SwingUtil.textFieldWithPopup();
        textUnvote.setToolTipText(MessagesUtil.get("NumVotes"));
        textUnvote.setColumns(10);
        textUnvote.setActionCommand(Action.UNVOTE.name());
        textUnvote.addActionListener(this);

        JButton btnUnvote = new JButton(MessagesUtil.get("Unvote"));
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
                        .addComponent(textVote, GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(btnVote, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnUnvote, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout2.setVerticalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addGap(16)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnVote)
                        .addComponent(textVote, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(16)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(textUnvote, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnUnvote)))
        );
        panel.setLayout(groupLayout2);
        // @formatter:on

        JButton btnDelegate = new JButton(MessagesUtil.get("RegisterAsDelegate"));
        btnDelegate.addActionListener(this);
        btnDelegate.setActionCommand(Action.DELEGATE.name());

        textName = SwingUtil.textFieldWithPopup();
        textName.setToolTipText(MessagesUtil.get("Name"));
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
                    .addGap(16)
                    .addComponent(textName, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addGap(16)
                    .addComponent(btnDelegate))
        );
        panel2.setLayout(groupLayout3);
        // @formatter:on

        refreshAccounts();
    }

    class DelegatesTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<Delegate> delegates;

        public DelegatesTableModel() {
            this.delegates = Collections.emptyList();
        }

        public void setData(List<Delegate> delegates) {
            this.delegates = delegates;
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
                return SwingUtil.formatNumber(row);
            case 1:
                return Bytes.toString(d.getName());
            case 2:
                return Hex.PREF + Hex.encode(d.getAddress());
            case 3:
                return SwingUtil.formatVote(d.getVotes());
            case 4:
                return SwingUtil.formatVote(d.getVotesFromMe());
            case 5:
                List<String> validators = Kernel.getInstance().getBlockchain().getValidators();
                return new HashSet<>(validators).contains(Hex.encode(d.getAddress())) ? "V" : "S";
            case 6:
                return SwingUtil.formatPercentage(d.getRate());
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
            long value = 0;
            try {
                value = SwingUtil.parseValue(v);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("EnterValidNumberOfvotes"));
                break;
            }

            if (a == null) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("SelectAccount"));
            } else if (d == null) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("SelectDelegate"));
            } else {
                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = action.equals(Action.VOTE) ? TransactionType.VOTE : TransactionType.UNVOTE;
                byte[] from = a.getKey().toAddress();
                byte[] to = d.getAddress();
                long fee = Config.MIN_TRANSACTION_FEE_SOFT;
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = {};
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(a.getKey());

                sendTransaction(pendingMgr, tx);
            }
            break;
        }
        case DELEGATE: {
            Account a = getSelectedAccount();
            String name = textName.getText();
            if (a == null) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("SelectAccount"));
            } else if (!name.matches("[_a-z0-9]{4,16}")) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("AccountNameError"));
            } else if (a.getBalance() < Config.DELEGATE_BURN_AMOUNT + Config.MIN_TRANSACTION_FEE_SOFT) {
                JOptionPane.showMessageDialog(this, MessagesUtil.get("InsufficientFunds",
                        SwingUtil.formatValue(Config.DELEGATE_BURN_AMOUNT + Config.MIN_TRANSACTION_FEE_SOFT)));
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        MessagesUtil.get("DelegateRegistrationInfo",
                                SwingUtil.formatValue(Config.DELEGATE_BURN_AMOUNT)),
                        MessagesUtil.get("ConfirmDelegateRegistration"), JOptionPane.YES_NO_OPTION);
                if (ret != JOptionPane.YES_OPTION) {
                    break;
                }

                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = TransactionType.DELEGATE;
                byte[] from = a.getKey().toAddress();
                byte[] to = from;
                long value = Config.DELEGATE_BURN_AMOUNT;
                long fee = Config.MIN_TRANSACTION_FEE_HARD;
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = Bytes.of(name);
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(a.getKey());

                sendTransaction(pendingMgr, tx);
            }
            break;
        }
        default:
            throw new UnreachableException();
        }
    }

    private void sendTransaction(PendingManager pendingMgr, Transaction tx) {
        if (pendingMgr.addTransactionSync(tx)) {
            JOptionPane.showMessageDialog(this, MessagesUtil.get("TransactionSent"));
            clear();
        } else {
            JOptionPane.showMessageDialog(this, MessagesUtil.get("TransactionFailed"));
        }
    }

    private Account getSelectedAccount() {
        int idx = from.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    private Delegate getSelectedDelegate() {
        int row = table.getSelectedRow();
        return (row == -1) ? null : tableModel.getRow(table.convertRowIndexToModel(row));
    }

    private void refreshAccounts() {
        List<String> accounts = new ArrayList<>();
        List<Account> list = model.getAccounts();
        for (int i = 0; i < list.size(); i++) {
            accounts.add(
                    MessagesUtil.get("AccountNumShort") + i + ", " + SwingUtil.formatValue(list.get(i).getBalance()));
        }

        /*
         * update account list. NOTE: assuming account index will never change
         */
        int idx = from.getSelectedIndex();

        from.removeAllItems();
        for (String item : accounts) {
            from.addItem(item);
        }
        from.setSelectedIndex(idx >= 0 && idx < accounts.size() ? idx : 0);
    }

    private void refreshDelegates() {
        List<Delegate> delegates = model.getDelegates();
        delegates.sort((d1, d2) -> {
            int c = Long.compare(d2.getVotes(), d1.getVotes());
            return c != 0 ? c : new String(d1.getName()).compareTo(new String(d2.getName()));
        });

        Account acc = getSelectedAccount();
        if (acc != null) {
            byte[] voter = acc.getKey().toAddress();
            Blockchain chain = Kernel.getInstance().getBlockchain();
            DelegateState ds = chain.getDeleteState();
            for (Delegate d : delegates) {
                long vote = ds.getVote(voter, d.getAddress());
                d.setVotesFromMe(vote);
                d.setNumberOfBlocksForged(chain.getNumberOfBlocksForged(d.getAddress()));
                d.setNumberOfTurnsHit(chain.getNumberOfTurnsHit(d.getAddress()));
                d.setNumberOfTurnsMissed(chain.getNumberOfTurnsMissed(d.getAddress()));
            }
        }

        /*
         * update table model
         */
        Delegate d = getSelectedDelegate();
        tableModel.setData(delegates);

        if (d != null) {
            for (int i = 0; i < delegates.size(); i++) {
                if (Arrays.equals(d.getAddress(), delegates.get(i).getAddress())) {
                    table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
                    break;
                }
            }
        }
    }

    private void clear() {
        textVote.setText("");
        textUnvote.setText("");
        textName.setText("");
    }
}
