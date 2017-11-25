/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
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
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.util.Bytes;
import org.semux.util.UnreachableException;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;
    private WalletModel model;

    private class Item {
        WalletAccount account;
        String name;

        public Item(WalletAccount a, int idx) {
            this.account = a;
            this.name = Hex.PREF + account.getKey().toAddressString() + ", " + MessagesUtil.get("AccountNumShort", idx)
                    + ", " + SwingUtil.formatValue(account.getAvailable());
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private JComboBox<Item> from;
    private JTextField to;
    private JTextField amount;
    private JTextField fee;
    private JTextField memo;

    public SendPanel(JFrame frame, WalletModel model) {
        this.frame = frame;
        this.model = model;
        this.model.addListener(this);
        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(MessagesUtil.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        from = new JComboBox<>();
        from.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(MessagesUtil.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        to = SwingUtil.textFieldWithCopyPastePopup();
        to.setColumns(24);
        to.setActionCommand(Action.SEND.name());
        to.addActionListener(this);

        JLabel lblAmount = new JLabel(MessagesUtil.get("Amount") + ":");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        amount = SwingUtil.textFieldWithCopyPastePopup();
        amount.setColumns(10);
        amount.setActionCommand(Action.SEND.name());
        amount.addActionListener(this);

        JLabel lblFee = new JLabel(MessagesUtil.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(MessagesUtil.get("FeeTip", SwingUtil.formatValue(Config.MIN_TRANSACTION_FEE)));

        fee = SwingUtil.textFieldWithCopyPastePopup();
        fee.setColumns(10);
        fee.setActionCommand(Action.SEND.name());
        fee.addActionListener(this);

        JLabel lblMemo = new JLabel(MessagesUtil.get("Memo") + ":");
        lblMemo.setHorizontalAlignment(SwingConstants.RIGHT);
        lblMemo.setToolTipText(MessagesUtil.get("MemoTip"));

        memo = SwingUtil.textFieldWithCopyPastePopup();
        memo.setColumns(10);
        memo.setActionCommand(Action.SEND.name());
        memo.addActionListener(this);

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton btnSend = new JButton(MessagesUtil.get("Send"));
        btnSend.addActionListener(this);
        btnSend.setActionCommand(Action.SEND.name());

        JButton btnClear = new JButton(MessagesUtil.get("Clear"));
        btnClear.addActionListener(this);
        btnClear.setActionCommand(Action.CLEAR.name());

        JButton btnAddressBook = new JButton(MessagesUtil.get("AddressBook"));
        btnAddressBook.addActionListener(this);
        btnAddressBook.setActionCommand(Action.SHOW_ADDRESSBOOK.name());

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
                        .addComponent(lblFee)
                        .addComponent(lblMemo))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnClear)
                            .addGap(10)
                            .addComponent(btnSend)
                            .addGap(10)
                            .addComponent(btnAddressBook)
                            .addContainerGap())
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(to, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                                .addComponent(from, 0, 306, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(amount, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                        .addComponent(fee)
                                        .addComponent(memo))
                                    .addGap(12)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblSem1)
                                        .addComponent(lblSem2))))
                            .addGap(59))))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(18)
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
                        .addComponent(lblMemo)
                        .addComponent(memo, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnAddressBook)
                        .addComponent(btnSend)
                        .addComponent(btnClear))
                    .addGap(18)
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

    public long getAmount() throws ParseException {
        return SwingUtil.parseValue(amount.getText().trim());
    }

    public void setAmount(long a) {
        amount.setText(SwingUtil.formatValue(a, false));
    }

    public long getFee() throws ParseException {
        return SwingUtil.parseValue(fee.getText().trim());
    }

    public void setFee(long f) {
        fee.setText(SwingUtil.formatValue(f, false));
    }

    public String getMemo() {
        return memo.getText().trim();
    }

    public void setMemo(String memo) {
        to.setText(memo.trim());
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case SEND:
            try {
                WalletAccount acc = getSelectedAccount();
                long value = getAmount();
                long fee = getFee();
                String memo = getMemo();
                byte[] to = Hex.parse(getTo());

                if (acc == null) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("SelectAccount"));
                } else if (value <= 0L) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("EnterValidValue"));
                } else if (fee < Config.MIN_TRANSACTION_FEE) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("TransactionFeeTooLow"));
                } else if (value + fee > acc.getAvailable()) {
                    JOptionPane.showMessageDialog(this,
                            MessagesUtil.get("InsufficientFunds", SwingUtil.formatValue(value + fee)));
                } else if (to.length != 20) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("InvalidReceivingAddress"));
                } else if (Bytes.of(memo).length > 128) {
                    JOptionPane.showMessageDialog(this, MessagesUtil.get("InvalidMemo", 128));
                } else {
                    int ret = JOptionPane.showConfirmDialog(this,
                            MessagesUtil.get("TransferInfo", SwingUtil.formatValue(value), Hex.PREF + Hex.encode(to)),
                            MessagesUtil.get("ConfirmTransfer"), JOptionPane.YES_NO_OPTION);
                    if (ret != JOptionPane.YES_OPTION) {
                        break;
                    }

                    Kernel kernel = Kernel.getInstance();
                    PendingManager pendingMgr = kernel.getPendingManager();

                    TransactionType type = TransactionType.TRANSFER;
                    byte[] from = acc.getKey().toAddress();
                    long nonce = pendingMgr.getNonce(from);
                    long timestamp = System.currentTimeMillis();
                    byte[] data = Bytes.of(memo);
                    Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
                    tx.sign(acc.getKey());

                    sendTransaction(pendingMgr, tx);
                }
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Exception: " + ex.getMessage());
            }
            break;
        case CLEAR:
            clear();
            break;
        case SHOW_ADDRESSBOOK:
            AddressBookDialog dialog = new AddressBookDialog(frame, model);
            dialog.setVisible(true);
            break;
        default:
            throw new UnreachableException();
        }
    }

    private void sendTransaction(PendingManager pendingMgr, Transaction tx) {
        if (pendingMgr.addTransactionSync(tx)) {
            JOptionPane.showMessageDialog(this, MessagesUtil.get("TransactionSent", 30));
            clear();
        } else {
            JOptionPane.showMessageDialog(this, MessagesUtil.get("TransactionFailed"));
        }
    }

    private WalletAccount getSelectedAccount() {
        int idx = from.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    private void refresh() {
        List<WalletAccount> list = model.getAccounts();

        /*
         * update account list.
         */
        Object selected = from.getSelectedItem();
        String address = null;
        if (selected != null && selected instanceof Item) {
            address = ((Item) selected).account.getKey().toAddressString();
        }

        from.removeAllItems();
        for (int i = 0; i < list.size(); i++) {
            from.addItem(new Item(list.get(i), i));
        }

        if (address == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            String addr = list.get(i).getKey().toAddressString();
            if (addr.equals(address)) {
                from.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clear() {
        setTo(Bytes.EMPTY_BYTES);
        setAmount(0);
        setFee(Config.MIN_TRANSACTION_FEE);
        setMemo("");
    }
}