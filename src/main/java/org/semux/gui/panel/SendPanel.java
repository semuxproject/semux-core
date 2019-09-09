/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.semux.config.Config;
import org.semux.core.Amount;
import org.semux.core.PendingManager;
import org.semux.core.TransactionType;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.gui.TransactionSender;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GuiMessages;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.exception.UnreachableException;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final transient SemuxGui gui;
    private final transient WalletModel model;
    private final transient Kernel kernel;
    private final transient Config config;

    private final JComboBox<AccountItem> selectFrom;
    private final JComboBox<AccountItem> selectTo;
    private final JTextField txtValue;
    private final JTextField txtFee;
    private final JTextField txtData;
    private final JRadioButton rdbtnText;
    private final JRadioButton rdbtnHex;

    public SendPanel(SemuxGui gui, JFrame frame) {
        this.gui = gui;
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();
        this.config = kernel.getConfig();

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel(GuiMessages.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        selectFrom = new JComboBox<>();
        selectFrom.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel lblTo = new JLabel(GuiMessages.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        selectTo = SwingUtil.comboBoxWithCopyPastePopup();
        selectTo.setName("selectTo");
        selectTo.setEditable(true);

        JLabel lblValue = new JLabel(GuiMessages.get("Value") + ":");
        lblValue.setHorizontalAlignment(SwingConstants.RIGHT);

        txtValue = SwingUtil.textFieldWithCopyPastePopup();
        txtValue.setName("txtValue");
        txtValue.setColumns(10);
        txtValue.setActionCommand(Action.SEND.name());
        txtValue.addActionListener(this);

        JLabel lblFee = new JLabel(GuiMessages.get("Fee") + ":");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFee.setToolTipText(GuiMessages.get("FeeTip", SwingUtil.formatAmount(config.spec().minTransactionFee())));

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

        JLabel lblUnit1 = new JLabel(SwingUtil.defaultUnit.symbol);
        JLabel lblUnit2 = new JLabel(SwingUtil.defaultUnit.symbol);

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
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblFrom)
                        .addComponent(lblTo)
                        .addComponent(lblValue)
                        .addComponent(lblFee)
                        .addComponent(lblData))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnClear)
                            .addGap(18)
                            .addComponent(btnSend))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(selectFrom)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(txtValue, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                        .addComponent(txtFee)
                                        .addComponent(txtData))
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addGap(12)
                                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                .addComponent(lblUnit1)
                                                .addComponent(lblUnit2)))
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(rdbtnText)
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(rdbtnHex))))
                                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                                    .addComponent(selectTo, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                    .addGap(18)
                                    .addComponent(btnAddressBook)))
                            .addGap(30))))
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
                        .addComponent(selectTo, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnAddressBook))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblValue)
                        .addComponent(txtValue, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblUnit1))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(txtFee, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblUnit2))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(txtData, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
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

    private String getTo() {

        Object selected = selectTo.getSelectedItem();
        String ret = "";

        if (selected instanceof AccountItem) {
            // selected item
            AccountItem accountItem = (AccountItem) selected;
            ret = Hex.encode0x(accountItem.address);
        } else if (selected != null) {
            // manually entered
            return selected.toString().trim();
        }

        return ret;
    }

    public void setTo(byte[] address) {
        selectTo.setSelectedItem(Hex.encode(address));
    }

    public Amount getValue() throws ParseException {
        return SwingUtil.parseAmount(txtValue.getText().trim());
    }

    public void setValue(Amount a) {
        txtValue.setText(SwingUtil.formatAmountNoUnit(a));
    }

    public Amount getFee() throws ParseException {
        return SwingUtil.parseAmount(txtFee.getText().trim());
    }

    public void setFee(Amount f) {
        txtFee.setText(SwingUtil.formatAmountNoUnit(f));
    }

    public String getData() {
        return txtData.getText().trim();
    }

    public void setData(String dataText) {
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

        List<AccountItem> accountItems = new ArrayList<>();
        for (WalletAccount aList : list) {
            AccountItem accountItem = new AccountItem(aList);
            accountItems.add(accountItem);
        }

        // update account list if user is not interacting with it
        if (!selectFrom.isPopupVisible()) {
            // record selected account
            AccountItem selected = (AccountItem) selectFrom.getSelectedItem();

            selectFrom.removeAllItems();

            for (AccountItem accountItem : accountItems) {
                selectFrom.addItem(accountItem);
            }

            // recover selected account
            if (selected != null) {
                for (AccountItem item : accountItems) {
                    if (Arrays.equals(item.address, selected.address)) {
                        selectFrom.setSelectedItem(item);
                        break;
                    }
                }
            }
        }

        // 'to' contains all current accounts and address book, only update if user
        // isn't interacting with it, and wallet is unlocked
        if (!selectTo.isPopupVisible() && kernel.getWallet().isUnlocked()) {

            // add aliases to list of accounts
            for (Map.Entry<ByteArray, String> address : kernel.getWallet().getAddressAliases().entrySet()) {
                // only add aliases not in wallet
                if (kernel.getWallet().getAccount(address.getKey().getData()) == null) {
                    accountItems.add(new AccountItem(address.getValue(), address.getKey().getData()));
                }
            }

            Object toSelected = selectTo.getEditor().getItem();

            selectTo.removeAllItems();
            for (AccountItem accountItem : accountItems) {
                selectTo.addItem(accountItem);
            }
            selectTo.setSelectedItem(toSelected);
        }
    }

    /**
     * Sends transaction.
     */
    protected void send() {
        try {
            WalletAccount acc = getSelectedAccount();
            Amount value = getValue();
            Amount fee = getFee();
            String data = getData();

            // decode0x recipient address
            byte[] to = Hex.decode0x(getTo());
            byte[] rawData = rdbtnText.isSelected() ? Bytes.of(data) : Hex.decode0x(data);

            if (acc == null) {
                showErrorDialog(GuiMessages.get("SelectAccount"));
            } else if (value.isNotPositive()) {
                showErrorDialog(GuiMessages.get("EnterValidValue"));
            } else if (fee.lessThan(config.spec().minTransactionFee())) {
                showErrorDialog(GuiMessages.get("TransactionFeeTooLow"));
            } else if (value.add(fee).greaterThan(acc.getAvailable())) {
                showErrorDialog(GuiMessages.get("InsufficientFunds", SwingUtil.formatAmount(value.add(fee))));
            } else if (to.length != Key.ADDRESS_LEN) {
                showErrorDialog(GuiMessages.get("InvalidReceivingAddress"));
            } else if (rawData.length > config.spec().maxTransactionDataSize(TransactionType.TRANSFER)) {
                showErrorDialog(
                        GuiMessages.get("InvalidData", config.spec().maxTransactionDataSize(TransactionType.TRANSFER)));
            } else {
                byte[] code = kernel.getBlockchain().getAccountState().getCode(to);
                if (code != null && code.length > 0) {
                    int ret = JOptionPane.showConfirmDialog(this,
                            GuiMessages.get("SendToContract"),
                            GuiMessages.get("SendToContractWarning"),
                            JOptionPane.OK_CANCEL_OPTION);
                    if (ret != JOptionPane.OK_OPTION) {
                        return;
                    }
                }

                int ret = JOptionPane.showConfirmDialog(this,
                        GuiMessages.get("TransferInfo", SwingUtil.formatAmountFull(value), Hex.encode0x(to)),
                        GuiMessages.get("ConfirmTransfer"), JOptionPane.YES_NO_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    TransactionType type = TransactionType.TRANSFER;
                    PendingManager.ProcessingResult result = TransactionSender.send(kernel, acc, type, to, value, fee,
                            rawData);
                    handleTransactionResult(result);
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
        setTo(Bytes.EMPTY_BYTES);
        setValue(Amount.ZERO);
        setFee(config.spec().minTransactionFee());
        setData("");
    }

    /**
     * Shows the address book.
     */
    protected void showAddressBook() {
        gui.getAddressBookDialog().setVisible(true);
    }

    /**
     * Returns the selected account.
     * 
     * @return
     */
    protected WalletAccount getSelectedAccount() {
        AccountItem selected = (AccountItem) selectFrom.getSelectedItem();
        return selected == null ? null
                : model.getAccounts()
                        .stream()
                        .filter(walletAccount -> Arrays.equals(selected.address, walletAccount.getAddress()))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Handles pending transaction result.
     *
     * @param result
     */
    protected void handleTransactionResult(PendingManager.ProcessingResult result) {
        if (result.error == null) {
            JOptionPane.showMessageDialog(
                    this,
                    GuiMessages.get("TransactionSent", 30),
                    GuiMessages.get("SuccessDialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            clear();
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
    protected static class AccountItem implements Comparable<AccountItem> {
        final byte[] address;
        final String name;

        AccountItem(WalletAccount a) {
            Optional<String> alias = a.getName();

            this.address = a.getKey().toAddress();
            this.name = Hex.PREF + a.getKey().toAddressString() + ", " // address
                    + (alias.map(s -> s + ", ").orElse("")) // alias
                    + SwingUtil.formatAmount(a.getAvailable()); // available
        }

        AccountItem(String alias, byte[] address) {
            this.name = Hex.encode0x(address) + ", " + alias;
            this.address = address;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public int compareTo(AccountItem o) {
            return name.compareTo(o.name);
        }
    }
}