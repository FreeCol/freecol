/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Display the current game statistics.
 */
public final class StatisticsPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(StatisticsPanel.class.getName());

    private static final String[] memoryKeys = {
        "freeMemory", "totalMemory", "maxMemory"
    };

    private static class StatisticsModel extends AbstractTableModel {

        private static final int NAME_COLUMN = 0, VALUE_COLUMN = 1;
        private final String[] columnNames = { "name", "value" };

        private Object data[][] = null;


        /**
         * A standard constructor.
         */
        public StatisticsModel() {}

        /**
         * Gives this table model the data that is being used in the
         * table. This method should only be called to initialize the
         * data set. To modify or extend the data set use other
         * methods.
         */
        public void setData(java.util.Map<String, String> statsData) {
            this.data = new Object[2][statsData.size()];
            int i = 0;
            for (Entry<String, String> e : mapEntriesByKey(statsData)) {
                data[NAME_COLUMN][i] = e.getKey();
                data[VALUE_COLUMN][i] = e.getValue();
                i++;
            }
        }

        // AbstractTableModel

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int column) {
            return Messages.message(columnNames[column]);
        }

        ///**
        // * {@inheritDoc}
        // */
        //@Override
        //public boolean isCellEditable(int row, int column) {
        //    return false;
        //}


        // Interface TableModel

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return data[NAME_COLUMN].length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
    }


    /**
     * Creates the statistics panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public StatisticsPanel(FreeColClient freeColClient) {
        super(freeColClient, new BorderLayout());

        // Retrieve the client and server data
        Map<String, String> serverStatistics = igc().getServerStatistics();
        Map<String, String> clientStatistics = freeColClient.getGame()
            .getStatistics();

        // Title
        JPanel header = new JPanel();
        this.add(header, BorderLayout.NORTH);
        header.add(Utility.localizedLabel("statistics"), JPanel.CENTER_ALIGNMENT);

        // Actual stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1,2));
        JScrollPane scrollPane = new JScrollPane(statsPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // correct way to make scroll pane opaque
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        this.add(scrollPane,BorderLayout.CENTER);
        statsPanel.add(displayStatsMessage("client", clientStatistics));
        statsPanel.add(displayStatsMessage("server", serverStatistics));

        add(okButton, BorderLayout.SOUTH);

        setSize(getPreferredSize());
    }


    private JPanel displayStatsMessage(String title,
                                       Map<String, String> stats) {
        JPanel panel = new JPanel();
        panel.setBorder(Utility.localizedBorder(title));
        Box b = new Box(BoxLayout.Y_AXIS);
        panel.add(b);
        Map<String, String> memory = new HashMap<>();
        Map<String, String> ai = new HashMap<>();
        for (String k : memoryKeys) {
            memory.put(Messages.message("memoryManager." + k),
                       stats.remove(k));
        }
        for (String k : new ArrayList<>(stats.keySet())) {
            if (k.startsWith("AI")) { // FIXME: AIMain.aiStatisticsPrefix
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
