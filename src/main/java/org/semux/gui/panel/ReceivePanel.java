package org.semux.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.LineBorder;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.semux.gui.Action;
import org.semux.gui.SwingUtil;

public class ReceivePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private String[] columnNames = { "#", "Address", "Balance" };
    private Object[][] data = { { new Integer(0), "0x1122334455667788112233445566778811223344", new Integer(12) } };

    public ReceivePanel() {
        JScrollPane scrollPane = new JScrollPane();
        table = new JTable(data, columnNames);
        table.getColumnModel().getColumn(0).setMaxWidth(32);
        table.getColumnModel().getColumn(2).setMaxWidth(64);
        scrollPane.setViewportView(table);

        JButton btnCopyAddress = new JButton("Copy Address");
        btnCopyAddress.addActionListener(this);
        btnCopyAddress.setActionCommand(Action.BTN_COPY_ADDRESS.name());

        JLabel qt = new JLabel("");
        qt.setIcon(SwingUtil.loadImage("send", 192, 192));
        qt.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(qt)
                        .addComponent(btnCopyAddress))
                    .addGap(20))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(qt)
                    .addGap(18)
                    .addComponent(btnCopyAddress)
                    .addContainerGap(61, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        setLayout(groupLayout);
        // @formatter:on
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
