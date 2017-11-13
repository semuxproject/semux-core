/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.Kernel;
import org.semux.core.Block;
import org.semux.crypto.Hex;
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletDelegate;

public class DelegateDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public DelegateDialog(JComponent parent, WalletDelegate d) {
        Block block = Kernel.getInstance().getBlockchain().getBlock(d.getRegisteredAt());

        JLabel lblName = new JLabel(MessagesUtil.get("Name") + ":");
        JLabel lblAddress = new JLabel(MessagesUtil.get("Address") + ":");
        JLabel lblRegisteredAt = new JLabel(MessagesUtil.get("RegisteredAt") + ":");
        JLabel lblVotes = new JLabel(MessagesUtil.get("Votes") + ":");
        JLabel lblVotesFromMe = new JLabel(MessagesUtil.get("VotesFromMe") + ":");
        JLabel lblNumOfBlocksForged = new JLabel(MessagesUtil.get("NumBlocksForged") + ":");
        JLabel lblNumOfTurnsHit = new JLabel(MessagesUtil.get("NumTurnsHit") + ":");
        JLabel lblNumOfTurnsMissed = new JLabel(MessagesUtil.get("NumTurnsMissed") + ":");
        JLabel lblRate = new JLabel(MessagesUtil.get("Rate") + ":");

        JTextArea name = SwingUtil.selectableTextArea(d.getNameString());
        JTextArea address = SwingUtil.selectableTextArea(Hex.PREF + Hex.encode(d.getAddress()));
        JLabel registeredAt = new JLabel(SwingUtil.formatTimestamp(block.getTimestamp()));
        JLabel votes = new JLabel(SwingUtil.formatVote(d.getVotes()));
        JLabel votesFromMe = new JLabel(SwingUtil.formatVote(d.getVotesFromMe()));
        JLabel numOfBlocksForged = new JLabel(SwingUtil.formatNumber(d.getNumberOfBlocksForged()));
        JLabel numOfTurnsHit = new JLabel(SwingUtil.formatNumber(d.getNumberOfTurnsHit()));
        JLabel numOfTurnsMissed = new JLabel(SwingUtil.formatNumber(d.getNumberOfTurnsMissed()));
        JLabel rate = new JLabel(SwingUtil.formatPercentage(d.getRate()));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblRate)
                        .addComponent(lblNumOfTurnsMissed)
                        .addComponent(lblNumOfTurnsHit)
                        .addComponent(lblNumOfBlocksForged)
                        .addComponent(lblVotesFromMe)
                        .addComponent(lblVotes)
                        .addComponent(lblRegisteredAt)
                        .addComponent(lblAddress)
                        .addComponent(lblName))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(name)
                        .addComponent(address)
                        .addComponent(votes)
                        .addComponent(votesFromMe)
                        .addComponent(registeredAt)
                        .addComponent(numOfBlocksForged)
                        .addComponent(numOfTurnsHit)
                        .addComponent(numOfTurnsMissed)
                        .addComponent(rate))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblName)
                        .addComponent(name, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(address, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRegisteredAt)
                        .addComponent(registeredAt))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblVotes)
                        .addComponent(votes))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblVotesFromMe)
                        .addComponent(votesFromMe))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNumOfBlocksForged)
                        .addComponent(numOfBlocksForged))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNumOfTurnsHit)
                        .addComponent(numOfTurnsHit))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNumOfTurnsMissed)
                        .addComponent(numOfTurnsMissed))
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblRate)
                        .addComponent(rate))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setTitle(MessagesUtil.get("Delegate"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
