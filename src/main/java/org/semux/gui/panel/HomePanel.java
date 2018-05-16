/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import static org.semux.core.Amount.sum;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
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

import org.semux.core.Block;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.HorizontalSeparator;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.gui.model.WalletModel.Status;
import org.semux.message.GuiMessages;
import org.semux.util.ByteArray;
import org.semux.util.TimeUtil;
import org.semux.util.exception.UnreachableException;

public class HomePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final int NUMBER_OF_TRANSACTIONS = 5;

    private static final EnumSet<TransactionType> FEDERATED_TRANSACTION_TYPES = EnumSet.of(TransactionType.COINBASE,
            TransactionType.TRANSFER);

    private final transient SemuxGui gui;
    private final transient WalletModel model;

    // Overview Table
    private final JLabel bestBlockNum;
    private final JLabel blockNum;
    private final JLabel blockTime;
    private final JLabel coinbase;
    private final JLabel status;
    private final JLabel available;
    private final JLabel locked;
    private final JLabel total;

    // Consensus Table
    private final JLabel primaryValidator;
    private final JLabel nextPrimaryValidator;
    private final JLabel nextValidatorSetUpdate;

    // Transactions Table
    private final JPanel transactions;

    public HomePanel(SemuxGui gui) {
        this.gui = gui;
        this.model = gui.getModel();
        this.model.addListener(this);

        Font plainFont = getFont().deriveFont(getFont().getStyle() | Font.PLAIN);
        Font boldFont = getFont().deriveFont(getFont().getStyle() | Font.BOLD);

        // setup overview panel
        JPanel overview = new JPanel();
        overview.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 10, 10, 10)),
                GuiMessages.get("Overview"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        overview.setLayout(new GridLayout(8, 2, 0, 0));

        JLabel labelBestBlockNum = new JLabel(GuiMessages.get("BestBlockNum") + ":");
        labelBestBlockNum.setFont(boldFont);
        overview.add(labelBestBlockNum);

        bestBlockNum = new JLabel("");
        bestBlockNum.setFont(plainFont);
        overview.add(bestBlockNum);

        JLabel labelBlockNum = new JLabel(GuiMessages.get("BlockNum") + ":");
        labelBlockNum.setFont(boldFont);
        overview.add(labelBlockNum);

        blockNum = new JLabel("");
        blockNum.setFont(plainFont);
        overview.add(blockNum);

        JLabel lblBlockTime = new JLabel(GuiMessages.get("BlockTime") + ":");
        lblBlockTime.setFont(boldFont);
        overview.add(lblBlockTime);

        blockTime = new JLabel("");
        blockTime.setFont(plainFont);
        overview.add(blockTime);

        JLabel labelCoinbase = new JLabel(GuiMessages.get("Coinbase") + ":");
        labelCoinbase.setFont(boldFont);
        overview.add(labelCoinbase);

        coinbase = new JLabel("");
        coinbase.setFont(plainFont);
        overview.add(coinbase);

        JLabel labelStatus = new JLabel(GuiMessages.get("Status") + ":");
        labelStatus.setFont(boldFont);
        overview.add(labelStatus);

        status = new JLabel("");
        status.setFont(plainFont);
        overview.add(status);

        JLabel labelAvailable = new JLabel(GuiMessages.get("Available") + ":");
        labelAvailable.setFont(boldFont);
        overview.add(labelAvailable);

        available = new JLabel("");
        available.setFont(plainFont);
        overview.add(available);

        JLabel labelLocked = new JLabel(GuiMessages.get("Locked") + ":");
        labelLocked.setFont(boldFont);
        overview.add(labelLocked);

        locked = new JLabel("");
        locked.setFont(plainFont);
        overview.add(locked);

        JLabel labelTotal = new JLabel(GuiMessages.get("TotalBalance") + ":");
        labelTotal.setFont(boldFont);
        overview.add(labelTotal);

        total = new JLabel("");
        total.setFont(plainFont);
        overview.add(total);

        // setup consensus panel
        JPanel consensus = new JPanel();
        consensus.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 10, 0, 10)),
                GuiMessages.get("Consensus"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        consensus.setLayout(new GridBagLayout());
        consensus.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;

        JLabel labelPrimaryValidator = new JLabel(GuiMessages.get("PrimaryValidator") + ":");
        labelPrimaryValidator.setFont(boldFont);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(labelPrimaryValidator, c);

        primaryValidator = new JLabel("");
        primaryValidator.setFont(plainFont);
        primaryValidator.setHorizontalAlignment(SwingConstants.LEFT);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(primaryValidator, c);

        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        consensus.add(new HorizontalSeparator(), c);

        JLabel labelNextPrimaryValidator = new JLabel(GuiMessages.get("NextPrimaryValidator") + ":");
        labelNextPrimaryValidator.setFont(boldFont);
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(labelNextPrimaryValidator, c);

        nextPrimaryValidator = new JLabel("");
        nextPrimaryValidator.setFont(plainFont);
        nextPrimaryValidator.setHorizontalAlignment(SwingConstants.LEFT);
        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(nextPrimaryValidator, c);

        c.gridx = 0;
        c.gridy = 5;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        consensus.add(new HorizontalSeparator(), c);

        JLabel labelNextValidatorSetUpdate = new JLabel(GuiMessages.get("NextValidatorSetUpdate") + ":");
        labelNextValidatorSetUpdate.setFont(boldFont);
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(labelNextValidatorSetUpdate, c);

        nextValidatorSetUpdate = new JLabel("");
        nextValidatorSetUpdate.setFont(plainFont);
        nextValidatorSetUpdate.setHorizontalAlignment(SwingConstants.LEFT);
        c.gridx = 0;
        c.gridy = 7;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        consensus.add(nextValidatorSetUpdate, c);

        // empty rows
        c.gridx = 0;
        c.gridy = 8;
        consensus.add(Box.createRigidArea(new Dimension(0, 0)), c);

        // setup transactions panel
        transactions = new JPanel();
        transactions.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 0, 10, 10)),
                GuiMessages.get("Transactions"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(overview, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                        .addComponent(consensus, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE))
                    .addGap(10)
                    .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(overview, GroupLayout.PREFERRED_SIZE, 210, GroupLayout.PREFERRED_SIZE)
                            .addGap(10)
                            .addComponent(consensus, GroupLayout.PREFERRED_SIZE, 210, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED, 353, Short.MAX_VALUE)
                            .addPreferredGap(ComponentPlacement.RELATED)))
                    .addGap(0))
        );
        transactions.setLayout(new BoxLayout(transactions, BoxLayout.Y_AXIS));
        setLayout(groupLayout);
        // @formatter:on

        refresh();
    }

    public static class TransactionPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public TransactionPanel(Transaction tx, boolean inBound, boolean outBound, String description) {
            this.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel lblType = new JLabel("");
            String bounding = inBound ? "inbound" : "outbound";
            String name = (inBound && outBound) ? "cycle" : (bounding);
            lblType.setIcon(SwingUtil.loadImage(name, 42, 42));
            String mathSign = inBound ? "+" : "-";
            String prefix = (inBound && outBound) ? "" : (mathSign);
            JLabel lblAmount = new JLabel(prefix + SwingUtil.formatAmount(tx.getValue()));
            lblAmount.setToolTipText(SwingUtil.formatAmount(tx.getValue()));
            lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

            JLabel lblTime = new JLabel(SwingUtil.formatTimestamp(tx.getTimestamp()));

            JLabel labelAddress = new JLabel(description);
            labelAddress.setForeground(Color.GRAY);

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
                                .addComponent(labelAddress, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
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
                                .addComponent(labelAddress))
                            .addComponent(lblType, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
            );
            this.setLayout(groupLayout);
            // @formatter:on
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        default:
            throw new UnreachableException();
        }
    }

    /**
     * Refreshes this panel.
     */
    protected void refresh() {
        Block block = model.getLatestBlock();
        this.bestBlockNum.setText(model.getSyncProgress().getTargetHeight() > 0
                ? SwingUtil.formatNumber(model.getSyncProgress().getTargetHeight() - 1)
                : "-");
        this.blockNum.setText(SwingUtil.formatNumber(block.getNumber()));
        this.blockTime.setText(SwingUtil.formatTimestamp(block.getTimestamp()));
        this.coinbase.setText(SwingUtil.getAddressAbbr(model.getCoinbase().toAddress()));
        this.coinbase.setToolTipText(Hex.PREF + model.getCoinbase().toAddressString());
        this.status.setText(model.getStatus() == Status.VALIDATOR ? GuiMessages.get("Validator")
                : (model.getStatus() == Status.DELEGATE ? GuiMessages.get("Delegate") : GuiMessages.get("Normal")));
        this.available.setText(SwingUtil.formatAmount(model.getTotalAvailable()));
        this.available.setToolTipText(SwingUtil.formatAmount(model.getTotalAvailable()));
        this.locked.setText(SwingUtil.formatAmount(model.getTotalLocked()));
        this.locked.setToolTipText(SwingUtil.formatAmount(model.getTotalLocked()));
        this.total.setText(SwingUtil.formatAmount(sum(model.getTotalAvailable(), model.getTotalLocked())));
        this.total.setToolTipText(SwingUtil.formatAmount(sum(model.getTotalAvailable(), model.getTotalLocked())));

        this.primaryValidator.setText(model.getPrimaryValidator().map(Delegate::getNameString).orElse("-"));
        this.nextPrimaryValidator.setText(model.getNextPrimaryValidator().map(Delegate::getNameString).orElse("-"));
        this.nextValidatorSetUpdate.setText(model.getNextValidatorSetUpdate()
                .map(n -> GuiMessages.get("NextValidatorSetUpdateTime", n,
                        TimeUtil.formatTimestamp(block.getTimestamp() + (n - block.getNumber()) * 30 * 1000)))
                .orElse("-"));

        // federate all transactions
        Set<ByteArray> hashes = new HashSet<>();
        List<Transaction> list = new ArrayList<>();
        for (WalletAccount acc : model.getAccounts()) {
            for (Transaction tx : acc.getTransactions()) {
                ByteArray key = ByteArray.of(tx.getHash());
                if (FEDERATED_TRANSACTION_TYPES.contains(tx.getType()) && !hashes.contains(key)) {
                    list.add(tx);
                    hashes.add(key);
                }
            }
        }
        list.sort((tx1, tx2) -> Long.compare(tx2.getTimestamp(), tx1.getTimestamp()));
        list = list.size() > NUMBER_OF_TRANSACTIONS ? list.subList(0, NUMBER_OF_TRANSACTIONS) : list;

        Set<ByteArray> accounts = new HashSet<>();
        for (WalletAccount a : model.getAccounts()) {
            accounts.add(ByteArray.of(a.getKey().toAddress()));
        }
        transactions.removeAll();
        for (Transaction tx : list) {
            boolean inBound = accounts.contains(ByteArray.of(tx.getTo()));
            boolean outBound = accounts.contains(ByteArray.of(tx.getFrom()));
            transactions.add(new TransactionPanel(tx, inBound, outBound, SwingUtil.getTransactionDescription(gui, tx)));
        }
        transactions.revalidate();
    }
}
