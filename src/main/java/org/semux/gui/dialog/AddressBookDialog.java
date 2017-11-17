/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.semux.gui.Action;
import org.semux.gui.MessagesUtil;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.AddressBook;
import org.semux.gui.model.SemuxAddress;
import org.semux.gui.panel.AddressChangeDialog;
import org.semux.util.UnreachableException;

public class AddressBookDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { MessagesUtil.get("Name"), MessagesUtil.get("Address") };

    private JTable table;
    private AdressTableModel tableModel;
    private JTextField nameField;
    private JTextField addressField;

    public AddressBookDialog(JPanel parent) {
        setLayout(new BorderLayout(0, 0));
        this.setMinimumSize(new Dimension(900, 600));
        this.setLocationRelativeTo(parent);
        tableModel = new AdressTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 800, 0.25, 0.75);
        SwingUtil.setColumnAlignments(table, false, false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    SemuxAddress address = tableModel.getRow(table.convertRowIndexToModel(row));
                    if (address != null) {
                        AddressChangeDialog dialog = new AddressChangeDialog(AddressBookDialog.this, address);
                        dialog.setVisible(true);
                    }
                }
            }
        });

        // customized table sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(0, SwingUtil.STRING_COMPARATOR);
        sorter.setComparator(1, SwingUtil.STRING_COMPARATOR);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

        add(scrollPane);
        JButton addButton = SwingUtil.createDefaultButton(MessagesUtil.get("Add"), this, Action.ADD_TO_ADDRESSBOOK);

        nameField = SwingUtil.textFieldWithCopyPastePopup();
        nameField.setColumns(20);
        addressField = SwingUtil.textFieldWithCopyPastePopup();
        addressField.setColumns(40);

        JLabel nameLabel = new JLabel(MessagesUtil.get("Name"));
        JLabel addressLabel = new JLabel(MessagesUtil.get("Address"));

        JPanel addPanel = new JPanel();
        addPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(addPanel, GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                        .addGap(18)
                        ).addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(addPanel, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
        );
       this.getContentPane().setLayout(groupLayout);
        // @formatter:on

        // @formatter:off
        GroupLayout addPanelLayout = new GroupLayout(addPanel);
        addPanelLayout.setHorizontalGroup(
                addPanelLayout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(addPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(addPanelLayout.createParallelGroup(Alignment.LEADING)
                            .addGap(16)
                            .addComponent(nameLabel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGap(4)
                            .addComponent(nameField, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGap(16)
                            .addComponent(addressLabel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGap(4)
                            .addComponent(addressField, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGap(16)
                            .addComponent(addButton, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                            .addGap(16))
                        .addContainerGap())
             );
            addPanelLayout.setVerticalGroup(
                addPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(addPanelLayout.createSequentialGroup()
                        .addGap(16)
                        .addComponent(nameLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addGap(4)
                        .addComponent(nameField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addGap(16)
                        .addComponent(addressLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addGap(4)
                        .addComponent(addressField, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addGap(16)
                        .addComponent(addButton, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
            );
        addPanel.setLayout(addPanelLayout);
        // @formatter:on

        refresh();

    }

    class AdressTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<SemuxAddress> addresses;

        public AdressTableModel() {
            this.addresses = Collections.emptyList();
        }

        public void setData(List<SemuxAddress> addresses) {
            this.addresses = addresses;
            this.fireTableDataChanged();
        }

        public SemuxAddress getRow(int row) {
            if ((row >= 0) && (row < addresses.size())) {
                return addresses.get(row);
            }

            return null;
        }

        @Override
        public int getRowCount() {
            return addresses.size();
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
        public Object getValueAt(int row, int column) {
            SemuxAddress adr = addresses.get(row);

            switch (column) {
            case 0:
                return adr.getName();
            case 1:
                return adr.getAddress();

            default:
                return null;
            }
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case ADD_TO_ADDRESSBOOK:
            AddressBook.put(nameField.getText(), addressField.getText());
            nameField.setText("");
            addressField.setText("");
            JOptionPane.showMessageDialog(this, MessagesUtil.get("InvalidReceivingAddress"));
            refresh();
            break;
        default:
            throw new UnreachableException();
        }
    }

    public void refresh() {
        List<SemuxAddress> addresses = AddressBook.getAllAddresses();

        /*
         * update table model
         */

        tableModel.setData(addresses);

        for (int i = 0; i < addresses.size(); i++) {
            table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
        }
    }
}
