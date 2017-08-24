package org.semux.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.semux.core.Unit;
import org.semux.gui.Action;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> payFrom;
    private JTextField payTo;
    private JTextField payAmount;
    private JTextField payFee;

    public SendPanel() {
        setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel lblFrom = new JLabel("From:");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel lblTo = new JLabel("To:");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);

        payTo = new JTextField();
        payTo.setColumns(24);

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);

        payAmount = new JTextField();
        payAmount.setColumns(10);

        JLabel lblFee = new JLabel("Fee:");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);

        payFrom = new JComboBox<>();

        payFee = new JTextField();
        payFee.setColumns(10);

        JSeparator separator = new JSeparator();

        JLabel lblSem1 = new JLabel("SEM");

        JLabel lblSem2 = new JLabel("SEM");

        JButton paySend = new JButton("Send");
        paySend.addActionListener(this);
        paySend.setActionCommand(Action.SEND.name());

        JButton payClear = new JButton("Clear");
        payClear.addActionListener(this);
        payClear.setActionCommand(Action.CLEAR.name());

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(24)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(38)
                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(lblTo)
                                .addComponent(lblFrom)
                                .addComponent(lblAmount)
                                .addComponent(lblFee))
                            .addGap(18)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(payTo, GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                                .addComponent(payFrom, 0, 305, Short.MAX_VALUE)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                        .addComponent(payFee)
                                        .addComponent(payAmount, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))
                                    .addPreferredGap(ComponentPlacement.UNRELATED)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblSem1)
                                        .addComponent(lblSem2))))
                            .addGap(59))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(separator, GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                            .addGap(21))))
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(133)
                    .addComponent(paySend, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(payClear, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(192, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(10)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFrom)
                        .addComponent(payFrom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(payTo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblAmount)
                        .addComponent(payAmount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem1))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(payFee, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSem2))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(payClear)
                        .addComponent(paySend))
                    .addContainerGap(139, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
        // @formatter:on
    }

    public String getFrom() {
        return (String) payFrom.getSelectedItem();
    }

    public void setFromItems(String[] items) {
        payFrom.removeAllItems();
        for (String item : items) {
            payFrom.addItem(item);
        }
    }

    public String getTo() {
        return payTo.getText().trim();
    }

    public long getAmount() {
        return (long) (Unit.SEM * Double.parseDouble(payTo.getText().trim()));
    }

    public long getFee() {
        return (long) (Unit.SEM * Double.parseDouble(payTo.getText().trim()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
