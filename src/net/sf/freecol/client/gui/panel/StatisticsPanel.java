/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Display the current game statistics.
 */
public final class StatisticsPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(StatisticsPanel.class.getName());

    class StatisticsModel extends AbstractTableModel {

        private static final int NAME_COLUMN = 0, VALUE_COLUMN = 1;
        private final String[] columnNames = { "Name", "Value" };

        private Object data[][] = null;

        /**
         * A standard constructor.
         */
        public StatisticsModel() {
        }

        /**
         * Gives this table model the data that is being used in the
         * table. This method should only be called to initialize the
         * data set. To modify or extend the data set use other
         * methods.
         */
        public void setData(java.util.Map<String, String> statsData) {
            this.data = new Object[2][statsData.size()];
            int i=0;
            List<String> keys = new ArrayList<String>(statsData.keySet());
            Collections.sort(keys);
            for (String s : keys) {
                data[NAME_COLUMN][i] = s;
                data[VALUE_COLUMN][i] = statsData.get(s);
                i++;
            }
        }

        /**
         * Returns the amount of columns in this statesTable.
         *
         * @return The amount of columns in this statesTable.
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the name of the specified column.
         *
         * @return The name of the specified column.
         */
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Returns the amount of rows in this statesTable.
         *
         * @return The amount of rows in this statesTable.
         */
        public int getRowCount() {
            return data[NAME_COLUMN].length;
        }

        /**
         * Returns the value at the requested location.
         *
         * @param row The requested row.
         * @param column The requested column.
         * @return The value at the requested location.
         */
        public Object getValueAt(int row, int column) {
            if ((row < getRowCount()) && (column < getColumnCount())
                && (row >= 0) && (column >= 0)) {
                switch (column) {
                case StatisticsModel.NAME_COLUMN:
                    return data[NAME_COLUMN][row];
                case StatisticsModel.VALUE_COLUMN:
                    return data[VALUE_COLUMN][row];
                }
            }
            return null;
        }

        /**
         * Returns 'true' if the specified cell is editable, 'false' otherwise.
         *
         * @param row The specified row.
         * @param column The specified column.
         * @return 'true' if the specified cell is editable, 'false' otherwise.
         */
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * Returns the Class of the objects in the given column.
         */
        public Class<?> getColumnClass(int column) {
            return String.class;
        }
    }


    /**
     * Creates the statistics panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public StatisticsPanel(FreeColClient freeColClient) {
        super(freeColClient, new BorderLayout());

        // Retrieve the client and server data
        Map<String, String> serverStatistics
            = getController().getServerStatistics();
        Map<String, String> clientStatistics
            = getController().getClientStatistics();

        // Title
        JPanel header = new JPanel();
        this.add(header, BorderLayout.NORTH);
        header.add(new JLabel("Statistics"),JPanel.CENTER_ALIGNMENT);

        // Actual stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1,2));
        JScrollPane scrollPane = new JScrollPane(statsPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // correct way to make scroll pane opaque
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        this.add(scrollPane,BorderLayout.CENTER);
        statsPanel.add(displayStatsMessage("Client", clientStatistics));
        statsPanel.add(displayStatsMessage("Server", serverStatistics));

        add(okButton, BorderLayout.SOUTH);

        setSize(getPreferredSize());
    }


    private JPanel displayStatsMessage(String title,
                                       Map<String, String> stats) {
        final String[] memoryKeys = {
            "freeMemory", "totalMemory", "maxMemory"
        };
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        Box b = new Box(BoxLayout.Y_AXIS);
        panel.add(b);
        Map<String, String> memory = new HashMap<String, String>();
        Map<String, String> ai = new HashMap<String, String>();
        for (String k : memoryKeys) {
            memory.put(Messages.message("menuBar.debug.memoryManager." + k),
                       stats.remove(k));
        }
        for (String k : new ArrayList<String>(stats.keySet())) {
            if (k.startsWith("AI")) { // TODO: AIMain.aiStatisticsPrefix
                ai.put(k, stats.remove(k));
            }
        }
        b.add(createStatsTable("Memory", memory));
        b.add(createStatsTable("Game", stats));
        if (ai.isEmpty()) {
            b.add(new JLabel());
        } else {
            b.add(createStatsTable("AI", ai));
        }
        return panel;
    }

    private JPanel createStatsTable(String title, Map<String, String> data) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);
        StatisticsModel model = new StatisticsModel();
        model.setData(data);
        JTable table = new JTable(model);
        table.setAutoCreateColumnsFromModel(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);
        table.addNotify();
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getColumnHeader().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(300, (data.size()+2)*17));
        return panel;
    }
}
