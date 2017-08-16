package org.semux.gui.panel;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.semux.core.Unit;

public class SendPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> payFrom;
    private JTextField payTo;
    private JTextField payAmount;
    private JTextField payFee;

    public SendPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 100, 200, 0 };
        gridBagLayout.rowHeights = new int[] { 60, 60, 60, 60, 60, 0 };
        gridBagLayout.columnWeights = new double[] { 0.3, 0.7, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        JLabel lblFrom = new JLabel("Pay From:");
        lblFrom.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbcFrom1 = new GridBagConstraints();
        gbcFrom1.fill = GridBagConstraints.BOTH;
        gbcFrom1.insets = new Insets(0, 0, 5, 5);
        gbcFrom1.gridx = 0;
        gbcFrom1.gridy = 0;
        add(lblFrom, gbcFrom1);

        payFrom = new JComboBox<>();
        GridBagConstraints gbcFrom2 = new GridBagConstraints();
        gbcFrom2.fill = GridBagConstraints.BOTH;
        gbcFrom2.insets = new Insets(0, 0, 5, 0);
        gbcFrom2.gridx = 1;
        gbcFrom2.gridy = 0;
        add(payFrom, gbcFrom2);

        JLabel lblTo = new JLabel("Pay To:");
        lblTo.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbcTo1 = new GridBagConstraints();
        gbcTo1.fill = GridBagConstraints.BOTH;
        gbcTo1.insets = new Insets(0, 0, 5, 5);
        gbcTo1.gridx = 0;
        gbcTo1.gridy = 1;
        add(lblTo, gbcTo1);

        payTo = new JTextField();
        payTo.setColumns(10);
        GridBagConstraints gbcPay2 = new GridBagConstraints();
        gbcPay2.fill = GridBagConstraints.HORIZONTAL;
        gbcPay2.insets = new Insets(0, 0, 5, 0);
        gbcPay2.gridx = 1;
        gbcPay2.gridy = 1;
        add(payTo, gbcPay2);

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbcAmount1 = new GridBagConstraints();
        gbcAmount1.fill = GridBagConstraints.BOTH;
        gbcAmount1.insets = new Insets(0, 0, 5, 5);
        gbcAmount1.gridx = 0;
        gbcAmount1.gridy = 2;
        add(lblAmount, gbcAmount1);

        JPanel panelAmount = new JPanel();
        GridBagConstraints gbcAmount2 = new GridBagConstraints();
        gbcAmount2.fill = GridBagConstraints.HORIZONTAL;
        gbcAmount2.insets = new Insets(0, 0, 5, 0);
        gbcAmount2.gridx = 1;
        gbcAmount2.gridy = 2;
        add(panelAmount, gbcAmount2);
        panelAmount.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        payAmount = new JTextField();
        panelAmount.add(payAmount);
        payAmount.setColumns(10);

        JLabel lblSem = new JLabel("SEM");
        panelAmount.add(lblSem);

        JLabel lblFee = new JLabel("Fee:");
        lblFee.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbcFee1 = new GridBagConstraints();
        gbcFee1.fill = GridBagConstraints.BOTH;
        gbcFee1.insets = new Insets(0, 0, 5, 5);
        gbcFee1.gridx = 0;
        gbcFee1.gridy = 3;
        add(lblFee, gbcFee1);

        JPanel panelFee = new JPanel();
        GridBagConstraints gbcFee2 = new GridBagConstraints();
        gbcFee2.fill = GridBagConstraints.HORIZONTAL;
        gbcFee2.insets = new Insets(0, 0, 5, 0);
        gbcFee2.gridx = 1;
        gbcFee2.gridy = 3;
        add(panelFee, gbcFee2);
        panelFee.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        payFee = new JTextField();
        panelFee.add(payFee);
        payFee.setColumns(10);

        JLabel lblUsem = new JLabel("mSEM");
        panelFee.add(lblUsem);

        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(this);
        GridBagConstraints gbcSend = new GridBagConstraints();
        gbcSend.anchor = GridBagConstraints.WEST;
        gbcSend.gridx = 1;
        gbcSend.gridy = 4;
        add(btnSend, gbcSend);
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
        return (long) (Unit.MILLI_SEM * Double.parseDouble(payTo.getText().trim()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
