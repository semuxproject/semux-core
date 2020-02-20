/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.semux.core.Wallet;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.semux.gui.Action;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;

public class InitializeHdWalletDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final transient Wallet wallet;

    private final JTextArea phraseField;
    private final JTextField repeatPhraseField;
    private MnemonicGenerator generator = new MnemonicGenerator();
    private String phrase = generator.getWordlist(Wallet.MNEMONIC_ENTROPY_LENGTH, Wallet.MNEMONIC_LANGUAGE);

    public InitializeHdWalletDialog(Wallet wallet, JFrame parent) {
        super(parent, GuiMessages.get("InitializeHdWallet"));
        this.wallet = wallet;

        JLabel lblMnemonicPhraseTip = new JLabel(GuiMessages.get("MnemonicPhraseTip"));
        JLabel lblMnemonicPhrase = new JLabel(GuiMessages.get("MnemonicPhrase") + ":");
        JLabel lblMnemonicPhraseRepeat = new JLabel(GuiMessages.get("RepeatMnemonicPhrase") + ":");

        phraseField = new JTextArea();
        phraseField.setEditable(false);
        phraseField.setLineWrap(true);
        phraseField.setWrapStyleWord(true);
        phraseField.setRows(2);
        phraseField.setText(phrase);

        repeatPhraseField = new JTextField();

        JButton okButton = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(lblMnemonicPhraseRepeat)
                        .addComponent(lblMnemonicPhrase))
                    .addGap(15)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblMnemonicPhraseTip)
                        .addComponent(phraseField, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                        .addComponent(repeatPhraseField, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                        .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(30)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblMnemonicPhraseTip))
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblMnemonicPhrase)
                        .addComponent(phraseField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblMnemonicPhraseRepeat)
                        .addComponent(repeatPhraseField))
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(okButton))
                    .addContainerGap(30, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK: {
            String phrase = phraseField.getText();

            String repeat = repeatPhraseField.getText();
            repeat = String.join(" ", repeat.trim().split("\\s+"));

            if (!repeat.equals(phrase)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("HdWalletInitializationFailure"));
                break;
            }

            wallet.initializeHdWallet(phrase);
            wallet.flush();
            JOptionPane.showMessageDialog(this, GuiMessages.get("HdWalletInitializationSuccess"));

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
