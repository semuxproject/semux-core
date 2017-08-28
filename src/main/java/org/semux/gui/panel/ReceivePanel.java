package org.semux.gui.panel;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.semux.core.Unit;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.gui.SwingUtil;

public class ReceivePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Model model;
    private JTable table;
    private ReceiveTableModel tableModel;

    public ReceivePanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        JScrollPane scrollPane = new JScrollPane();
        tableModel = new ReceiveTableModel();
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(32);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        scrollPane.setViewportView(table);

        JButton btnCopyAddress = new JButton("Copy Address");
        btnCopyAddress.addActionListener(this);
        btnCopyAddress.setActionCommand(Action.COPY_ADDRESS.name());

        JLabel qr = new JLabel("");
        qr.setIcon(SwingUtil.loadImage("send", 200, 200));
        qr.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(btnCopyAddress)
                        .addComponent(qr)))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(qr)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(btnCopyAddress)
                    .addContainerGap(18, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        setLayout(groupLayout);
        // @formatter:on

        refresh();
    }

    class ReceiveTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private String[] columnNames = { "#", "Address", "Balance", "Locked" };
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
                return acc.getAddress().toAddressString();
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
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case COPY_ADDRESS:
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
        default:
            break;
        }
    }

    private void refresh() {
        List<Account> list = model.getAccounts();

        int row = table.getSelectedRow();
        tableModel.setData(list);
        if (row != -1 && row < list.size()) {
            table.setRowSelectionInterval(row, row);
        }
    }
}
