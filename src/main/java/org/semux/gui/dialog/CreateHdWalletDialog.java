/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import com.github.orogvany.bip39.Language;
import com.github.orogvany.bip39.MnemonicGenerator;
import org.semux.core.Wallet;
import org.semux.gui.Action;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateHdWalletDialog extends JDialog implements ActionListener {
    private final transient Wallet wallet;

    private final JTextArea phraseField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private MnemonicGenerator generator = new MnemonicGenerator();
    String phrase;

    public CreateHdWalletDialog(Wallet wallet, JFrame parent) {
        super(parent, GuiMessages.get("CreateHdWallet"));
        this.wallet = wallet;

        JLabel lblPassword = new JLabel(GuiMessages.get("Password") + ":");
        JLabel lblConfirmPassword = new JLabel(GuiMessages.get("RepeatPassword") + ":");
        JLabel lblPhrase = new JLabel(GuiMessages.get("WalletRecoveryPhrase") + ":");

        phraseField = new JTextArea();
        phraseField.setEditable(false);
        phraseField.setLineWrap(true);
        phraseField.setWrapStyleWord(true);
        phraseField.setRows(2);
        phrase = generator.getWordlist(128, Language.english);

        phraseField.setText(phrase);
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        JButton btnOk = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGap(20)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(lblConfirmPassword)
                                        .addComponent(lblPassword)
                                        .addComponent(lblPhrase))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addGap(18)
                                                .addComponent(btnOk))
                                        .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addComponent(confirmPasswordField, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
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
                                        .addComponent(lblConfirmPassword)
                                        .addComponent(confirmPasswordField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addGap(18)
                                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnOk))
                                .addContainerGap(77, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.setMinimumSize(new Dimension(400, 260));
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK: {
            String phrase = phraseField.getText();
            String password = new String(passwordField.getPassword());
            String passwordConfirm = new String(confirmPasswordField.getPassword());
            if(!password.equals(passwordConfirm)) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("RepeatPasswordError"));
                break;
            }

            byte[] seed;
            try {
                seed = generator.getSeedFromWordlist(phrase, password, Language.english);
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("InvalidHdPhrase"));
                break;
            }

            wallet.setHdSeed(seed);
            wallet.scanForHdKeys(null);
            wallet.flush();
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
