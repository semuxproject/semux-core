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

import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.SemuxGUI;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.util.Bytes;
import org.semux.util.exception.UnreachableException;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;
    private transient WalletModel model;

    private transient Kernel kernel;
    private transient Config config;

    private JComboBox<Item> fromComboBox;
    private JTextField toText;
    private JTextField amountText;
    private JTextField feeText;
    private JTextField dataText;

    public SendPanel(SemuxGUI gui, JFrame frame) {
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();
        this.config = kernel.getConfig();

        this.frame = frame;

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(GUIMessages.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        fromComboBox = new JComboBox<>();
        fromComboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(GUIMessages.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        toText = SwingUtil.textFieldWithCopyPastePopup();
        toText.setName("toText");
        toText.setColumns(24);
        toText.setActionCommand(Action.SEND.name());
        toText.addActionListener(this);

        JLabel lblAmount = new JLabel(GUIMessages.get("Amount") + ":");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        amountText = SwingUtil.textFieldWithCopyPastePopup();
        amountText.setName("amountText");
        amountText.setColumns(10);
        amountText.setActionCommand(Action.SEND.name());
        amountText.addActionListener(this);

        JLabel lblFee = new JLabel(GUIMessages.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(GUIMessages.get("FeeTip", SwingUtil.formatValue(config.minTransactionFee())));

        feeText = SwingUtil.textFieldWithCopyPastePopup();
        feeText.setColumns(10);
        feeText.setActionCommand(Action.SEND.name());
        feeText.addActionListener(this);

        JLabel lblData = new JLabel(GUIMessages.get("Data") + ":");
        lblData.setHorizontalAlignment(SwingConstants.RIGHT);
        lblData.setToolTipText(GUIMessages.get("DataTip"));

        dataText = SwingUtil.textFieldWithCopyPastePopup();
        dataText.setColumns(10);
        dataText.setActionCommand(Action.SEND.name());
        dataText.addActionListener(this);
        dataText.setToolTipText(GUIMessages.get("DataTip"));

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton btnSend = new JButton(GUIMessages.get("Send"));
        btnSend.setName("sendButton");
        btnSend.addActionListener(this);
        btnSend.setActionCommand(Action.SEND.name());

        JButton btnClear = new JButton(GUIMessages.get("Clear"));
        btnClear.addActionListener(this);
        btnClear.setActionCommand(Action.CLEAR.name());

        JButton btnAddressBook = new JButton(GUIMessages.get("AddressBook"));
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
                        .addComponent(lblData))
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
                                        .addComponent(dataText))
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
                        .addComponent(lblData)
                        .addComponent(dataText, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
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

    // TODO: clean methods below

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

    public String getDataText() {
        return dataText.getText().trim();
    }

    public void setDataText(String dataText) {
        toText.setText(dataText.trim());
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
                String data = getDataText();

                // decode0x recipient address
                byte[] to = Hex.decode0x(getToText());

                if (acc == null) {
                    JOptionPane.showMessageDialog(this, GUIMessages.get("SelectAccount"));
                } else if (value <= 0L) {
                    JOptionPane.showMessageDialog(this, GUIMessages.get("EnterValidValue"));
                } else if (fee < config.minTransactionFee()) {
                    JOptionPane.showMessageDialog(this, GUIMessages.get("TransactionFeeTooLow"));
                } else if (value + fee > acc.getAvailable()) {
                    JOptionPane.showMessageDialog(this,
                            GUIMessages.get("InsufficientFunds", SwingUtil.formatValue(value + fee)));
                } else if (to.length != EdDSA.ADDRESS_LEN) {
                    JOptionPane.showMessageDialog(this, GUIMessages.get("InvalidReceivingAddress"));
                } else if (Bytes.of(data).length > 128) {
                    JOptionPane.showMessageDialog(this, GUIMessages.get("InvalidData", 128));
                } else {
                    int ret = JOptionPane.showConfirmDialog(this,
                            GUIMessages.get("TransferInfo", SwingUtil.formatValue(value), Hex.encode0x(to)),
                            GUIMessages.get("ConfirmTransfer"), JOptionPane.YES_NO_OPTION);
                    if (ret != JOptionPane.YES_OPTION) {
                        break;
                    }

                    PendingManager pendingMgr = kernel.getPendingManager();

                    TransactionType type = TransactionType.TRANSFER;
                    byte[] from = acc.getKey().toAddress();
                    long nonce = pendingMgr.getNonce(from);
                    long timestamp = System.currentTimeMillis();
                    Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, Bytes.of(data));
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
            JOptionPane.showMessageDialog(this, GUIMessages.get("TransactionSent", 30));
            clear();
        } else {
            JOptionPane.showMessageDialog(this, GUIMessages.get("TransactionFailed"));
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
        setFeeText(config.minTransactionFee());
        setDataText("");
    }

    private class Item {
        WalletAccount account;
        String name;

        public Item(WalletAccount a, int idx) {
            this.account = a;
            this.name = Hex.PREF + account.getKey().toAddressString() + ", " + GUIMessages.get("AccountNumShort", idx)
                    + ", " + SwingUtil.formatValue(account.getAvailable());
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}