package org.semux.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import org.semux.core.Delegate;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.Model;
import org.semux.gui.Model.Account;
import org.semux.utils.Bytes;

public class DelegatesPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Model model;
    private JTable table;
    private DelegatesTableModel tableModel;

    JComboBox<String> from;

    private JTextField textVote;
    private JTextField textUnvote;
    private JTextField textName;

    public DelegatesPanel(Model model) {
        this.model = model;
        this.model.addListener(this);

        JScrollPane scrollPane = new JScrollPane();
        tableModel = new DelegatesTableModel();
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(128);
        table.getColumnModel().getColumn(2).setMaxWidth(128);
        scrollPane.setViewportView(table);

        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JPanel panel2 = new JPanel();
        panel2.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JLabel label = new JLabel(
                "<html>NOTE: A minimum transaction fee (5 mSEM) will apply when you vote/unvote/register a delegate.</p></html>");
        label.setForeground(Color.DARK_GRAY);

        from = new JComboBox<>();

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(from, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panel, 0, 200, Short.MAX_VALUE)
                        .addComponent(panel2, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addComponent(label, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(5)
                    .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(panel2, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(label)
                    .addContainerGap(88, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
        );
        setLayout(groupLayout); 
        // @formatter:on

        textVote = new JTextField();
        textVote.setToolTipText("# of votes");
        textVote.setColumns(10);

        JButton btnVote = new JButton("Vote");
        btnVote.setActionCommand(Action.VOTE.name());
        btnVote.addActionListener(this);

        textUnvote = new JTextField();
        textUnvote.setToolTipText("# of votes");
        textUnvote.setColumns(10);

        JButton btnUnvote = new JButton("Unvote");
        btnUnvote.setActionCommand(Action.UNVOTE.name());
        btnUnvote.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout2 = new GroupLayout(panel);
        groupLayout2.setHorizontalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout2.createParallelGroup(Alignment.LEADING)
                        .addComponent(textUnvote, 0, 0, Short.MAX_VALUE)
                        .addComponent(textVote, GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.TRAILING)
                        .addComponent(btnVote)
                        .addComponent(btnUnvote, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
        );
        groupLayout2.setVerticalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addGap(16)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnVote)
                        .addComponent(textVote, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(textUnvote, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnUnvote)))
        );
        panel.setLayout(groupLayout2);
        // @formatter:on

        JButton btnDelegate = new JButton("Register as delegate");
        btnDelegate.addActionListener(this);
        btnDelegate.setActionCommand(Action.DELEGATE.name());

        textName = new JTextField();
        textName.setToolTipText("Name");
        textName.setColumns(10);

        // @formatter:off
        GroupLayout groupLayout3 = new GroupLayout(panel2);
        groupLayout3.setHorizontalGroup(
            groupLayout3.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout3.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout3.createParallelGroup(Alignment.TRAILING)
                        .addComponent(btnDelegate, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                        .addComponent(textName, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout3.setVerticalGroup(
            groupLayout3.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout3.createSequentialGroup()
                    .addGap(10)
                    .addComponent(textName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18)
                    .addComponent(btnDelegate)
                    .addContainerGap(10, Short.MAX_VALUE))
        );
        panel2.setLayout(groupLayout3);
        // @formatter:on

        refresh();
    }

    class DelegatesTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private String[] columnNames = { "Name", "Address", "Votes" };
        private List<Delegate> data;

        public DelegatesTableModel() {
            this.data = Collections.emptyList();
        }

        public void setData(List<Delegate> data) {
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
            Delegate d = data.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return Bytes.toString(d.getName());
            case 1:
                return Hex.encode(d.getAddress());
            case 2:
                return d.getVote();
            default:
                return null;
            }
        }
    }

    public int getFrom() {
        return from.getSelectedIndex();
    }

    public void setFromItems(List<? extends Object> items) {
        int n = from.getSelectedIndex();

        from.removeAllItems();
        for (Object item : items) {
            from.addItem(item.toString());
        }

        if (!items.isEmpty()) {
            from.setSelectedIndex(n >= 0 && n < items.size() ? n : 0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        default:
            break;
        }
    }

    private void refresh() {
        List<String> list1 = new ArrayList<>();
        List<Account> accounts = model.getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            list1.add("Account #" + i);
        }
        setFromItems(list1);

        List<Delegate> list2 = model.getDelegates();
        list2.sort((d1, d2) -> {
            int c = Long.compare(d2.getVote(), d1.getVote());
            return c != 0 ? c : new String(d1.getName()).compareTo(new String(d2.getName()));
        });

        int row = table.getSelectedRow();
        tableModel.setData(list2);
        if (row != -1 && row < list2.size()) {
            table.setRowSelectionInterval(row, row);
        }
    }
}
