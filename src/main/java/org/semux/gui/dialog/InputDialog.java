package org.semux.gui.dialog;

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
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.gui.Action;
import org.semux.gui.SwingUtil;
import org.semux.utils.UnreachableException;

public class InputDialog extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextField textField;
    private String text;

    private AtomicBoolean done = new AtomicBoolean(false);

    public InputDialog(String message, boolean isPassword) {
        JLabel labelLogo = new JLabel("");
        labelLogo.setIcon(SwingUtil.loadImage("logo", 96, 96));

        JLabel lblMessage = new JLabel(message);
        textField = isPassword ? new JPasswordField() : new JTextField();
        textField.setActionCommand(Action.OK.name());
        textField.addActionListener(this);

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
                            .addComponent(textField, GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))))
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
                            .addComponent(textField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addGap(18)
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

    public String getInput() {
        this.setVisible(true);

        synchronized (done) {
            while (!done.get()) {
                try {
                    done.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }

            return text;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            text = new String(textField.getText());
            notifyDone();
            break;
        case CANCEL:
            text = null;
            notifyDone();
            break;
        default:
            throw new UnreachableException();
        }

        this.dispose();
    }

    private void notifyDone() {
        synchronized (done) {
            done.set(true);
            done.notifyAll();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        InputDialog pwd = new InputDialog("Please enter your password", true);
        String password = pwd.getInput();
        System.out.println("Password: " + password);
    }
}
