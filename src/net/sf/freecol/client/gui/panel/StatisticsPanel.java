/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.networking.StatisticsMessage;

/**
 * This is the StatisticsPanel panel 
 */
public final class StatisticsPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(StatisticsPanel.class.getName());

    private static final int CLOSE = 0;
        
    private final Canvas parent;
 
    private JButton exitButton;
    
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
         * Gives this table model the data that is being used in the table. This
         * method should only be called to initialize the data set. To modify or
         * extend the data set use other methods.
         */
        public void setData(HashMap<String,Long> statsData) {
            this.data = new Object[2][statsData.size()];
            int i=0;
            for (String s : statsData.keySet()) {
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
            if ((row < getRowCount()) && (column < getColumnCount()) && (row >= 0) && (column >= 0)) {
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
            return getValueAt(0, column).getClass();
        }
    }
    
    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the
    *       client.
    */
    public StatisticsPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        
        setLayout(new BorderLayout());
        
        // Retrieve the client and server data
        StatisticsMessage serverStatistics = freeColClient.getInGameController().getServerStatistics();
        StatisticsMessage clientStatistics = new StatisticsMessage(freeColClient.getGame(), null);

        // Title
        JPanel header = new JPanel();
        this.add(header, BorderLayout.NORTH);
        header.add(new JLabel("Statistics"),JPanel.CENTER_ALIGNMENT);
        
        // Actual stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1,2)); 
        this.add(statsPanel,BorderLayout.CENTER);
        statsPanel.add(displayStatsMessage("Client", clientStatistics));
        statsPanel.add(displayStatsMessage("Server", serverStatistics));

        // Close button
        exitButton = new JButton(Messages.message("close"));
        exitButton.addActionListener(this);
        enterPressesWhenFocused(exitButton);
        setCancelComponent(exitButton);
        exitButton.setActionCommand(String.valueOf(CLOSE));        
        this.add(exitButton,BorderLayout.SOUTH);
        exitButton.setFocusable(true);

        setSize(getPreferredSize());
    }
    
    private JPanel displayStatsMessage(String title, StatisticsMessage statistics) {
        JPanel panel = new JPanel(new GridLayout(3,1));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(createStatsTable("Memory", statistics.getMemoryStatistics()));
        panel.add(createStatsTable("Game", statistics.getGameStatistics()));
        if (statistics.getAIStatistics()!=null) {
            panel.add(createStatsTable("AI", statistics.getAIStatistics()));
        } else {
            panel.add(new JLabel());
        }
        return panel;
    }
    
    private JPanel createStatsTable(String title, HashMap<String,Long> data) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);
        StatisticsModel model = new StatisticsModel();
        model.setData(data);
        JTable table = new JTable(model);
        table.setAutoCreateColumnsFromModel(true);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollPane = new JScrollPane(table);
        table.addNotify();
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getColumnHeader().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(300, 150));
        return panel;
    }

    private String serializeStats(String title, HashMap<String, Long> stats) {
        String message = title+"\n";
        long total = 0;
        for (String s :stats.keySet()) {
            Long value = stats.get(s);
            message += s+": "+value.toString()+"\n";
            total += value;
        }
        message += "Total: "+total+"\n";
        return message;
    }
    
    /**
    * This function analyzes an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case CLOSE:
                    parent.remove(this);
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

}
