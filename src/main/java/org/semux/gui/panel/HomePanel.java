package org.semux.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;
import org.semux.utils.ByteArray;

public class HomePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Model model;

    private JLabel blockNum;
    private JLabel status;
    private JLabel balance;
    private JLabel locked;

    private JPanel transactions;

    public HomePanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        // setup overview panel
        JPanel overview = new JPanel();
        overview.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Overview", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        overview.setLayout(new GridLayout(4, 2, 0, 0));

        JLabel labelBlockNum = new JLabel("Block #:");
        overview.add(labelBlockNum);

        blockNum = new JLabel("");
        overview.add(blockNum);

        JLabel labelStatus = new JLabel("Status:");
        overview.add(labelStatus);

        status = new JLabel("");
        overview.add(status);

        JLabel labelBalance = new JLabel("Balance:");
        overview.add(labelBalance);

        balance = new JLabel("");
        overview.add(balance);

        JLabel labelLocked = new JLabel("Locked:");
        overview.add(labelLocked);

        locked = new JLabel("");
        overview.add(locked);

        // setup transactions panel
        transactions = new JPanel();
        transactions.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(10, 10, 10, 10)),
                "Transactions", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(overview, GroupLayout.PREFERRED_SIZE, 294, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                        .addComponent(overview, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE))
                    .addGap(0))
        );
        transactions.setLayout(new BoxLayout(transactions, BoxLayout.Y_AXIS));
        // @formatter:on

        setLayout(groupLayout);

        refresh();
    }

    public static class TransactionPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        public TransactionPanel(Transaction tx, boolean isInbound) {
            this.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel lblType = new JLabel("");
            lblType.setIcon(SwingUtil.loadImage(isInbound ? "inbound" : "outbound", 48, 48));

            JLabel lblAmount = new JLabel(
                    String.format("%s%.3f SEM", isInbound ? "+" : "-", tx.getValue() / (double) Unit.SEM));
            lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

            SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm:ss");
            JLabel lblTime = new JLabel(df.format(new Date(tx.getTimestamp())));

            JLabel lblFrom = new JLabel("0x" + Hex.encode(isInbound ? tx.getFrom() : tx.getTo()));
            lblFrom.setForeground(Color.GRAY);

            // @formatter:off
            GroupLayout groupLayout = new GroupLayout(this);
            groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblType)
                        .addGap(18)
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(lblTime, GroupLayout.PREFERRED_SIZE, 169, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED, 87, Short.MAX_VALUE)
                                .addComponent(lblAmount, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE))
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(lblFrom, GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                .addContainerGap())))
            );
            groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(groupLayout.createSequentialGroup()
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(lblTime, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblAmount, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblFrom))
                            .addComponent(lblType, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
            );
            this.setLayout(groupLayout);
            // @formatter:on
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
        this.blockNum.setText(Long.toString(model.getLatestBlockNumber()));
        this.status.setText(model.isDelegate() ? "Delegate" : "Normal");
        this.balance.setText(String.format("%.3f SEM", model.getTotalBalance() / (double) Unit.SEM));
        this.locked.setText(String.format("%.3f SEM", model.getTotalLocked() / (double) Unit.SEM));

        List<Transaction> list = new ArrayList<>();
        for (Account acc : model.getAccounts()) {
            for (Transaction tx : acc.getTransactions()) {
                if (TransactionType.TRANSFER.equals(tx.getType())) {
                    list.add(tx);
                }
            }
        }
        list.sort((tx1, tx2) -> {
            return Long.compare(tx2.getTimestamp(), tx1.getTimestamp());
        });
        list = list.size() > 6 ? list.subList(0, 6) : list;

        Set<ByteArray> set = new HashSet<>();
        for (Account a : model.getAccounts()) {
            set.add(ByteArray.of(a.getAddress().toAddress()));
        }

        transactions.removeAll();
        for (Transaction tx : list) {
            transactions.add(new TransactionPanel(tx, set.contains(ByteArray.of(tx.getTo()))));
        }
        transactions.revalidate();
    }
}
