package org.semux.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.utils.UnreachableException;

public class PasswordFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JPasswordField textPassword;
    private String password;
    private AtomicBoolean done = new AtomicBoolean(false);

    public PasswordFrame() {
        this(null);
    }

    public PasswordFrame(String message) {
        JLabel labelLogo = new JLabel("");
        labelLogo.setIcon(SwingUtil.loadImage("send", 96, 96));

        JLabel lblMessage = new JLabel(message == null ? "Please enter your password:" : message);
        textPassword = new JPasswordField();
        textPassword.setActionCommand(Action.OK.name());
        textPassword.addActionListener(this);

        JButton btnOk = new JButton("OK");
        btnOk.setSelected(true);
        btnOk.setActionCommand(Action.OK.name());
        btnOk.addActionListener(this);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setActionCommand(Action.CANCEL.name());
        btnCancel.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(labelLogo)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblMessage)
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                            .addComponent(textPassword, GroupLayout.PREFERRED_SIZE, 313, GroupLayout.PREFERRED_SIZE)))
                    .addGap(17))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(labelLogo))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(21)
                            .addComponent(lblMessage)
                            .addGap(18)
                            .addComponent(textPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(26)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(btnOk)
                                .addComponent(btnCancel))))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.pack();
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                notifyDone();
            }
        });
        SwingUtil.centerizeFrame(this, this.getWidth(), this.getHeight());
    }

    public String getPassword() {
        synchronized (done) {
            while (!done.get()) {
                try {
                    done.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }

            return password;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            password = new String(textPassword.getPassword());
            notifyDone();
            break;
        case CANCEL:
            password = null;
            notifyDone();
            break;
        default:
            throw new UnreachableException();
        }

        this.dispose();
    }

    private void notifyDone() {
        synchronized (done) {
            done.notifyAll();
            done.set(true);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PasswordFrame pwd = new PasswordFrame();
        pwd.setVisible(true);
        String password = pwd.getPassword();
        System.out.println("Password: " + password);
    }
}
