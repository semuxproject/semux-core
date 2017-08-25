package org.semux.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

import org.semux.CLI;
import org.semux.core.Account;
import org.semux.core.Delegate;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.SwingUtil;

public class HomePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JLabel blockNum;
    private JLabel status;
    private JLabel balance;
    private JLabel locked;

    public HomePanel(Model model) {
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
        JPanel transactions = new JPanel();
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

        public TransactionPanel() {
            this.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel lblType = new JLabel("");
            lblType.setIcon(SwingUtil.loadImage("send", 48, 48));

            JLabel lblAmount = new JLabel("+12.3456 SEM");
            lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

            JLabel lblTime = new JLabel("2017-01-02 12:00 PM");

            JLabel lblFrom = new JLabel("0x1122334455667788112233445566778811223344");
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
        // update block number
        blockNum.setText(Long.toString(CLI.chain.getLatestBlockNumber()));

        // update status
        Wallet w = Wallet.getInstance();
        DelegateState ds = CLI.chain.getDeleteState();
        Delegate d = ds.getDelegateByAddress(CLI.coinbase.toAddress());
        status.setText(d == null ? "Normal" : "Delegate");

        // update balance
        AccountState as = CLI.chain.getAccountState();
        long balanceSEM = 0;
        long lockedSEM = 0;
        for (EdDSA key : w.getAccounts()) {
            Account a = as.getAccount(key.toAddress());
            balanceSEM += a.getBalance();
            lockedSEM += a.getLocked();
        }
        this.balance.setText(String.format("%.3f SEM", balanceSEM / (double) Unit.SEM));
        this.locked.setText(String.format("%.3f SEM", lockedSEM / (double) Unit.SEM));
    }
}
