/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;

import org.semux.core.Wallet;
import org.semux.crypto.Key;
import org.semux.crypto.bip39.Language;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.semux.gui.Action;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoverHdWalletDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final transient SemuxGui gui;
    private static final Logger logger = LoggerFactory.getLogger(RecoverHdWalletDialog.class);

    private final JTextArea phraseField;
    private final JPasswordField passwordField;

    public RecoverHdWalletDialog(SemuxGui gui, JFrame parent) {
        super(parent, GuiMessages.get("RecoverHdWallet"));
        this.gui = gui;

        JLabel lblPassword = new JLabel(GuiMessages.get("Password") + ":");
        JLabel lblPhrase = new JLabel(GuiMessages.get("WalletRecoveryPhrase") + ":");

        phraseField = new JTextArea();
        phraseField.setLineWrap(true);
        phraseField.setRows(2);
        phraseField.setWrapStyleWord(true);

        passwordField = new JPasswordField();

        JButton btnOk = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

        JButton btnCancel = SwingUtil.createDefaultButton(GuiMessages.get("Cancel"), this, Action.CANCEL);
        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGap(20)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(lblPassword)
                                        .addComponent(lblPhrase))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(btnCancel)
                                                .addGap(18)
                                                .addComponent(btnOk))
                                        .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addComponent(passwordField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                                                .addComponent(phraseField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)))
                                .addGap(23))
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGap(32)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblPhrase)
                                        .addComponent(phraseField, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblPassword)
                                        .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnCancel)
                                        .addComponent(btnOk))
                                .addContainerGap(77, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(400, 240));
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK: {
            String phrase = phraseField.getText();
            String password = new String(passwordField.getPassword());

            MnemonicGenerator generator = new MnemonicGenerator();
            byte[] seed;
            try {
                seed = generator.getSeedFromWordlist(phrase, password, Language.ENGLISH);
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidHdPhrase"));
                break;
            }

            Wallet wallet = gui.getKernel().getWallet();
            int found = 0;
            try {
                Wallet importedWallet = new Wallet(File.createTempFile("wallet", ".data"),
                        wallet.getNetwork());
                importedWallet.setHdSeed(seed);
                found = importedWallet.scanForHdKeys(gui.getKernel().getBlockchain().getAccountState());

                if (found > 0) {
                    wallet.addAccounts(importedWallet.getAccounts());

                    for (Key key : importedWallet.getAccounts()) {
                        wallet.addAccount(key);
                        if (!wallet.getAddressAlias(key.toAddress()).isPresent()) {
                            wallet.setAddressAlias(key.toAddress(),
                                    GuiMessages.get("Imported") + importedWallet.getAddressAlias(key.toAddress()));
                        }
                    }
                    wallet.flush();
                }

            } catch (IOException e1) {
                logger.error(e1.getMessage(), e);
            }

            JOptionPane.showMessageDialog(this, GuiMessages.get("ImportSuccess", found));
            this.dispose();
            break;
        }
        case CANCEL: {
            this.dispose();
            break;
        }
        default:
            break;
        }
    }
}
