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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.generator.MapGeneratorOptions;
import cz.autel.dmi.HIGLayout;

/**
 * The panel where you choose your nation and color and connected players are
 * shown.
 */
public final class StartGamePanel extends FreeColPanel implements ActionListener {




    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private static final int START = 0, CANCEL = 1,
    // MAPSIZE = 2,
            READY = 3, CHAT = 4, GAME_OPTIONS = 5, MAP_GENERATOR_OPTIONS = 6;


    public static final int NAME_COLUMN = 0, NATION_COLUMN = 1, ADVANTAGE_COLUMN = 2, COLOR_COLUMN = 3;

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private Game game;

    private Player thisPlayer;

    private boolean singlePlayerGame;

    private final JCheckBox readyBox;

    private final JTextField chat;

    private final JTextArea chatArea;

    private final JTable table;

    private final PlayersTableModel tableModel;

    private JButton start;

    private JButton gameOptions;

    private JButton mapGeneratorOptions;

    private int advantages;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client
     */
    public StartGamePanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        JButton cancel = new JButton(Messages.message("cancel"));

        JScrollPane chatScroll, tableScroll;

        setCancelComponent(cancel);

        start = new JButton(Messages.message("startGame"));
        gameOptions = new JButton(Messages.message("gameOptions"));
        mapGeneratorOptions = new JButton(Messages.message("mapGeneratorOptions"));
        readyBox = new JCheckBox(Messages.message("iAmReady"));

        chat = new JTextField();
        chatArea = new JTextArea();
        chatScroll = new JScrollPane(chatArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableModel = new PlayersTableModel(freeColClient, freeColClient.getPreGameController());
        table = new JTable(tableModel);

        TableColumn nameColumn = table.getColumnModel().getColumn(NAME_COLUMN),
            advantageColumn = table.getColumnModel().getColumn(ADVANTAGE_COLUMN),
            colorsColumn = table.getColumnModel().getColumn(COLOR_COLUMN);
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();

        dtcr.setOpaque(false);
        nameColumn.setCellRenderer(dtcr);
        colorsColumn.setCellEditor(new ColorCellEditor(parent, this));
        colorsColumn.setCellRenderer(new ColorCellRenderer(true));

        table.setRowHeight(22);
        table.setCellSelectionEnabled(false);

        tableScroll = new JScrollPane(table);
        table.addNotify();
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getColumnHeader().setOpaque(false);

        int[] widths = { 400, margin, 300 };
        int[] heights = { 200, margin, 0, margin, 0, margin, 0, margin, 0 };
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        add(tableScroll, higConst.rc(row, leftColumn));
        add(chatScroll, higConst.rcwh(row, rightColumn, 1, 3));
        row += 2;

        add(mapGeneratorOptions, higConst.rc(row, leftColumn));
        row += 2;

        add(gameOptions, higConst.rc(row, leftColumn));
        add(chat, higConst.rc(row, rightColumn));
        row += 2;

        add(readyBox, higConst.rc(row, leftColumn));
        row += 2;

        add(start, higConst.rc(row, leftColumn, "r"));
        add(cancel, higConst.rc(row, rightColumn, "l"));

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

    }

    public void requestFocus() {
        start.requestFocus();
    }

    /**
     * Initializes the data that is displayed in this panel.
     * 
     * @param singlePlayerGame <code>true</code> if the user wants to start a
     *            single player game, <code>false</code> otherwise.
     * @param additionalNations whether to allow additional nations
     * @param advantages the type of advantages used
     */
    public void initialize(boolean singlePlayerGame, boolean additionalNations, int advantages) {
        this.singlePlayerGame = singlePlayerGame;
        game = freeColClient.getGame();
        thisPlayer = freeColClient.getMyPlayer();

        this.advantages = advantages;

        Nation[] nations;
        if (additionalNations) {
            nations = FreeCol.getSpecification().getEuropeanNations().toArray(new Nation[] {});
        } else {
            nations = FreeCol.getSpecification().getClassicNations().toArray(new Nation[] {});
        }

        tableModel.setData(game.getPlayers(), thisPlayer, advantages);

        TableColumn nationsColumn = table.getColumnModel().getColumn(NATION_COLUMN);
        nationsColumn.setCellEditor(new DefaultCellEditor(new JComboBox(nations)));
        nationsColumn.setCellRenderer(new NationCellRenderer(nations));
        ((NationCellRenderer) table.getColumnModel().getColumn(NATION_COLUMN).getCellRenderer())
        .setData(game.getPlayers(), thisPlayer);

        TableColumn advantagesColumn = table.getColumnModel().getColumn(ADVANTAGE_COLUMN);
        if (advantages == AdvantageCellRenderer.SELECTABLE) {
            advantagesColumn.setCellEditor(new AdvantageCellEditor());
        }
        advantagesColumn.setCellRenderer(new AdvantageCellRenderer());
        ((AdvantageCellRenderer) table.getColumnModel().getColumn(ADVANTAGE_COLUMN).getCellRenderer())
        .setData(game.getPlayers(), thisPlayer, advantages);
        
        ((ColorCellEditor) table.getColumnModel().getColumn(COLOR_COLUMN).getCellEditor())
        .setData(game.getPlayers(), thisPlayer);
        
        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will
            // be able to change the settings as he likes.
            thisPlayer.setReady(false);

            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        } else {
            readyBox.setSelected(thisPlayer.isReady());
        }

