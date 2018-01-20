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
import java.util.Arrays;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.semux.Kernel;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.gui.dialog.AddressBookDialog;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.util.Bytes;
import org.semux.util.exception.UnreachableException;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;
    private transient WalletModel model;

    private transient Kernel kernel;
    private transient Config config;

    private JComboBox<AccountItem> selectFrom;
    private JTextField txtTo;
    private JTextField txtAmount;
    private JTextField txtFee;
    private JTextField txtData;
    private JRadioButton rdbtnText;
    private JRadioButton rdbtnHex;

    private AddressBookDialog addressBookDialog;

    public SendPanel(SemuxGui gui, JFrame frame) {
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();
        this.config = kernel.getConfig();

        this.frame = frame;

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(GuiMessages.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        selectFrom = new JComboBox<>();
        selectFrom.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(GuiMessages.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        txtTo = SwingUtil.textFieldWithCopyPastePopup();
        txtTo.setName("txtTo");
        txtTo.setColumns(24);
        txtTo.setActionCommand(Action.SEND.name());
        txtTo.addActionListener(this);

        JLabel lblAmount = new JLabel(GuiMessages.get("Amount") + ":");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        txtAmount = SwingUtil.textFieldWithCopyPastePopup();
        txtAmount.setName("txtAmount");
        txtAmount.setColumns(10);
        txtAmount.setActionCommand(Action.SEND.name());
        txtAmount.addActionListener(this);

        JLabel lblFee = new JLabel(GuiMessages.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(GuiMessages.get("FeeTip", SwingUtil.formatValue(config.minTransactionFee())));

        txtFee = SwingUtil.textFieldWithCopyPastePopup();
        txtFee.setName("txtFee");
        txtFee.setColumns(10);
        txtFee.setActionCommand(Action.SEND.name());
        txtFee.addActionListener(this);

        JLabel lblData = new JLabel(GuiMessages.get("Data") + ":");
        lblData.setHorizontalAlignment(SwingConstants.RIGHT);
        lblData.setToolTipText(GuiMessages.get("DataTip"));

        txtData = SwingUtil.textFieldWithCopyPastePopup();
        txtData.setName("txtData");
        txtData.setColumns(10);
        txtData.setActionCommand(Action.SEND.name());
        txtData.addActionListener(this);
        txtData.setToolTipText(GuiMessages.get("DataTip"));

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton btnSend = new JButton(GuiMessages.get("Send"));
        btnSend.setName("btnSend");
        btnSend.addActionListener(this);
        btnSend.setActionCommand(Action.SEND.name());

        JButton btnClear = new JButton(GuiMessages.get("Clear"));
        btnClear.setName("btnClear");
        btnClear.addActionListener(this);
        btnClear.setActionCommand(Action.CLEAR.name());

        JButton btnAddressBook = new JButton(GuiMessages.get("AddressBook"));
        btnAddressBook.setName("btnAddressBook");
        btnAddressBook.addActionListener(this);
        btnAddressBook.setActionCommand(Action.SHOW_ADDRESS_BOOK.name());

        rdbtnText = new JRadioButton(GuiMessages.get("Text"));
        rdbtnText.setSelected(true);
        rdbtnHex = new JRadioButton(GuiMessages.get("Hex"));
        ButtonGroup btnGroupDataType = new ButtonGroup();
        btnGroupDataType.add(rdbtnText);
        btnGroupDataType.add(rdbtnHex);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(62)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblFrom)
                        .addComponent(lblTo)
                        .addComponent(lblAmount)
                        .addComponent(lblFee)
                        .addComponent(lblData))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnClear)
                            .addGap(10)
                            .addComponent(btnSend))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(selectFrom, 0, 400, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(txtAmount, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                        .addComponent(txtFee)
                                        .addComponent(txtData))
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addGap(12)
                                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                .addComponent(lblSem1)
                                                .addComponent(lblSem2)))
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(rdbtnText)
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(rdbtnHex))))
                                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                                    .addComponent(txtTo, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                    .addGap(18)
                                    .addComponent(btnAddressBook)))
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
                        .addComponent(txtTo, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnAddressBook))
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
                        .addComponent(txtData, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addComponent(rdbtnText)
                        .addComponent(rdbtnHex))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnSend)
                        .addComponent(btnClear))
                    .addContainerGap(30, Short.MAX_VALUE))
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
        txtData.setText(dataText.trim());
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

        // quit if no update
        boolean match = selectFrom.getItemCount() == list.size();
        if (match) {
            for (int i = 0; i < list.size(); i++) {
                if (!Arrays.equals(selectFrom.getItemAt(i).account.getAddress(), list.get(i).getAddress())) {
                    match = false;
                    break;
                }
                // check if name updated
                if (!selectFrom.getItemAt(i).account.getName().equals(list.get(i).getName())) {
                    match = false;
                    break;
                }
            }
        }

        if (!match) {
            // record selected account
            AccountItem selected = (AccountItem) selectFrom.getSelectedItem();

            // update account list
            selectFrom.removeAllItems();
            for (int i = 0; i < list.size(); i++) {
                selectFrom.addItem(new AccountItem(list.get(i)));
            }

            // recover selected account
            if (selected != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Arrays.equals(list.get(i).getAddress(), selected.account.getAddress())) {
                        selectFrom.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        // refresh address book dialog regardless
        if (addressBookDialog != null) {
            addressBookDialog.refresh();
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
                showErrorDialog(GuiMessages.get("SelectAccount"));
            } else if (value <= 0L) {
                showErrorDialog(GuiMessages.get("EnterValidValue"));
            } else if (fee < config.minTransactionFee()) {
                showErrorDialog(GuiMessages.get("TransactionFeeTooLow"));
            } else if (value + fee > acc.getAvailable()) {
                showErrorDialog(GuiMessages.get("InsufficientFunds", SwingUtil.formatValue(value + fee)));
            } else if (to.length != Key.ADDRESS_LEN) {
                showErrorDialog(GuiMessages.get("InvalidReceivingAddress"));
            } else if (Bytes.of(data).length > config.maxTransactionDataSize(TransactionType.TRANSFER)) {
                showErrorDialog(
                        GuiMessages.get("InvalidData", config.maxTransactionDataSize(TransactionType.TRANSFER)));
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        GuiMessages.get("TransferInfo", SwingUtil.formatValue(value), Hex.encode0x(to)),
                        GuiMessages.get("ConfirmTransfer"), JOptionPane.YES_NO_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    PendingManager pendingMgr = kernel.getPendingManager();

                    byte[] rawData = rdbtnText.isSelected() ? Bytes.of(data) : Hex.decode0x(data);

                    Network network = kernel.getConfig().network();
                    TransactionType type = TransactionType.TRANSFER;
                    byte[] from = acc.getKey().toAddress();
                    long nonce = pendingMgr.getNonce(from);
                    long timestamp = System.currentTimeMillis();
                    Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, rawData);
                    tx.sign(acc.getKey());

                    sendTransaction(pendingMgr, tx);
                }
            }
        } catch (ParseException | CryptoException ex) {
            showErrorDialog(GuiMessages.get("EnterValidValue"));
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
        if (addressBookDialog == null) {
            addressBookDialog = new AddressBookDialog(frame, model, kernel.getWallet());
        }

        addressBookDialog.setVisible(true);
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
                    GuiMessages.get("TransactionSent", 30),
                    GuiMessages.get("SuccessDialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            clear();
            model.fireUpdateEvent();
        } else {
            showErrorDialog(GuiMessages.get("TransactionFailed", result.error.toString()));
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
                GuiMessages.get("ErrorDialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Represents an item in the account drop list.
     */
    protected static class AccountItem {
        WalletAccount account;
        String name;

        public AccountItem(WalletAccount a) {
            this.account = a;
            this.name = Hex.PREF + account.getKey().toAddressString() + ", " // address
                    + (account.getName().isPresent() ? account.getName().get() + ", " : "") // alias
                    + SwingUtil.formatValue(account.getAvailable()); // available
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}