package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import org.semux.core.Unit;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;

public class ReceivePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "#", "Address", "Balance", "Locked" };

    private Model model;

    private JTable table;
    private ReceiveTableModel tableModel;
    private JLabel qr;

    public ReceivePanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        tableModel = new ReceiveTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(24);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 500, 0.1, 0.6, 0.2, 0.2);
        SwingUtil.setColumnAlignments(table, false, false, true, true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        table.getSelectionModel().addListSelectionListener((ev) -> {
            actionPerformed(new ActionEvent(ReceivePanel.this, 0, Action.SELECT.name()));
        });

        JButton btnCopyAddress = new JButton("Copy Address");
        btnCopyAddress.addActionListener(this);
        btnCopyAddress.setActionCommand(Action.COPY_ADDRESS.name());

        qr = new JLabel("");
        qr.setIcon(SwingUtil.emptyImage(200, 200));
        qr.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(btnCopyAddress)
                        .addComponent(qr)))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(qr)
                    .addGap(18)
                    .addComponent(btnCopyAddress)
                    .addContainerGap(292, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
        );
        setLayout(groupLayout);
        // @formatter:on

        refresh();
    }

    class ReceiveTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<Account> data;

        public ReceiveTableModel() {
            this.data = Collections.emptyList();
        }

        public void setData(List<Account> data) {
            this.data = data;
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Account acc = data.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return "0x" + acc.getAddress().toAddressString();
            case 2:
                return String.format("%.3f SEM", acc.getBalance() / (double) Unit.SEM);
            case 3:
                return String.format("%.3f SEM", acc.getLocked() / (double) Unit.SEM);
            default:
                return null;
            }
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH: {
            refresh();
            break;
        }
        case SELECT: {
            int row = table.getSelectedRow();
            if (row != -1) {
                Account acc = model.getAccounts().get(row);
                BufferedImage bi = SwingUtil.generateQR("semux://" + acc.getAddress().toAddressString(), 200);
                qr.setIcon(new ImageIcon(bi));
            }
            break;
        }
        case COPY_ADDRESS: {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an account");
            } else {
                Account acc = model.getAccounts().get(row);

                StringSelection stringSelection = new StringSelection(acc.toString());
                Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
                clpbrd.setContents(stringSelection, null);

                JOptionPane.showMessageDialog(this, "Address copied: " + acc);
            }
            break;
        }
        default:
            break;
        }
    }

    private void refresh() {
        List<Account> list = model.getAccounts();

        // NOTE: This operation is safe as the order of accounts does not change.
        // However, this may change in the future
        int row = table.getSelectedRow();
        tableModel.setData(list);
        if (row != -1 && row < list.size()) {
            table.setRowSelectionInterval(row, row);
        }
    }
}