        chatArea.setText("");

        setEnabled(true);

    }

    /**
     * Updates the map generator options displayed on this panel.
     */
    public void updateMapGeneratorOptions() {
        freeColClient.getPreGameController().getMapGeneratorOptions().getObject(MapGeneratorOptions.MAP_SIZE);
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

        table.setEnabled(enabled);

        if (enabled) {
            start.setEnabled(freeColClient.isAdmin());
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
                // The ready flag was set to false for single player mode in
                // order
                // to allow the player to change whatever he wants.
                if (singlePlayerGame) {
                    thisPlayer.setReady(true);
                }

                freeColClient.getPreGameController().requestLaunch();
                break;
            case CANCEL:
                freeColClient.getConnectController().quitGame(true);
                parent.remove(this);
                parent.showPanel(new NewPanel(parent));
                break;
            case READY:
                freeColClient.getPreGameController().setReady(readyBox.isSelected());
                refreshPlayersTable();
                break;
            case CHAT:
                if (chat.getText().trim().length() > 0) {
                    freeColClient.getPreGameController().chat(chat.getText());
                    displayChat(freeColClient.getMyPlayer().getName(), chat.getText(), false);
                    chat.setText("");
                }
                break;
            case GAME_OPTIONS:
                parent.showGameOptionsDialog(freeColClient.isAdmin());
                break;
            case MAP_GENERATOR_OPTIONS:
                parent.showMapGeneratorOptionsDialog(freeColClient.isAdmin());
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
        tableModel.fireTableDataChanged();
    }
}

/**
 * The TableModel for the players table.
 */
class PlayersTableModel extends AbstractTableModel {

    @SuppressWarnings("unused")
    private FreeColClient freeColClient;

    private List<Player> players;

    private Player thisPlayer;

    private final PreGameController preGameController;

    private int advantages;

    private static final String[] columnNames = {
        Messages.message("player"),
        Messages.message("nation"),
        Messages.message("advantage"),
        Messages.message("color")
    };


    /**
     * A standard constructor.
     * 
     * @param freeColClient The main controller object for the client
     * @param pgc The PreGameController to use when updates need to be notified
     *            across the network.
     */
    public PlayersTableModel(FreeColClient freeColClient, PreGameController pgc) {
        this.freeColClient = freeColClient;
        players = new ArrayList<Player>();
        thisPlayer = null;
        preGameController = pgc;
    }

    /**
     * Gives this table model the data that is being used in the table. This
     * method should only be called to initialize the data set. To modify or
     * extend the data set use other methods.
     * 
     * @param myPlayers The players to use in the table.
     * @param owningPlayer The player running the client that is displaying the
     *            table.
     */
    public void setData(List<Player> myPlayers, Player owningPlayer, int advantages) {
        players = myPlayers;
        thisPlayer = owningPlayer;
        this.advantages = advantages;
    }

    /**
     * Returns the Class of the objects in the given column.
     * 
     * @param column The column to return the Class of.
     * @return The Class of the objects in the given column.
     */
    public Class<?> getColumnClass(int column) {
        return getValueAt(0, column).getClass();
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
        return players.size();
    }

    private Player getPlayer(int i) {
        if (i == 0) {
            return thisPlayer;
        } else if (players.get(i) == thisPlayer) {
            return players.get(0);
        } else {
            return players.get(i);
        }
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
            Player player = getPlayer(row);
            switch (column) {
            case StartGamePanel.NAME_COLUMN:
                return player.getName();
            case StartGamePanel.NATION_COLUMN:
                return new Integer(player.getIndex());
            case StartGamePanel.COLOR_COLUMN:
                return player.getColor();
            case StartGamePanel.ADVANTAGE_COLUMN:
                return player.getNationType();
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
        if ((column > 0) && (column < columnNames.length) && (players.size() > 0) && (row >= 0)
                && thisPlayer == getPlayer(row) && !thisPlayer.isReady()) {
            return true;
        } else {
            return false;
        }
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

            if (column == StartGamePanel.NATION_COLUMN) {
                //Nation nation = FreeCol.getSpecification().getNation(((Integer) value).intValue());
                Nation nation = (Nation) value;
                preGameController.setNation(nation);
                preGameController.setColor(nation.getColor());
                fireTableCellUpdated(row, StartGamePanel.COLOR_COLUMN);
                if (advantages == AdvantageCellRenderer.FIXED) {
                    preGameController.setNationType(nation.getType());
                    fireTableCellUpdated(row, StartGamePanel.ADVANTAGE_COLUMN);
                }
            } else if (column == StartGamePanel.COLOR_COLUMN) {
                preGameController.setColor((Color) value);
            } else if (column == StartGamePanel.ADVANTAGE_COLUMN) {
                preGameController.setNationType((NationType) value);
            }

            fireTableCellUpdated(row, column);
        }
    }
}
