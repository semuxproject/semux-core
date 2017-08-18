package org.semux.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;

import org.semux.gui.Action;

public class DelegatesPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private String[] columnNames = { "Name", "Address", "Votes" };
    private Object[][] data = { { "test", "0x1122334455667788112233445566778811223344", new Integer(12) } };

    private JTextField textVote;
    private JTextField textUnvote;

    public DelegatesPanel() {
        JScrollPane scrollPane = new JScrollPane();
        table = new JTable(data, columnNames);
        table.getColumnModel().getColumn(0).setMaxWidth(96);
        table.getColumnModel().getColumn(2).setMaxWidth(64);
        scrollPane.setViewportView(table);

        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                    .addGap(18)
                    .addComponent(panel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(panel, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(160, Short.MAX_VALUE))
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        // @formatter:on

        textVote = new JTextField();
        textVote.setColumns(10);

        JButton btnVote = new JButton("Vote");
        btnVote.setActionCommand(Action.BTN_VOTE.name());
        btnVote.addActionListener(this);

        textUnvote = new JTextField();
        textUnvote.setColumns(10);

        JButton btnUnvote = new JButton("Unvote");
        btnUnvote.setActionCommand(Action.BTN_UNVOTE.name());
        btnUnvote.addActionListener(this);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setActionCommand(Action.BTN_REFRESH.name());
        btnRefresh.addActionListener(this);

        // @formatter:off
        GroupLayout groupLayout2 = new GroupLayout(panel);
        groupLayout2.setHorizontalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout2.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(btnRefresh, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(Alignment.LEADING, groupLayout2.createSequentialGroup()
                            .addGroup(groupLayout2.createParallelGroup(Alignment.TRAILING, false)
                                .addComponent(textUnvote, 0, 0, Short.MAX_VALUE)
                                .addComponent(textVote, GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(groupLayout2.createParallelGroup(Alignment.LEADING)
                                .addComponent(btnVote)
                                .addComponent(btnUnvote, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE))))
                    .addGap(179))
        );
        groupLayout2.setVerticalGroup(
            groupLayout2.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout2.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(textVote, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnVote))
                    .addGap(18)
                    .addGroup(groupLayout2.createParallelGroup(Alignment.BASELINE)
                        .addComponent(textUnvote, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnUnvote))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(btnRefresh)
                    .addContainerGap(15, Short.MAX_VALUE))
        );
        panel.setLayout(groupLayout2);
        setLayout(groupLayout);
        // @formatter:on
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
    }
}
