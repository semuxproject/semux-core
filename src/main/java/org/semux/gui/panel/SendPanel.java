package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import org.semux.gui.SwingUtil;
import org.semux.utils.Bytes;
import org.semux.utils.UnreachableException;

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
        from.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel("To:");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        to = SwingUtil.editableTextField();
        to.setColumns(24);
        to.setActionCommand(Action.SEND.name());
        to.addActionListener(this);

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        amount = SwingUtil.editableTextField();
        amount.setColumns(10);
        amount.setActionCommand(Action.SEND.name());
        amount.addActionListener(this);

        JLabel lblFee = new JLabel("Fee:");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);

        fee = SwingUtil.editableTextField();
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
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(paySend, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                            .addGap(28)
                            .addComponent(payClear, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap())
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(to, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                                .addComponent(from, 0, 306, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(fee)
                                        .addComponent(amount, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
                                    .addGap(12)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblSem1)
                                        .addComponent(lblSem2))))
                            .addGap(59))))
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
                    .addGap(27)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(paySend)
                        .addComponent(payClear))
                    .addContainerGap(158, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
        // @formatter:on

        refresh();
        clear();
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
            Account acc = getSelectedAccount();
            long value = getAmount();
            long fee = getFee();
            byte[] to = Hex.parse(getTo());

            if (acc == null) {
                JOptionPane.showMessageDialog(this, "Please select an account!");
            } else if (fee < Config.MIN_TRANSACTION_FEE) {
                JOptionPane.showMessageDialog(this, "Transaction fee is too low!");
            } else if (value + fee > acc.getBalance()) {
                JOptionPane.showMessageDialog(this, "Insufficient funds!");
            } else if (to.length != 20) {
                JOptionPane.showMessageDialog(this, "Invalid receiving address!");
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to transfer " + value / Unit.SEM + " SEM to 0x" + Hex.encode(to) + "?",
                        "Confirm transfer", JOptionPane.YES_NO_OPTION);
                if (ret != JOptionPane.YES_OPTION) {
                    break;
                }

                Kernel kernel = Kernel.getInstance();
                PendingManager pendingMgr = kernel.getPendingManager();

                TransactionType type = TransactionType.TRANSFER;
                byte[] from = acc.getKey().toAddress();
                long nonce = pendingMgr.getNonce(from);
                long timestamp = System.currentTimeMillis();
                byte[] data = {};
                Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
                tx.sign(acc.getKey());

                sendTransaction(pendingMgr, tx);
            }
            break;
        case CLEAR:
            clear();
            break;
        default:
            throw new UnreachableException();
        }
    }

    private void sendTransaction(PendingManager pendingMgr, Transaction tx) {
        if (pendingMgr.addTransactionSync(tx)) {
            JOptionPane.showMessageDialog(this, "Transaction sent. It takes at least 20s to get processed!");
            clear();
        } else {
            JOptionPane.showMessageDialog(this, "Transaction failed!");
        }
    }

    private Account getSelectedAccount() {
        int idx = from.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    private void refresh() {
        List<String> accounts = new ArrayList<>();
        List<Account> list = model.getAccounts();
        for (int i = 0; i < list.size(); i++) {
            accounts.add("0x" + list.get(i).getKey().toAddressString() + ", #" + i + ", "
                    + list.get(i).getBalance() / Unit.SEM + " SEM");
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

    private void clear() {
        setTo(Bytes.EMPY_BYTES);
        setAmount(0);
        setFee(Config.MIN_TRANSACTION_FEE);
    }
}
