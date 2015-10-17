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

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
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
import javax.swing.table.TableColumnModel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;


/**
 * The table of players.
 */
public final class PlayersTable extends JTable {

    /**
     * A table cell editor that can be used to select a nation.
     */
    private class AdvantageCellEditor extends DefaultCellEditor {

        private final JComboBox<EuropeanNationType> box;

        /**
         * Internal constructor.
         *
         * @param box The <code>JComboBox</code> to edit.
         */
        private AdvantageCellEditor(JComboBox<EuropeanNationType> box) {
            super(box);

            this.box = box;
        }

        /**
         * A standard constructor.
         *
         * @param nationTypes List of <code>EuropeanNationType></code>
         */
        public AdvantageCellEditor(List<EuropeanNationType> nationTypes) {
            this(new JComboBox<>(nationTypes
                    .toArray(new EuropeanNationType[0])));

            this.box.setRenderer(new FreeColComboBoxRenderer<EuropeanNationType>());
        }


        // Implement DefaultCellEditor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getCellEditorValue() {
            return ((JComboBox)getComponent()).getSelectedItem();
        }
    }

    private class AdvantageCellRenderer extends JLabel
        implements TableCellRenderer {

        /** The national advantages type. */
        private final Advantages advantages;

 
        /**
         * The default constructor.
         *
         * @param advantages The type of national <code>Advantages</code>.
         */
        public AdvantageCellRenderer(Advantages advantages) {
            this.advantages = advantages;
        }


        // Implement TableCellRenderer

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            final Player player = (Player)table.getValueAt(row,
                PlayersTable.PLAYER_COLUMN);
            final NationType nationType = ((Nation)table.getValueAt(row,
                    PlayersTable.NATION_COLUMN)).getType();
            JLabel label;
            switch (advantages) {
            case SELECTABLE:
                return Utility.localizedLabel(Messages.nameKey((player == null)
                        ? nationType
                        : player.getNationType()));
            case FIXED:
                label = Utility.localizedLabel(Messages.nameKey(nationType));
                break;
            case NONE:
            default:
                label = Utility.localizedLabel("none");
                break;
            }
            label.setForeground((player != null && player.isReady())
                ? Color.GRAY
                : table.getForeground());
            label.setBackground(table.getBackground());
            Utility.localizeToolTip(this, StringTemplate
                .key(advantages.getShortDescriptionKey()));
            return label;
        }
    }

    private static class AvailableCellRenderer extends JLabel
        implements TableCellRenderer {

        private final JComboBox<NationState> box
            = new JComboBox<>(NationState.values());


        /**
         * The default constructor.
         */
        public AvailableCellRenderer() {
            box.setRenderer(new NationStateRenderer());
        }


        // Implement TableCellRenderer

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            box.setSelectedItem(value);
            final NationState nationState = (NationState)value;
            Utility.localizeToolTip(this, StringTemplate
                .key(nationState.getShortDescriptionKey()));
            return box;
        }
    }

    private final class AvailableCellEditor extends AbstractCellEditor
        implements TableCellEditor {

        private final JComboBox<NationState> aiStateBox
            = new JComboBox<>(new NationState[] {
                    NationState.AI_ONLY,
                    NationState.NOT_AVAILABLE
                });
        private final JComboBox<NationState> allStateBox
            = new JComboBox<>(NationState.values());
        private JComboBox activeBox;

        private final ActionListener listener = (ActionEvent ae) -> {
            stopCellEditing();
        };


        public AvailableCellEditor() {
            aiStateBox.setRenderer(new NationStateRenderer());
            aiStateBox.addActionListener(listener);
            allStateBox.setRenderer(new NationStateRenderer());
            allStateBox.addActionListener(listener);
        }


        // Implement AbstractCellEditor

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
            NationType nationType = ((Nation) getValueAt(row, NATION_COLUMN))
                .getType();
            activeBox = (nationType instanceof EuropeanNationType)
                ? allStateBox
                : aiStateBox;
            return activeBox;
        }

        @Override
        public Object getCellEditorValue() {
            return activeBox.getSelectedItem();
        }
    }

    private static class HeaderListener extends MouseAdapter {

        private final JTableHeader header;

        private final HeaderRenderer renderer;


        public HeaderListener(JTableHeader header, HeaderRenderer renderer) {
            this.header = header;
            this.renderer = renderer;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int col = header.columnAtPoint(e.getPoint());
            renderer.setPressedColumn(col);
            header.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            renderer.setPressedColumn(HeaderRenderer.NO_COLUMN);
            header.repaint();
        }
    }

    private static class HeaderRenderer implements TableCellRenderer {

        private static final int NO_COLUMN = -1;
        private int pressedColumn = NO_COLUMN;
        private final Component[] components;

        public HeaderRenderer(Component... components) {
            this.components = components;
        }


        // Implement TableCellEditor

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            if (components[column] instanceof JButton) {
                boolean isPressed = (column == pressedColumn);
                ((JButton)components[column]).getModel().setPressed(isPressed);
                ((JButton)components[column]).getModel().setArmed(isPressed);
            }
            return components[column];
        }

        public void setPressedColumn(int column) {
            pressedColumn = column;
        }
    }

    private class NationCellRenderer extends JLabel
        implements TableCellRenderer {

        // Implement TableCellEditor

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            Nation nation = (Nation) value;
            setText(Messages.message(StringTemplate.template("countryName")
                    .add("%nation%", Messages.nameKey(nation.getId()))));
            setIcon(new ImageIcon(gui.getImageLibrary()
                    .getSmallerMiscIconImage(nation)));
            return this;
        }
    }

    private static class NationStateRenderer extends JLabel
        implements ListCellRenderer<NationState> {

        // Implement ListCellEditor<NationState>

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends NationState> list,
                                                      NationState value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(Messages.getName(value));
            return this;
        }
    }

    private static class PlayerCellRenderer implements TableCellRenderer {

        private final JLabel label = new JLabel();
        private final JButton button = Utility.localizedButton("select");


        public PlayerCellRenderer() {
            label.setHorizontalAlignment(JLabel.CENTER);
            Utility.padBorder(button, 5, 10, 5, 10);
        }


        // Implement TableCellRenderer

        /**
         * {@inheritDoc}
         */
        @Override
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
                label.setText(nation.getRulerName());
            } else {
                label.setText(player.getName());
            }
            return label;
        }
    }

    private final class PlayerCellEditor extends AbstractCellEditor
        implements TableCellEditor {

        private final JButton button = Utility.localizedButton("select");


        public PlayerCellEditor() {
            button.addActionListener((ActionEvent ae) -> {
                    fireEditingStopped();
                });
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return Boolean.TRUE;
        }
    }

    /**
     * The TableModel for the players table.
     */
    private static class PlayersTableModel extends AbstractTableModel {

        private final PreGameController preGameController;

        private final NationOptions nationOptions;

        private final Player thisPlayer;

        private final List<Nation> nations;

        private final Map<Nation, Player> players;


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
            nations = new ArrayList<>();
            players = new HashMap<>();
            for (Nation nation : thisPlayer.getSpecification().getNations()) {
                if (nation.isUnknownEnemy()) continue;
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
            for (Player player : thisPlayer.getGame().getLivePlayers(null)) {
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
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
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
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get the name of a column.
         *
         * @param column The column number to look up.
         * @return The name of the specified column.
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Get the number of rows in this table.
         *
         * @return The number of rows.
         */
        @Override
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
        @Override
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
        @Override
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
        @Override
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
                        preGameController.setNationType(nation.getType());
                        update();
                    }
                    break;
                default:
                    break;
                }
                fireTableCellUpdated(row, column);
            }
        }
    }


    public static final int NATION_COLUMN = 0,
        AVAILABILITY_COLUMN = 1,
        ADVANTAGE_COLUMN = 2,
        COLOR_COLUMN = 3,
        PLAYER_COLUMN = 4;

    private static final String[] columnNames = {
        Messages.message("nation"),
        Messages.message("playersTable.availability"),
        Messages.message("playersTable.advantage"),
        Messages.message("color"),
        Messages.message("player")
    };

    /** A link to the gui. */
    private final GUI gui;


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

        gui = freeColClient.getGUI();
        final Specification spec = freeColClient.getGame().getSpecification();

        setModel(new PlayersTableModel(freeColClient.getPreGameController(),
                 nationOptions, myPlayer));
        setRowHeight(47);

        JButton nationButton = Utility.localizedButton("nation");
        nationButton.addActionListener((ActionEvent ae) -> {
                gui.showColopediaPanel(PanelType.NATIONS.getKey());
            });

        JLabel availabilityLabel = Utility.localizedLabel("playersTable.availability");
        JButton advantageButton = Utility.localizedButton("playersTable.advantage");
        advantageButton.addActionListener((ActionEvent ae) -> {
                gui.showColopediaPanel(PanelType.NATION_TYPES.getKey());
            });

        JLabel colorLabel = Utility.localizedLabel("color");
        JLabel playerLabel = Utility.localizedLabel("player");

        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
        dtcr.setOpaque(false);

        HeaderRenderer renderer = new HeaderRenderer(nationButton,
            availabilityLabel, advantageButton, colorLabel, playerLabel);
        JTableHeader header = getTableHeader();
        header.addMouseListener(new HeaderListener(header, renderer));

        final TableColumnModel tcm = getColumnModel();
        
        TableColumn nationColumn = tcm.getColumn(NATION_COLUMN);
        nationColumn.setCellRenderer(new NationCellRenderer());
        nationColumn.setHeaderRenderer(renderer);
        nationColumn.setPreferredWidth(2 * tcm.getTotalColumnWidth()
            / tcm.getColumnCount());

        TableColumn availableColumn = tcm.getColumn(AVAILABILITY_COLUMN);
        availableColumn.setCellRenderer(new AvailableCellRenderer());
        availableColumn.setCellEditor(new AvailableCellEditor());

        TableColumn advantagesColumn = tcm.getColumn(ADVANTAGE_COLUMN);
        switch (nationOptions.getNationalAdvantages()) {
        case SELECTABLE:
            advantagesColumn.setCellEditor(new AdvantageCellEditor(spec
                    .getEuropeanNationTypes()));
            break;
        case FIXED:
            break; // Do nothing
        case NONE:
            spec.clearEuropeanNationalAdvantages();
            break;
        default:
            break;
        }
        advantagesColumn.setCellRenderer(new AdvantageCellRenderer(nationOptions.getNationalAdvantages()));
        advantagesColumn.setHeaderRenderer(renderer);

        TableColumn colorsColumn = tcm.getColumn(COLOR_COLUMN);
        colorsColumn.setCellRenderer(new ColorCellRenderer(true));
        colorsColumn.setCellEditor(new ColorCellEditor(freeColClient));

        TableColumn playerColumn = tcm.getColumn(PLAYER_COLUMN);
        playerColumn.setCellEditor(new PlayerCellEditor());
        playerColumn.setCellRenderer(new PlayerCellRenderer());
    }

    public void update() {
        ((PlayersTableModel)getModel()).update();
    }
}
