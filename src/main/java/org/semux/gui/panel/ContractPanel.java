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
import java.math.BigDecimal;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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

public class ContractPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final transient SemuxGui gui;
    private final transient WalletModel model;
    private final transient Kernel kernel;
    private final transient Config config;

    private final JLabel lblTo;
    private final JButton btnAddressBook;

    private final JRadioButton rdbtnCreate;
    private final JRadioButton rdbtnCall;
    private final JComboBox<AccountItem> selectFrom;
    private final JComboBox<AccountItem> selectTo;
    private final JTextField txtValue;
    private final JTextField txtGas;
    private final JTextField txtGasPrice;
    private final JTextArea txtData;

    public ContractPanel(SemuxGui gui, JFrame frame) {
        this.gui = gui;
        this.model = gui.getModel();
        this.model.addListener(this);

        this.kernel = gui.getKernel();
        this.config = kernel.getConfig();

        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblType = new JLabel(GuiMessages.get("Type") + ":");
        lblType.setHorizontalAlignment(SwingConstants.RIGHT);

        rdbtnCreate = new JRadioButton(GuiMessages.get("DeployContract"));
        rdbtnCall = new JRadioButton(GuiMessages.get("CallContract"));
        ButtonGroup btnGroupDataType = new ButtonGroup();
        btnGroupDataType.add(rdbtnCreate);
        btnGroupDataType.add(rdbtnCall);

        JLabel lblFrom = new JLabel(GuiMessages.get("From") + ":");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        selectFrom = new JComboBox<>();
        selectFrom.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        lblTo = new JLabel(GuiMessages.get("To") + ":");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        selectTo = SwingUtil.comboBoxWithCopyPastePopup();
        selectTo.setName("selectTo");
        selectTo.setEditable(true);

        btnAddressBook = new JButton(GuiMessages.get("AddressBook"));
        btnAddressBook.setName("btnAddressBook");
        btnAddressBook.addActionListener(this);
        btnAddressBook.setActionCommand(Action.SHOW_ADDRESS_BOOK.name());

        JLabel lblValue = new JLabel(GuiMessages.get("Value") + ":");
        lblValue.setHorizontalAlignment(SwingConstants.RIGHT);

        txtValue = SwingUtil.textFieldWithCopyPastePopup();
        txtValue.setName("txtValue");
        txtValue.setColumns(10);
        txtValue.setActionCommand(Action.SEND.name());
        txtValue.addActionListener(this);

        JLabel lblGas = new JLabel(GuiMessages.get("Gas") + ":");
        lblGas.setHorizontalAlignment(SwingConstants.RIGHT);

        txtGas = SwingUtil.textFieldWithCopyPastePopup();
        txtGas.setName("txtGas");
        txtGas.setColumns(10);
        txtGas.setActionCommand(Action.SEND.name());
        txtGas.addActionListener(this);

        JLabel lblGasPrice = new JLabel(GuiMessages.get("GasPrice") + ":");
        lblGasPrice.setHorizontalAlignment(SwingConstants.RIGHT);

        txtGasPrice = SwingUtil.textFieldWithCopyPastePopup();
        txtGasPrice.setName("txtGasPrice");
        txtGasPrice.setColumns(10);
        txtGasPrice.setActionCommand(Action.SEND.name());
        txtGasPrice.addActionListener(this);

        JLabel lblData = new JLabel(GuiMessages.get("DataHex") + ":");
        lblData.setHorizontalAlignment(SwingConstants.RIGHT);

        txtData = SwingUtil.textAreaWithCopyPastePopup(Hex.PREF);
        txtData.setName("txtData");
        JScrollPane dataPane = new JScrollPane(txtData);

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

        rdbtnCreate.addActionListener(e -> toggleToAddress(false));
        rdbtnCall.addActionListener(e -> toggleToAddress(true));
        rdbtnCall.setSelected(true);
        toggleToAddress(true);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblType)
                        .addComponent(lblFrom)
                        .addComponent(lblTo)
                        .addComponent(lblValue)
                        .addComponent(lblGas)
                        .addComponent(lblGasPrice)
                        .addComponent(lblData))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(btnClear)
                            .addGap(18)
                            .addComponent(btnSend))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(rdbtnCall)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(rdbtnCreate))
                                .addComponent(selectFrom)
                                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                                    .addComponent(selectTo, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                    .addGap(18)
                                    .addComponent(btnAddressBook))
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(txtValue, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                        .addComponent(txtGas)
                                        .addComponent(txtGasPrice))
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addGap(10)
                                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                .addComponent(lblUnit1)
                                                .addComponent(lblUnit2)))))
                                .addComponent(dataPane, 0, 400, Short.MAX_VALUE))
                        .addGap(30))))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(lblType)
                            .addComponent(rdbtnCreate)
                            .addComponent(rdbtnCall))
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
                            .addComponent(lblGas)
                            .addComponent(txtGas, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(lblGasPrice)
                            .addComponent(txtGasPrice, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblUnit2))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(dataPane, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE))
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

    public long getGas() throws ParseException {
        BigDecimal gas = SwingUtil.parseNumber(txtGas.getText().trim());
        return gas.longValue();
    }

    public void setGas(long gas) {
        txtGas.setText(SwingUtil.formatNumber(gas));
    }

    public Amount getGasPrice() throws ParseException {
        return SwingUtil.parseAmount(txtGasPrice.getText().trim());
    }

    public void setGasPrice(Amount a) {
        txtGasPrice.setText(SwingUtil.formatAmountNoUnit(a));
    }

    public String getDataText() {
        return txtData.getText().trim();
    }

    public void setDataText(String dataText) {
        txtData.setText(dataText.trim());
    }

    private void toggleToAddress(boolean visible) {
        lblTo.setVisible(visible);
        selectTo.setVisible(visible);
        btnAddressBook.setVisible(visible);
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
            long gas = getGas();
            Amount gasPrice = getGasPrice();
            String data = getDataText();

            byte[] to = Hex.decode0x(getTo());
            byte[] rawData = Hex.decode0x(data);
            boolean isCall = rdbtnCall.isSelected();
            TransactionType type = isCall ? TransactionType.CALL : TransactionType.CREATE;
            to = isCall ? to : Bytes.EMPTY_ADDRESS;

            if (acc == null) {
                showErrorDialog(GuiMessages.get("SelectAccount"));
            } else if (value.isNegative()) {
                showErrorDialog(GuiMessages.get("EnterValidValue"));
            } else if (gas < 21_000) {
                showErrorDialog(GuiMessages.get("EnterValidGas"));
            } else if (gasPrice.lessThan(Amount.ONE)) {
                showErrorDialog(GuiMessages.get("EnterValidGasPrice"));
            } else if (to.length != Key.ADDRESS_LEN) {
                showErrorDialog(GuiMessages.get("InvalidReceivingAddress"));
            } else if (rawData.length > config.spec().maxTransactionDataSize(type)) {
                showErrorDialog(
                        GuiMessages.get("InvalidData", config.spec().maxTransactionDataSize(TransactionType.TRANSFER)));
            } else {
                int ret = JOptionPane.showConfirmDialog(this,
                        isCall ? GuiMessages.get("CallInfo", Hex.encode0x(to)) : GuiMessages.get("CreateInfo"),
                        isCall ? GuiMessages.get("ConfirmCall") : GuiMessages.get("ConfirmCreate"),
                        JOptionPane.YES_NO_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    PendingManager.ProcessingResult result = TransactionSender.send(kernel, acc, type, to, value,
                            Amount.ZERO, rawData, gas, gasPrice);
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
        setGas(21_000L);
        setGasPrice(config.poolMinGasPrice());
        setDataText("");
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