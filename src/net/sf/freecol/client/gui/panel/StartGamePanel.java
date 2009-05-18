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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
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
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.panel.ColopediaPanel.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import net.miginfocom.swing.MigLayout;

/**
 * The panel where you choose your nation and color and connected players are
 * shown.
 */
public final class StartGamePanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private static final int START = 0, CANCEL = 1,
        READY = 3, CHAT = 4, GAME_OPTIONS = 5, MAP_GENERATOR_OPTIONS = 6;

    public static final int NATION_COLUMN = 0, AVAILABILITY_COLUMN = 1, ADVANTAGE_COLUMN = 2,
        COLOR_COLUMN = 3, PLAYER_COLUMN = 4;

    private static final EuropeanNationType[] europeans = 
        FreeCol.getSpecification().getEuropeanNationTypes().toArray(new EuropeanNationType[0]);

    private static final NationState[] allStates = new NationState[] {
        NationState.AVAILABLE,
        NationState.AI_ONLY,
        NationState.NOT_AVAILABLE
    };

    private static final String[] allStateNames = new String[] {
        NationState.AVAILABLE.getName(),
        NationState.AI_ONLY.getName(),
        NationState.NOT_AVAILABLE.getName()
    };

    private static final String[] aiStateNames = new String[] {
        NationState.AI_ONLY.getName(),
        NationState.NOT_AVAILABLE.getName()
    };


    private boolean singlePlayerGame;

    private JCheckBox readyBox;

    private JTextField chat;

    private JTextArea chatArea;

    private JButton start;

    private JButton gameOptions;

    private JButton mapGeneratorOptions;

    private JTable table = new JTable();

    private final ListCellRenderer stateBoxRenderer = new NationStateRenderer();

    private PlayersTableModel tableModel;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public StartGamePanel(final Canvas parent) {
        super(parent);
    }

    public void initialize(boolean singlePlayer) {

        removeAll();
        this.singlePlayerGame = singlePlayer;

        NationOptions nationOptions = getGame().getNationOptions();

        JButton cancel = new JButton(Messages.message("cancel"));

        JScrollPane chatScroll, tableScroll;

        setCancelComponent(cancel);

        tableModel = new PlayersTableModel(getClient().getPreGameController(), nationOptions, getMyPlayer());
        table = new JTable(tableModel);
        table.setRowHeight(22);

        JButton nationButton = new JButton(Messages.message("nation"));
        JLabel availabilityLabel = new JLabel(Messages.message("availability"));
        JButton advantageButton = new JButton(Messages.message("advantage"));
        JLabel colorLabel = new JLabel(Messages.message("color"));
        JLabel playerLabel = new JLabel(Messages.message("player"));

        nationButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showColopediaPanel(PanelType.NATIONS);
                }
            });

        advantageButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showColopediaPanel(PanelType.NATION_TYPES);
                }
            });

        HeaderRenderer renderer = new HeaderRenderer(nationButton, availabilityLabel,
                                                     advantageButton, colorLabel, playerLabel);
        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new HeaderListener(header, renderer));

        TableColumn advantagesColumn = table.getColumnModel().getColumn(ADVANTAGE_COLUMN);
        if (nationOptions.getNationalAdvantages() == NationOptions.Advantages.SELECTABLE) {
            advantagesColumn.setCellEditor(new AdvantageCellEditor());
        }
        advantagesColumn.setCellRenderer(new AdvantageCellRenderer(nationOptions.getNationalAdvantages()));
        advantagesColumn.setHeaderRenderer(renderer);

        TableColumn availableColumn = table.getColumnModel().getColumn(AVAILABILITY_COLUMN);
        availableColumn.setCellRenderer(new AvailableCellRenderer());
        availableColumn.setCellEditor(new AvailableCellEditor());
        
        TableColumn colorsColumn = table.getColumnModel().getColumn(COLOR_COLUMN);
        ColorCellEditor colorCellEditor = new ColorCellEditor(getCanvas(), this);
        colorsColumn.setCellEditor(colorCellEditor);
        colorsColumn.setCellRenderer(new ColorCellRenderer(true));
        colorCellEditor.setData(getGame().getPlayers(), getMyPlayer());

        TableColumn playerColumn = table.getColumnModel().getColumn(PLAYER_COLUMN);
        playerColumn.setCellEditor(new PlayerCellEditor());
        
        start = new JButton(Messages.message("startGame"));
        gameOptions = new JButton(Messages.message("gameOptions"));
        mapGeneratorOptions = new JButton(Messages.message("mapGeneratorOptions"));
        readyBox = new JCheckBox(Messages.message("iAmReady"));

        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will
            // be able to change the settings as he likes.
            getMyPlayer().setReady(false);
            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        } else {
            readyBox.setSelected(getMyPlayer().isReady());
        }

        chat = new JTextField();
        chatArea = new JTextArea();
        chatScroll = new JScrollPane(chatArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        table.setLayout(new MigLayout("insets 10 10 10 40", "", ""));
        refreshPlayersTable();
        tableScroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.getViewport().setOpaque(false);

        setLayout(new MigLayout("wrap 3", "", ""));

        add(tableScroll, "span 2, grow");
        add(chatScroll, "width 250:, grow");
        add(mapGeneratorOptions, "grow");
        add(gameOptions, "grow");
        add(chat, "grow");
        add(readyBox, "span");
        add(start, "span, split 2, tag ok");
        add(cancel, "tag cancel");

        start.setActionCommand(String.valueOf(START));
        cancel.setActionCommand(String.valueOf(CANCEL));
        readyBox.setActionCommand(String.valueOf(READY));
        gameOptions.setActionCommand(String.valueOf(GAME_OPTIONS));
        mapGeneratorOptions.setActionCommand(String.valueOf(MAP_GENERATOR_OPTIONS));
        chat.setActionCommand(String.valueOf(CHAT));

        enterPressesWhenFocused(start);
        enterPressesWhenFocused(cancel);
        
        start.addActionListener(this);
        cancel.addActionListener(this);
        readyBox.addActionListener(this);
        chat.addActionListener(this);
        gameOptions.addActionListener(this);
        mapGeneratorOptions.addActionListener(this);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        setSize(getPreferredSize());

        chatArea.setText("");

        setEnabled(true);

    }

    public void requestFocus() {
        start.requestFocus();
    }

    /**
     * Updates the map generator options displayed on this panel.
     */
    public void updateMapGeneratorOptions() {
        getClient().getPreGameController().getMapGeneratorOptions()
            .getObject(MapGeneratorOptions.MAP_SIZE);
    }

    /**
     * Updates the game options displayed on this panel.
     */
    public void updateGameOptions() {
        // Nothing yet.
    }

    /**
     * Sets whether or not this component is enabled. It also does this for its
     * children.
     * 
     * @param enabled 'true' if this component and its children should be
     *            enabled, 'false' otherwise.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }

        if (singlePlayerGame && enabled) {
            readyBox.setEnabled(false);
        }

        if (enabled) {
            start.setEnabled(getClient().isAdmin());
        }

        gameOptions.setEnabled(enabled);
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        try {
            switch (Integer.valueOf(command).intValue()) {
            case START:
                // The ready flag was set to false for single player
                // mode in order to allow the player to change
                // whatever he wants.
                if (singlePlayerGame) {
                    getMyPlayer().setReady(true);
                }

                getClient().getPreGameController().requestLaunch();
                break;
            case CANCEL:
                getClient().getConnectController().quitGame(true);
                getCanvas().remove(this);
                getCanvas().showPanel(new NewPanel(getCanvas()));
                break;
            case READY:
                getClient().getPreGameController().setReady(readyBox.isSelected());
                refreshPlayersTable();
                break;
            case CHAT:
                if (chat.getText().trim().length() > 0) {
                    getClient().getPreGameController().chat(chat.getText());
                    displayChat(getMyPlayer().getName(), chat.getText(), false);
                    chat.setText("");
                }
                break;
            case GAME_OPTIONS:
                getCanvas().showFreeColDialog(new GameOptionsDialog(getCanvas(), getClient().isAdmin()));
                break;
            case MAP_GENERATOR_OPTIONS:
                getCanvas().showMapGeneratorOptionsDialog(getClient().isAdmin());
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

    /**
     * Displays a chat message to the user.
     * 
     * @param senderName The name of the player who sent the chat message to the
     *            server.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     */
    public void displayChat(String senderName, String message, boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (private): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }

    /**
     * Refreshes the table that displays the players and the choices that
     * they've made.
     */
    public void refreshPlayersTable() {
        tableModel.update();
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
        JTableHeader header;

        HeaderRenderer renderer;

        HeaderListener(JTableHeader header, HeaderRenderer renderer) {
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


    class NationStateRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(((NationState) value).getName());
            return this;
        }
    }

    class AvailableCellRenderer implements TableCellRenderer {

        /**
         * Returns the component used to render the cell's value.
         * @param table The table whose cell needs to be rendered.
         * @param value The value of the cell being rendered.
         * @param hasFocus Indicates whether or not the cell in question has focus.
         * @param row The row index of the cell that is being rendered.
         * @param column The column index of the cell that is being rendered.
         * @return The component used to render the cell's value.
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return new JLabel(((NationState) value).getName());
        }
    }

    public final class AvailableCellEditor extends AbstractCellEditor implements TableCellEditor {

        private JComboBox aiStateBox = new JComboBox(aiStateNames);
        private JComboBox allStateBox = new JComboBox(allStateNames);
        private JComboBox activeBox;

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            NationType nationType = ((Nation) table.getValueAt(row, StartGamePanel.NATION_COLUMN)).getType();
            if (nationType instanceof EuropeanNationType) {
                activeBox = allStateBox;
            } else {
                activeBox = aiStateBox;
            }
            return activeBox;
        }

        public Object getCellEditorValue() {
            String available = (String) activeBox.getSelectedItem();
            for (int index = 0; index < allStateNames.length; index++) {
                if (allStateNames[index].equals(available)) {
                    return allStates[index];
                }
            }
            return NationState.NOT_AVAILABLE;
        }
    }

    public final class PlayerCellEditor extends AbstractCellEditor implements TableCellEditor {

        private JLabel label = new JLabel(Messages.message("select"));

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            return label;
        }

        public Object getCellEditorValue() {
            return true;
        }

    }

}

/**
 * The TableModel for the players table.
 */
class PlayersTableModel extends AbstractTableModel {

    private List<Nation> nations;

    private Map<Nation, Player> players;

    private Player thisPlayer;

    private final PreGameController preGameController;

    private NationOptions nationOptions;

    private static final String[] columnNames = {
        Messages.message("nation"),
        Messages.message("availability"),
        Messages.message("advantage"),
        Messages.message("color"),
        Messages.message("player")
    };


    /**
     * A standard constructor.
     * 
     * @param pgc The PreGameController to use when updates need to be notified
     *            across the network.
     * @param advantages an <code>Advantages</code> value
     */
    public PlayersTableModel(PreGameController pgc, NationOptions nationOptions, Player owningPlayer) {
        nations = new ArrayList<Nation>();
        players = new HashMap<Nation, Player>();
        for (Nation nation : Specification.getSpecification().getNations()) {
            NationState state = nationOptions.getNations().get(nation);
            if (state != null) {
                nations.add(nation);
                players.put(nation, null);
            }
        }
        thisPlayer = owningPlayer;
        players.put(thisPlayer.getNation(), thisPlayer);
        preGameController = pgc;
        this.nationOptions = nationOptions;
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
     * Returns the Class of the objects in the given column.
     * 
     * @param column The column to return the Class of.
     * @return The Class of the objects in the given column.
     */
    public Class<?> getColumnClass(int column) {
        switch(column) {
        case StartGamePanel.NATION_COLUMN:
            return Nation.class;
        case StartGamePanel.AVAILABILITY_COLUMN:
            return NationOptions.NationState.class;
        case StartGamePanel.ADVANTAGE_COLUMN:
            return NationType.class;
        case StartGamePanel.COLOR_COLUMN:
            return Color.class;
        case StartGamePanel.PLAYER_COLUMN:
            return Player.class;
        }
        return String.class;
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
        return nations.size();
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
            Nation nation = nations.get(row);
            switch (column) {
            case StartGamePanel.NATION_COLUMN:
                return nation;
            case StartGamePanel.AVAILABILITY_COLUMN:
                return nationOptions.getNationState(nation);
            case StartGamePanel.ADVANTAGE_COLUMN:
                if (players.get(nation) == null) {
                    return nation.getType();
                } else {
                    return players.get(nation).getNationType();
                }
            case StartGamePanel.COLOR_COLUMN:
                if (players.get(nation) == null) {
                    return nation.getColor();
                } else {
                    return players.get(nation).getColor();
                }
            case StartGamePanel.PLAYER_COLUMN:
                return players.get(nation);
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
        if ((row >= 0) && (row < nations.size())) {
            Nation nation = nations.get(row);
            boolean ownRow = (thisPlayer == players.get(nation) && !thisPlayer.isReady());
            switch(column) {
            case StartGamePanel.AVAILABILITY_COLUMN:
                return thisPlayer.isAdmin();
            case StartGamePanel.ADVANTAGE_COLUMN:
                return (nation.getType() instanceof EuropeanNationType && ownRow);
            case StartGamePanel.COLOR_COLUMN:
                return (ownRow || thisPlayer.isAdmin());
            case StartGamePanel.PLAYER_COLUMN:
                return (nation.getType() instanceof EuropeanNationType && players.get(nation) == null);
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
        if ((row < getRowCount()) && (column < getColumnCount()) && (row >= 0) && (column >= 0)) {
            // Column 0 can't be updated.

            switch(column) {
            case StartGamePanel.ADVANTAGE_COLUMN:
                preGameController.setNationType((NationType) value);
                break;
            case StartGamePanel.AVAILABILITY_COLUMN:
                preGameController.setAvailable(nations.get(row), (NationState) value);
                break;
            case StartGamePanel.COLOR_COLUMN:
                preGameController.setColor((Color) value);
                break;
            case StartGamePanel.PLAYER_COLUMN:
                Nation nation = nations.get(row);
                preGameController.setNation(nation);
                preGameController.setColor(nation.getColor());
                update();
                break;
            }

            fireTableCellUpdated(row, column);
        }
    }
}