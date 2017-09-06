package org.semux.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.semux.Config;
import org.semux.Kernel;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.utils.Bytes;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Model model;

    private JComboBox<String> from;
    private JTextField to;
    private JTextField amount;
    private JTextField fee;

    public SendPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel("From:");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        from = new JComboBox<>();

        JLabel lblTo = new JLabel("To:");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        to = new JTextField();
        to.setColumns(24);
        to.setActionCommand(Action.SEND.name());
        to.addActionListener(this);

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        amount = new JTextField();
        amount.setColumns(10);
        amount.setActionCommand(Action.SEND.name());
        amount.addActionListener(this);

        JLabel lblFee = new JLabel("Fee:");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);

        fee = new JTextField();
        fee.setColumns(10);
        fee.setActionCommand(Action.SEND.name());
        fee.addActionListener(this);

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton paySend = new JButton("Send");
        paySend.addActionListener(this);
        paySend.setActionCommand(Action.SEND.name());

        JButton payClear = new JButton("Clear");
        payClear.addActionListener(this);
        payClear.setActionCommand(Action.CLEAR.name());

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(62)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblTo)
                        .addComponent(lblFrom)
                        .addComponent(lblAmount)
                        .addComponent(lblFee))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(to, GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                        .addComponent(from, 0, 305, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(fee)
                                .addComponent(amount, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(lblSem1)
                                .addComponent(lblSem2))))
                    .addGap(59))
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(132)
                    .addComponent(paySend, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(payClear, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(187, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(10)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFrom)
                        .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(to, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAmount)
                        .addComponent(amount, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem1))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(fee, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem2))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(paySend)
                        .addComponent(payClear))
                    .addContainerGap(157, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
        // @formatter:on

        refresh();
        clear();
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

    public String getTo() {
        return to.getText().trim();
    }

    public void setTo(byte[] addr) {
        to.setText(Hex.encode(addr));
    }

    public long getAmount() {
        return (long) (Unit.SEM * Double.parseDouble(amount.getText().trim()));
    }

    public void setAmount(long a) {
        amount.setText(a == 0 ? "" : String.format("%.3f", a / (double) Unit.SEM));
    }

    public long getFee() {
        return (long) (Unit.SEM * Double.parseDouble(fee.getText().trim()));
    }

    public void setFee(long f) {
        fee.setText(f == 0 ? "" : String.format("%.3f", f / (double) Unit.SEM));
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case SEND:
            Account acc = model.getAccounts().get(getFrom());
            long value = getAmount();
            long fee = getFee();
            byte[] to = Hex.parse(getTo());

            if (fee < Config.MIN_TRANSACTION_FEE) {
                JOptionPane.showMessageDialog(this, "Transaction fee is too low!");
            } else if (value + fee > acc.getBalance()) {
                JOptionPane.showMessageDialog(this, "Insufficient funds!");
            } else if (to.length != 20) {
                JOptionPane.showMessageDialog(this, "Invalid receiving address!");
            } else {
                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = TransactionType.TRANSFER;
                byte[] from = acc.getAddress().toAddress();
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = {};
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(acc.getAddress());

                pendingMgr.addTransaction(tx);
                JOptionPane.showMessageDialog(this, "Transaction sent!");
                clear();
            }
            break;
        case CLEAR:
            clear();
            break;
        default:
            break;
        }
    }

    private void refresh() {
        setFromItems(model.getAccounts());
    }

    private void clear() {
        setTo(Bytes.EMPY_BYTES);
        setAmount(0);
        setFee(Config.MIN_TRANSACTION_FEE);
    }
}
