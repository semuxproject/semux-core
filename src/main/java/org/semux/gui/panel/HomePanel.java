/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.semux.core.SyncManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.gui.Action;
import org.semux.gui.SemuxGUI;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletAccount;
import org.semux.gui.model.WalletModel;
import org.semux.message.GUIMessages;
import org.semux.util.ByteArray;
import org.semux.util.exception.UnreachableException;

public class HomePanel extends JPanel implements ActionListener {

    private static final int NUMBER_OF_TRANSACTIONS = 6;

    private static final long serialVersionUID = 1L;

    private static final EnumSet<TransactionType> FEDERATED_TRANSACTION_TYPES = EnumSet.of(TransactionType.COINBASE,
            TransactionType.TRANSFER);

    private transient SemuxGUI gui;
    private transient WalletModel model;

    private JLabel syncProgress;
    private JLabel blockNum;
    private JLabel blockTime;
    private JLabel coinbase;
    private JLabel status;
    private JLabel available;
    private JLabel locked;
    private JLabel total;

    private JPanel transactions;
    private JLabel peers;

    public HomePanel(SemuxGUI gui) {
        this.gui = gui;
        this.model = gui.getModel();
        this.model.addListener(this);

        // setup overview panel
        JPanel overview = new JPanel();
        overview.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 10, 10, 10)),
                GUIMessages.get("Overview"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        overview.setLayout(new GridLayout(7, 2, 0, 0));

        JLabel labelBlockNum = new JLabel(GUIMessages.get("BlockNum") + ":");
        overview.add(labelBlockNum);

        blockNum = new JLabel("");
        overview.add(blockNum);

        JLabel lblBlockTime = new JLabel(GUIMessages.get("BlockTime") + ":");
        overview.add(lblBlockTime);

        blockTime = new JLabel("");
        overview.add(blockTime);

        JLabel labelCoinbase = new JLabel(GUIMessages.get("Coinbase") + ":");
        overview.add(labelCoinbase);

        coinbase = new JLabel("");
        overview.add(coinbase);

        JLabel labelStatus = new JLabel(GUIMessages.get("Status") + ":");
        overview.add(labelStatus);

        status = new JLabel("");
        overview.add(status);

        JLabel labelAvailable = new JLabel(GUIMessages.get("Available") + ":");
        overview.add(labelAvailable);

        available = new JLabel("");
        overview.add(available);

        JLabel labelLocked = new JLabel(GUIMessages.get("Locked") + ":");
        overview.add(labelLocked);

        locked = new JLabel("");
        overview.add(locked);

        JLabel labelTotal = new JLabel(GUIMessages.get("TotalBalance") + ":");
        overview.add(labelTotal);

        total = new JLabel("");
        overview.add(total);

        JLabel lblPeers = new JLabel(GUIMessages.get("Peers") + ":");
        peers = new JLabel("");

        JLabel syncProgressLabel = new JLabel(GUIMessages.get("SyncProgress") + ":");
        overview.add(syncProgressLabel);
        syncProgress = new JLabel("");
        syncProgress.setName("syncProgress");

        // setup transactions panel
        transactions = new JPanel();
        transactions.setBorder(new TitledBorder(
                new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 10, 10, 10)),
                GUIMessages.get("Transactions"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(lblPeers)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(peers)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(syncProgressLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(syncProgress))
                        .addComponent(overview, GroupLayout.PREFERRED_SIZE, 324, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(transactions, GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(overview, GroupLayout.PREFERRED_SIZE, 199, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED, 353, Short.MAX_VALUE)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPeers)
                                .addComponent(peers)
                                .addComponent(syncProgressLabel)
                                .addComponent(syncProgress))

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
            JLabel lblAmount = new JLabel(prefix + SwingUtil.formatValue(tx.getValue()));
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
                                .addComponent(labelAddress, GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
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

    private void refresh() {
        Block block = model.getLatestBlock();
        this.syncProgress.setText(SyncProgressFormatter.format(model.getSyncProgress()));
        this.blockNum.setText(SwingUtil.formatNumber(block.getNumber()));
        this.blockTime.setText(SwingUtil.formatTimestamp(block.getTimestamp()));
        this.coinbase.setText(GUIMessages.get("AccountNum", model.getCoinbase()));
        this.status.setText(model.isDelegate() ? GUIMessages.get("Delegate") : GUIMessages.get("Normal"));
        this.available.setText(SwingUtil.formatValue(model.getTotalAvailable()));
        this.locked.setText(SwingUtil.formatValue(model.getTotalLocked()));
        this.peers.setText(SwingUtil.formatNumber(model.getActivePeers().size()));
        this.total.setText(SwingUtil.formatValue(model.getTotalAvailable() + model.getTotalLocked()));

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

    public static class SyncProgressFormatter {

        private SyncProgressFormatter() {
        }

        public static String format(SyncManager.Progress progress) {
            if (progress != null && progress.getTargetHeight() > 0) {
                return SwingUtil.formatPercentage(
                        (double) progress.getCurrentHeight() / (double) progress.getTargetHeight() * 100d);
            } else {
                return "-";
            }
        }
    }
}
