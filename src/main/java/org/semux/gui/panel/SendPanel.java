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
import org.semux.crypto.CryptoException;
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

    private JComboBox<Item> selectFrom;
    private JTextField txtTo;
    private JTextField txtAmount;
    private JTextField txtFee;
    private JTextField txtData;

    public SendPanel(SemuxGUI gui, JFrame frame) {
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();
        this.config = kernel.getConfig();

        this.frame = frame;

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(GUIMessages.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        selectFrom = new JComboBox<>();
        selectFrom.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(GUIMessages.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        txtTo = SwingUtil.textFieldWithCopyPastePopup();
        txtTo.setName("txtTo");
        txtTo.setColumns(24);
        txtTo.setActionCommand(Action.SEND.name());
        txtTo.addActionListener(this);

        JLabel lblAmount = new JLabel(GUIMessages.get("Amount") + ":");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        txtAmount = SwingUtil.textFieldWithCopyPastePopup();
        txtAmount.setName("txtAmount");
        txtAmount.setColumns(10);
        txtAmount.setActionCommand(Action.SEND.name());
        txtAmount.addActionListener(this);

        JLabel lblFee = new JLabel(GUIMessages.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(GUIMessages.get("FeeTip", SwingUtil.formatValue(config.minTransactionFee())));

        txtFee = SwingUtil.textFieldWithCopyPastePopup();
        txtFee.setName("txtFee");
        txtFee.setColumns(10);
        txtFee.setActionCommand(Action.SEND.name());
        txtFee.addActionListener(this);

        JLabel lblData = new JLabel(GUIMessages.get("Data") + ":");
        lblData.setHorizontalAlignment(SwingConstants.RIGHT);
        lblData.setToolTipText(GUIMessages.get("DataTip"));

        txtData = SwingUtil.textFieldWithCopyPastePopup();
        txtData.setName("txtData");
        txtData.setColumns(10);
        txtData.setActionCommand(Action.SEND.name());
        txtData.addActionListener(this);
        txtData.setToolTipText(GUIMessages.get("DataTip"));

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton btnSend = new JButton(GUIMessages.get("Send"));
        btnSend.setName("btnSend");
        btnSend.addActionListener(this);
        btnSend.setActionCommand(Action.SEND.name());

        JButton btnClear = new JButton(GUIMessages.get("Clear"));
        btnClear.setName("btnClear");
        btnClear.addActionListener(this);
        btnClear.setActionCommand(Action.CLEAR.name());

        JButton btnAddressBook = new JButton(GUIMessages.get("AddressBook"));
        btnAddressBook.setName("btnAddressBook");
        btnAddressBook.addActionListener(this);
        btnAddressBook.setActionCommand(Action.SHOW_ADDRESS_BOOK.name());

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
                                .addComponent(txtTo, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                                .addComponent(selectFrom, 0, 306, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(txtAmount, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                        .addComponent(txtFee)
                                        .addComponent(txtData))
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
                        .addComponent(selectFrom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(txtTo, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAmount)
                        .addComponent(txtAmount, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem1))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(txtFee, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem2))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(txtData, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
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
        return txtTo.getText().trim();
    }

    public void setToText(byte[] address) {
        txtTo.setText(Hex.encode(address));
    }

    public long getAmountText() throws ParseException {
        return SwingUtil.parseValue(txtAmount.getText().trim());
    }

    public void setAmountText(long a) {
        txtAmount.setText(SwingUtil.formatValue(a, false));
    }

    public long getFeeText() throws ParseException {
        return SwingUtil.parseValue(txtFee.getText().trim());
    }

    public void setFeeText(long f) {
        txtFee.setText(SwingUtil.formatValue(f, false));
    }

    public String getDataText() {
        return txtData.getText().trim();
    }

    public void setDataText(String dataText) {
        txtTo.setText(dataText.trim());
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case SEND:
            send();
            break;
        case CLEAR:
            clear();
            break;
        case SHOW_ADDRESS_BOOK:
            showAddressBook();
            break;
        default:
            throw new UnreachableException();
        }
    }

    /**
     * Refreshes the GUI.
     */
    protected void refresh() {
        List<WalletAccount> list = model.getAccounts();

        /*
         * update account list.
         */
        Object selected = selectFrom.getSelectedItem();
        String address = null;
        if (selected != null && selected instanceof Item) {
            address = ((Item) selected).account.getKey().toAddressString();
        }

        selectFrom.removeAllItems();
        for (int i = 0; i < list.size(); i++) {
            selectFrom.addItem(new Item(list.get(i), i));
        }

        if (address == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            String addr = list.get(i).getKey().toAddressString();
            if (addr.equals(address)) {
                selectFrom.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Sends transaction.
     */
    protected void send() {
        try {
            WalletAccount acc = getSelectedAccount();
            long value = getAmountText();
            long fee = getFeeText();
            String data = getDataText();

            // decode0x recipient address
            byte[] to = Hex.decode0x(getToText());

            if (acc == null) {
                showErrorDialog(GUIMessages.get("SelectAccount"));
            } else if (value <= 0L) {
                showErrorDialog(GUIMessages.get("EnterValidValue"));
            } else if (fee < config.minTransactionFee()) {
                showErrorDialog(GUIMessages.get("TransactionFeeTooLow"));
            } else if (value + fee > acc.getAvailable()) {
                showErrorDialog(GUIMessages.get("InsufficientFunds", SwingUtil.formatValue(value + fee)));
            } else if (to.length != EdDSA.ADDRESS_LEN) {
                showErrorDialog(GUIMessages.get("InvalidReceivingAddress"));
            } else if (Bytes.of(data).length > config.maxTransferDataSize()) {
                showErrorDialog(GUIMessages.get("InvalidData", config.maxTransferDataSize()));
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        GUIMessages.get("TransferInfo", SwingUtil.formatValue(value), Hex.encode0x(to)),
                        GUIMessages.get("ConfirmTransfer"), JOptionPane.YES_NO_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    PendingManager pendingMgr = kernel.getPendingManager();

                    TransactionType type = TransactionType.TRANSFER;
                    byte[] from = acc.getKey().toAddress();
                    long nonce = pendingMgr.getNonce(from);
                    long timestamp = System.currentTimeMillis();
                    Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, Bytes.of(data));
                    tx.sign(acc.getKey());

                    sendTransaction(pendingMgr, tx);
                }
            }
        } catch (ParseException | CryptoException ex) {
            showErrorDialog(GUIMessages.get("EnterValidValue"));
        }
    }

    /**
     * Clears all input fields.
     */
    protected void clear() {
        setToText(Bytes.EMPTY_BYTES);
        setAmountText(0);
        setFeeText(config.minTransactionFee());
        setDataText("");
    }

    /**
     * Shows the address book.
     */
    protected void showAddressBook() {
        AddressBookDialog dialog = new AddressBookDialog(frame, model);
        dialog.setVisible(true);
    }

    /**
     * Returns the selected account.
     * 
     * @return
     */
    protected WalletAccount getSelectedAccount() {
        int idx = selectFrom.getSelectedIndex();
        return (idx == -1) ? null : model.getAccounts().get(idx);
    }

    /**
     * Adds a transaction to the pending manager.
     * 
     * @param pendingMgr
     * @param tx
     */
    protected void sendTransaction(PendingManager pendingMgr, Transaction tx) {
        PendingManager.ProcessTransactionResult result = pendingMgr.addTransactionSync(tx);
        if (result.error == null) {
            JOptionPane.showMessageDialog(
                    this,
                    GUIMessages.get("TransactionSent", 30),
                    GUIMessages.get("SuccessDialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            clear();
        } else {
            showErrorDialog(GUIMessages.get("TransactionFailed", result.error.toString()));
        }
    }

    /**
     * Shows an error dialog.
     * 
     * @param message
     */
    protected void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                GUIMessages.get("ErrorDialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Represents an item in the account drop list.
     */
    protected static class Item {
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