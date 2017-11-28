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
    private transient WalletModel model;

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

    private JComboBox<Item> fromComboBox;
    private JTextField toText;
    private JTextField amountText;
    private JTextField feeText;
    private JTextField memoText;

    public SendPanel(JFrame frame, WalletModel model) {
        this.frame = frame;
        this.model = model;
        this.model.addListener(this);
        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(MessagesUtil.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        fromComboBox = new JComboBox<>();
        fromComboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(MessagesUtil.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        toText = SwingUtil.textFieldWithCopyPastePopup();
        toText.setColumns(24);
        toText.setActionCommand(Action.SEND.name());
        toText.addActionListener(this);

        JLabel lblAmount = new JLabel(MessagesUtil.get("Amount") + ":");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        amountText = SwingUtil.textFieldWithCopyPastePopup();
        amountText.setColumns(10);
        amountText.setActionCommand(Action.SEND.name());
        amountText.addActionListener(this);

        JLabel lblFee = new JLabel(MessagesUtil.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(MessagesUtil.get("FeeTip", SwingUtil.formatValue(Config.MIN_TRANSACTION_FEE)));

        feeText = SwingUtil.textFieldWithCopyPastePopup();
        feeText.setColumns(10);
        feeText.setActionCommand(Action.SEND.name());
        feeText.addActionListener(this);

        JLabel lblMemo = new JLabel(MessagesUtil.get("Memo") + ":");
        lblMemo.setHorizontalAlignment(SwingConstants.RIGHT);
        lblMemo.setToolTipText(MessagesUtil.get("MemoTip"));

        memoText = SwingUtil.textFieldWithCopyPastePopup();
        memoText.setColumns(10);
        memoText.setActionCommand(Action.SEND.name());
        memoText.addActionListener(this);

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
                                .addComponent(toText, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                                .addComponent(fromComboBox, 0, 306, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(amountText, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                        .addComponent(feeText)
                                        .addComponent(memoText))
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
                        .addComponent(fromComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(toText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAmount)
                        .addComponent(amountText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem1))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(feeText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem2))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblMemo)
                        .addComponent(memoText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
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

    public String getToText() {
        return toText.getText().trim();
    }

    public void setToText(byte[] addr) {
        toText.setText(Hex.encode(addr));
    }

    public long getAmountText() throws ParseException {
        return SwingUtil.parseValue(amountText.getText().trim());
    }

    public void setAmountText(long a) {
        amountText.setText(SwingUtil.formatValue(a, false));
    }

    public long getFeeText() throws ParseException {
        return SwingUtil.parseValue(feeText.getText().trim());
    }

    public void setFeeText(long f) {
        feeText.setText(SwingUtil.formatValue(f, false));
    }

    public String getMemoText() {
        return memoText.getText().trim();
    }

    public void setMemoText(String memoText) {
        toText.setText(memoText.trim());
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
                long value = getAmountText();
                long fee = getFeeText();
                String memo = getMemoText();
                byte[] to = Hex.parse(getToText());

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
                            MessagesUtil.get("TransferInfo", SwingUtil.formatValue(value), Hex.encodeWithPrefix(to)),
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
        int idx = fromComboBox.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    private void refresh() {
        List<WalletAccount> list = model.getAccounts();

        /*
         * update account list.
         */
        Object selected = fromComboBox.getSelectedItem();
        String address = null;
        if (selected != null && selected instanceof Item) {
            address = ((Item) selected).account.getKey().toAddressString();
        }

        fromComboBox.removeAllItems();
        for (int i = 0; i < list.size(); i++) {
            fromComboBox.addItem(new Item(list.get(i), i));
        }

        if (address == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            String addr = list.get(i).getKey().toAddressString();
            if (addr.equals(address)) {
                fromComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clear() {
        setToText(Bytes.EMPTY_BYTES);
        setAmountText(0);
        setFeeText(Config.MIN_TRANSACTION_FEE);
        setMemoText("");
    }
}