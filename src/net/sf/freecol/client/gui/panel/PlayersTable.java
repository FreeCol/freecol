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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ColorCellEditor;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The table of players.
 */
public final class PlayersTable extends JTable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PlayersTable.class.getName());

    public static final int NATION_COLUMN = 0,
        AVAILABILITY_COLUMN = 1,
        ADVANTAGE_COLUMN = 2,
        COLOR_COLUMN = 3,
        PLAYER_COLUMN = 4;

    private static final String[] columnNames = {
        Messages.message("nation"),
        Messages.message("availability"),
        Messages.message("advantage"),
        Messages.message("color"),
        Messages.message("player")
    };

    private static final NationState[] allStates = new NationState[] {
        NationState.AVAILABLE,
        NationState.AI_ONLY,
        NationState.NOT_AVAILABLE
    };

    private static final NationState[] aiStates = new NationState[] {
        NationState.AI_ONLY,
        NationState.NOT_AVAILABLE
    };

    private final ImageLibrary library;


    /**
     * Creates a players table.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param nationOptions The <code>NationOptions</code> for this game.
     * @param myPlayer The client <code>Player</code>.
     */
    public PlayersTable(final FreeColClient freeColClient,
                        NationOptions nationOptions, Player myPlayer) {
        super();

        final GUI gui = freeColClient.getGUI();
        final Specification spec = freeColClient.getGame().getSpecification();
        library = gui.getImageLibrary();

        setModel(new PlayersTableModel(freeColClient.getPreGameController(),
                nationOptions, myPlayer));
        setRowHeight(47);

        JButton nationButton = new JButton(Messages.message("nation"));
        JLabel availabilityLabel = new JLabel(Messages.message("availability"));
        JButton advantageButton = new JButton(Messages.message("advantage"));
        JLabel colorLabel = new JLabel(Messages.message("color"));
        JLabel playerLabel = new JLabel(Messages.message("player"));

        nationButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showColopediaPanel(PanelType.NATIONS.toString());
                }
            });

        advantageButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showColopediaPanel(PanelType.NATION_TYPES.toString());
                }
            });

        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
        dtcr.setOpaque(false);

        HeaderRenderer renderer = new HeaderRenderer(nationButton,
            availabilityLabel, advantageButton, colorLabel, playerLabel);
        JTableHeader header = getTableHeader();
        header.addMouseListener(new HeaderListener(header, renderer));

        TableColumn nationColumn = getColumnModel().getColumn(NATION_COLUMN);
        nationColumn.setCellRenderer(new NationCellRenderer());
        nationColumn.setHeaderRenderer(renderer);

        TableColumn availableColumn
            = getColumnModel().getColumn(AVAILABILITY_COLUMN);
        availableColumn.setCellRenderer(new AvailableCellRenderer());
        availableColumn.setCellEditor(new AvailableCellEditor());

        TableColumn advantagesColumn
            = getColumnModel().getColumn(ADVANTAGE_COLUMN);
        if (nationOptions.getNationalAdvantages()
            == NationOptions.Advantages.SELECTABLE) {
            advantagesColumn.setCellEditor(new AdvantageCellEditor(spec
                    .getEuropeanNationTypes()));
        }
        advantagesColumn.setCellRenderer(new AdvantageCellRenderer(nationOptions.getNationalAdvantages()));
        advantagesColumn.setHeaderRenderer(renderer);

        TableColumn colorsColumn = getColumnModel().getColumn(COLOR_COLUMN);
        colorsColumn.setCellRenderer(new ColorCellRenderer(true));
        colorsColumn.setCellEditor(new ColorCellEditor(freeColClient));

        TableColumn playerColumn = getColumnModel().getColumn(PLAYER_COLUMN);
        playerColumn.setCellEditor(new PlayerCellEditor());
        playerColumn.setCellRenderer(new PlayerCellRenderer());
    }

    public void update() {
        ((PlayersTableModel)getModel()).update();
    }

    private class HeaderRenderer implements TableCellRenderer {

        private static final int NO_COLUMN = -1;
        private int pressedColumn = NO_COLUMN;
        private Component[] components;

        public HeaderRenderer(Component... components) {
            this.components = components;
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            if (components[column] instanceof JButton) {
                boolean isPressed = (column == pressedColumn);
                ((JButton) components[column]).getModel().setPressed(isPressed);
                ((JButton) components[column]).getModel().setArmed(isPressed);
            }
            return components[column];
        }

        public void setPressedColumn(int column) {
            pressedColumn = column;
        }
    }

    private class HeaderListener extends MouseAdapter {

        private JTableHeader header;

        private HeaderRenderer renderer;

        public HeaderListener(JTableHeader header, HeaderRenderer renderer) {
            this.header = header;
            this.renderer = renderer;
        }

        public void mousePressed(MouseEvent e) {
            int col = header.columnAtPoint(e.getPoint());
            renderer.setPressedColumn(col);
            header.repaint();
        }

        public void mouseReleased(MouseEvent e) {
            renderer.setPressedColumn(HeaderRenderer.NO_COLUMN);
            header.repaint();
        }
    }

    private class AvailableCellRenderer implements TableCellRenderer {

        @SuppressWarnings("unchecked") // FIXME in Java7
        private JComboBox box = new JComboBox(allStates);

        @SuppressWarnings("unchecked") // FIXME in Java7
        public AvailableCellRenderer() {
            box.setRenderer(new NationStateRenderer());
        }

        /**
         * Gets the component used to render the cell's value.
         *
         * @param table The table whose cell needs to be rendered.
         * @param value The value of the cell being rendered.
         * @param hasFocus Indicates whether or not the cell in
         *     question has focus.
         * @param row The row index of the cell that is being rendered.
         * @param column The column index of the cell that is being rendered.
         * @return The component used to render the cell's value.
         */
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            box.setSelectedItem(value);
            return box;
        }
    }

    private final class AvailableCellEditor extends AbstractCellEditor
        implements TableCellEditor {

        @SuppressWarnings("unchecked") // FIXME in Java7
        private JComboBox aiStateBox = new JComboBox(aiStates);
        @SuppressWarnings("unchecked") // FIXME in Java7
        private JComboBox allStateBox = new JComboBox(allStates);
        private JComboBox activeBox;

        private ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing();
                }
            };

        @SuppressWarnings("unchecked") // FIXME in Java7
        public AvailableCellEditor() {
            aiStateBox.setRenderer(new NationStateRenderer());
            aiStateBox.addActionListener(listener);
            allStateBox.setRenderer(new NationStateRenderer());
            allStateBox.addActionListener(listener);
        }

        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
            NationType nationType = ((Nation) getValueAt(row, NATION_COLUMN))
                .getType();
            activeBox = (nationType instanceof EuropeanNationType)
                ? allStateBox
                : aiStateBox;
            return activeBox;
        }

        public Object getCellEditorValue() {
            return activeBox.getSelectedItem();
        }
    }

    private class NationCellRenderer extends JLabel
        implements TableCellRenderer {

        /**
         * Gets the component used to render the cell's value.
         *
         * @param table The table whose cell needs to be rendered.
         * @param value The value of the cell being rendered.
         * @param hasFocus Indicates whether or not the cell in
         *     question has focus.
         * @param row The row index of the cell that is being rendered.
         * @param column The column index of the cell that is being rendered.
         * @return The component used to render the cell's value.
         */
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            Nation nation = (Nation) value;
            setText(Messages.message(nation.getNameKey()));
            setIcon(new ImageIcon(library.getCoatOfArmsImage(nation, 0.5)));
            return this;
        }
    }

    private class NationStateRenderer extends JLabel
        implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            setText(Messages.message("nationState."
                    + ((NationState)value).toString()));
            return this;
        }
    }

    private class PlayerCellRenderer implements TableCellRenderer {

        JLabel label = new JLabel();
        JButton button = new JButton(Messages.message("select"));

        public PlayerCellRenderer() {
            label.setHorizontalAlignment(JLabel.CENTER);
            button.setBorder(BorderFactory
                .createCompoundBorder(BorderFactory
                    .createEmptyBorder(5, 10, 5, 10),
                    button.getBorder()));
        }

        /**
         * Gets the component used to render the cell's value.
         *
         * @param table The table whose cell needs to be rendered.
         * @param value The value of the cell being rendered.
         * @param hasFocus Indicates whether or not the cell in
         *     question has focus.
         * @param row The row index of the cell that is being rendered.
         * @param column The column index of the cell that is being rendered.
         * @return The component used to render the cell's value.
         */
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            Player player = (Player)value;
            if (player == null) {
                NationType nationType
                    = (NationType)table.getValueAt(row, ADVANTAGE_COLUMN);
                if (nationType instanceof EuropeanNationType) {
                    NationState nationState = (NationState)table
                        .getValueAt(row, AVAILABILITY_COLUMN);
                    if (nationState == NationState.AVAILABLE) {
                        return button;
                    }
                }
                Nation nation = (Nation) table.getValueAt(row, NATION_COLUMN);
                label.setText(Messages.message(nation.getRulerNameKey()));
            } else {
                label.setText(player.getDisplayName());
            }
            return label;
        }
    }

    private final class PlayerCellEditor extends AbstractCellEditor
        implements TableCellEditor {

        private JButton button = new JButton(Messages.message("select"));

        public PlayerCellEditor() {
            button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fireEditingStopped();
                    }
                });
        }

        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
            return button;
        }

        public Object getCellEditorValue() {
            return true;
        }
    }

    /**
     * The TableModel for the players table.
     */
    private class PlayersTableModel extends AbstractTableModel {

        private final PreGameController preGameController;

        private NationOptions nationOptions;

        private Player thisPlayer;

        private List<Nation> nations;

        private Map<Nation, Player> players;


        /**
         * A standard constructor.
         *
         * @param preGameController The <code>PreGameController</code>
         *     to use notify of updates.
         * @param nationOptions The current <code>NationOptions</code>.
         * @param thisPlayer The <code>Player</code> that owns the client.
         */
        public PlayersTableModel(PreGameController preGameController,
                                 NationOptions nationOptions,
                                 Player thisPlayer) {
            this.preGameController = preGameController;
            this.nationOptions = nationOptions;
            this.thisPlayer = thisPlayer;
            nations = new ArrayList<Nation>();
            players = new HashMap<Nation, Player>();
            for (Nation nation : thisPlayer.getSpecification().getNations()) {
                NationState state = nationOptions.getNations().get(nation);
                if (state != null) {
                    nations.add(nation);
                    players.put(nation, null);
                }
            }
            players.put(thisPlayer.getNation(), thisPlayer);
        }

        public void update() {
            for (Nation nation : nations) {
                players.put(nation, null);
            }
            for (Player player : thisPlayer.getGame().getPlayers()) {
                players.put(player.getNation(), player);
            }
            fireTableDataChanged();
        }

        /**
         * Gets the class of the objects in the given column.
         *
         * @param column The column to return the class of.
         * @return The <code>Class</code> of the objects in the given column.
         */
        public Class<?> getColumnClass(int column) {
            switch(column) {
            case NATION_COLUMN:
                return Nation.class;
            case AVAILABILITY_COLUMN:
                return NationOptions.NationState.class;
            case ADVANTAGE_COLUMN:
                return NationType.class;
            case COLOR_COLUMN:
                return Color.class;
            case PLAYER_COLUMN:
                return Player.class;
            }
            return String.class;
        }

        /**
         * Get the number of columns in this table.
         *
         * @return The number of columns.
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get the name of a column.
         *
         * @param column The column number to look up.
         * @return The name of the specified column.
         */
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Get the number of rows in this table.
         *
         * @return The number of rows.
         */
        public int getRowCount() {
            return nations.size();
        }

        /**
         * Get the value at the requested location.
         *
         * @param row The requested row.
         * @param column The requested column.
         * @return The value at the requested location.
         */
        public Object getValueAt(int row, int column) {
            if (row >= 0 && row < getRowCount()
                && column >= 0 && column < getColumnCount()) {
                Nation nation = nations.get(row);
                switch (column) {
                case NATION_COLUMN:
                    return nation;
                case AVAILABILITY_COLUMN:
                    return nationOptions.getNationState(nation);
                case ADVANTAGE_COLUMN:
                    return (players.get(nation) == null) ? nation.getType()
                        : players.get(nation).getNationType();
                case COLOR_COLUMN:
                    return nation.getColor();
                case PLAYER_COLUMN:
                    return players.get(nation);
                }
            }
            return null;
        }

        /**
         * Is a cell editable?
         *
         * @param row The specified row.
         * @param column The specified column.
         * @return True if the specified cell is editable.
         */
        public boolean isCellEditable(int row, int column) {
            if (row >= 0 && row < getRowCount()) {
                Nation nation = nations.get(row);
                boolean ownRow = thisPlayer == players.get(nation)
                    && !thisPlayer.isReady();
                switch (column) {
                case AVAILABILITY_COLUMN:
                    return !ownRow && thisPlayer.isAdmin();
                case ADVANTAGE_COLUMN:
                    return nation.getType() instanceof EuropeanNationType
                        && ownRow;
                case COLOR_COLUMN:
                    // Allow a player to change all the colors. 
                    // This is an accessibility issue for users with a
                    // colour vision deficiency.  Better to support them
                    // and just admit that if someone wants to be a pain
                    // and set all the colours the same, they can.
                    return nation.getType() instanceof EuropeanNationType;
                case PLAYER_COLUMN:
                    return nation.getType() instanceof EuropeanNationType
                        && players.get(nation) == null;
                }
            }
            return false;
        }

        /**
         * Sets the value at the specified location.
         *
         * @param value The new value.
         * @param row The specified row.
         * @param column The specified column.
         */
        public void setValueAt(Object value, int row, int column) {
            if (row >= 0 && row < getRowCount()
                && column > 0 && column < getColumnCount()) {
                // Column 0 can't be updated.
                Nation nation = nations.get(row);
                switch (column) {
                case ADVANTAGE_COLUMN:
                    preGameController.setNationType((NationType)value);
                    break;
                case AVAILABILITY_COLUMN:
                    preGameController.setAvailable(nations.get(row),
                        (NationState)value);
                    update();
                    break;
                case COLOR_COLUMN:
                    preGameController.setColor(nation, (Color)value);
                    break;
                case PLAYER_COLUMN:
                    if (nationOptions.getNationState(nation)
                        == NationState.AVAILABLE) {
                        preGameController.setNation(nation);
                        if (nationOptions.getNationalAdvantages()
                            != NationOptions.Advantages.NONE) {
                            preGameController.setNationType(nation.getType());
                        }
                        update();
                    }
                    break;
                }

                fireTableCellUpdated(row, column);
            }
        }
    }
}
