
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;

/**
* The panel where you choose your nation and color and connected players are shown.
*/
public final class StartGamePanel extends JPanel implements ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private static final int    START = 0,
                                CANCEL = 1,
                                MAPSIZE = 2,
                                READY = 3,
                                CHAT = 4;

    private final String[]  mapSizes = {"Small", "Medium", "Large", "Huge"};
    private final String[]  colors = {"Black", "Blue", "Cyan", "Gray", "Green", "Magenta",
                                        "Orange", "Pink", "Red", "White", "Yellow"};

    private final Canvas        parent;
    private final FreeColClient freeColClient;
    private Game                game;
    private Player              thisPlayer;
    private boolean             singlePlayerGame;

    private final JComboBox     mapSize;
    private final JCheckBox     readyBox;



    private final JTextField        chat;
    private final JTextArea         chatArea;
    private final JPanel            optionsPanel,
                                    chatPanel;
    private final JTable            table;
    private final PlayersTableModel tableModel;

    private JButton start;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public StartGamePanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        JButton     cancel = new JButton("Cancel");
        JLabel      mapSizeLabel = new JLabel("Map Size");
        JScrollPane chatScroll,
                    tableScroll;

        start = new JButton("Start Game");

        optionsPanel = new JPanel();
        readyBox = new JCheckBox("I'm Ready");
        mapSize = new JComboBox(mapSizes);

        chatPanel = new JPanel();
        chat = new JTextField();
        chatArea = new JTextArea();
        chatScroll = new JScrollPane(chatArea);
        tableModel = new PlayersTableModel(freeColClient.getPreGameController());
        table = new JTable(tableModel);

        TableColumn nameColumn = table.getColumnModel().getColumn(0),
                    nationsColumn = table.getColumnModel().getColumn(1),
                    colorsColumn = table.getColumnModel().getColumn(2);
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();

        dtcr.setOpaque(false);
        nameColumn.setCellRenderer(dtcr);
        nationsColumn.setCellEditor(new NationCellEditor());
        nationsColumn.setCellRenderer(new NationCellRenderer());
        colorsColumn.setCellEditor(new ColorCellEditor(parent, this));
        colorsColumn.setCellRenderer(new ColorCellRenderer(true));

        table.setRowHeight(22);
        table.setCellSelectionEnabled(false);

        tableScroll = new JScrollPane(table);
        table.addNotify();
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getColumnHeader().setOpaque(false);

        mapSize.setSize(65, 20);
        mapSizeLabel.setSize(60, 20);
        optionsPanel.setSize(150, 380);
        start.setSize(110, 20);
        cancel.setSize(80, 20);
        readyBox.setSize(90, 20);
        chat.setSize(240, 20);
        chatScroll.setSize(240, 110);
        tableScroll.setSize(240, 170);
        chatPanel.setSize(260, 160);

        mapSize.setLocation(75, 20);
        mapSizeLabel.setLocation(10, 20);
        optionsPanel.setLocation(260, 5);
        start.setLocation(15, 370);
        cancel.setLocation(155, 370);
        readyBox.setLocation(90, 335);
        chat.setLocation(10, 130);
        chatScroll.setLocation(10, 10);
        tableScroll.setLocation(10, 10);
        chatPanel.setLocation(0, 180);

        setLayout(null);
        optionsPanel.setLayout(null);
        chatPanel.setLayout(null);

        mapSize.setActionCommand(String.valueOf(MAPSIZE));
        start.setActionCommand(String.valueOf(START));
        cancel.setActionCommand(String.valueOf(CANCEL));
        readyBox.setActionCommand(String.valueOf(READY));

        chat.setActionCommand(String.valueOf(CHAT));

        mapSize.addActionListener(this);
        start.addActionListener(this);
        cancel.addActionListener(this);
        readyBox.addActionListener(this);

        chat.addActionListener(this);

        // if I'm not an admin
        // start.setEnabled(false);

        chatArea.setEditable(false);

        // Disable while not implemented.
        mapSizeLabel.setEnabled(false);
        mapSize.setEnabled(false);

        optionsPanel.add(mapSize);
        optionsPanel.add(mapSizeLabel);
        add(optionsPanel);
        add(start);
        add(cancel);
        add(readyBox);
        add(tableScroll);

        chatPanel.add(chat);
        chatPanel.add(chatScroll);
        add(chatPanel);

        try {
            BevelBorder border1 = new BevelBorder(BevelBorder.RAISED);
            setBorder(border1);
            TitledBorder border2 = new TitledBorder("Options");
            optionsPanel.setBorder(border2);
        } catch(Exception e) {}

        setSize(420, 400);
    }



    public void requestFocus() {
        start.requestFocus();
    }


    /**
    * Initializes the data that is displayed in this panel.
    *
    * @param singlePlayerMode 'true' if the user wants to start a single player game,
    *        'false' otherwise.
    */
    public void initialize(boolean singlePlayerGame) {
        this.singlePlayerGame = singlePlayerGame;
        game = freeColClient.getGame();
        thisPlayer = freeColClient.getMyPlayer();

        tableModel.setData(game.getPlayers(), thisPlayer);

        ((NationCellRenderer)table.getColumnModel().getColumn(1).getCellRenderer())
                .setPlayers(game.getPlayers());

        ((ColorCellEditor)table.getColumnModel().getColumn(2).getCellEditor())
                .setPlayers(game.getPlayers());

        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will be able to change the
            // settings as he likes.
            thisPlayer.setReady(false);

            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        }
        else {
            readyBox.setSelected(thisPlayer.isReady());
        }

        chatArea.setText("");

        setEnabled(true);
    }


    /**
    * Sets whether or not this component is enabled. It also does this for
    * its children.
    * @param enabled 'true' if this component and its children should be
    * enabled, 'false' otherwise.
    */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }

        components = optionsPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            //components[i].setEnabled(enabled);

            // Disable while not implemented.
            components[i].setEnabled(false);
        }

        components = chatPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }

        if (singlePlayerGame && enabled) {
            readyBox.setEnabled(false);
        }

        table.setEnabled(enabled);
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        try {
            switch (Integer.valueOf(command).intValue()) {
                case START:
                    // The ready flag was set to false for single player mode in order
                    // to allow the player to change whatever he wants.
                    if (singlePlayerGame) {
                        thisPlayer.setReady(true);
                    }

                    freeColClient.getPreGameController().requestLaunch();
                    break;
                case CANCEL:
                    freeColClient.getConnectController().quitGame(true);
                    parent.remove(this);
                    parent.showMainPanel();
                    break;
                case MAPSIZE:
                    // TODO
                    break;
                case READY:
                    freeColClient.getPreGameController().setReady(readyBox.isSelected());
                    refreshPlayersTable();
                    break;
                case CHAT:
                    if (chat.getText().trim().length() > 0) {
                        freeColClient.getPreGameController().chat(chat.getText());
                        displayChat(freeColClient.getMyPlayer().getName(),
                                chat.getText(), false);
                        chat.setText("");
                    }
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }


    /**
    * Displays a chat message to the user.
    *
    * @param senderName The name of the player who sent the chat message to
    *                   the server.
    * @param message The chat message.
    * @param privateChat 'true' if the message is a private one, 'false' otherwise.
    */
    public void displayChat(String senderName, String message, boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (private): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }

    /**
    * Refreshes the table that displays the players and the choices that they've made.
    */
    public void refreshPlayersTable() {
        tableModel.fireTableDataChanged();
    }
}


/**
* The TableModel for the players table.
*/
class PlayersTableModel extends AbstractTableModel {
    private Vector players;
    private Player thisPlayer;
    private final PreGameController preGameController;

    private static final String[] columnNames = {"Player", "Nation", "Color"};

    /**
    * A standard constructor.
    *
    * @param pgc The PreGameController to use when updates need to be notified across the
    * network.
    */
    public PlayersTableModel(PreGameController pgc) {
        players = new Vector();
        thisPlayer = null;
        preGameController = pgc;
    }

    /**
    * Gives this table model the data that is being used in the table.
    * This method should only be called to initialize the data set. To modify
    * or extend the data set use other methods.
    *
    * @param myPlayers The players to use in the table.
    * @param owningPlayer The player running the client that is displaying the table.
    */
    public void setData(Vector myPlayers, Player owningPlayer) {
        players = myPlayers;
        thisPlayer = owningPlayer;
    }

    /**
    * Returns the Class of the objects in the given column.
    * @param column The column to return the Class of.
    * @return The Class of the objects in the given column.
    */
    public Class getColumnClass(int column) {
        return getValueAt(0, column).getClass();
    }

    /**
    * Returns the amount of columns in this statesTable.
    * @return The amount of columns in this statesTable.
    */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
    * Returns the name of the specified column.
    * @return The name of the specified column.
    */
    public String getColumnName(int column) {
        return columnNames[column];
    }

    /**
    * Returns the amount of rows in this statesTable.
    * @return The amount of rows in this statesTable.
    */
    public int getRowCount() {
        return players.size();
    }

    /**
    * Returns the value at the requested location.
    * @param row The requested row.
    * @param column The requested column.
    * @return The value at the requested location.
    */
    public Object getValueAt(int row, int column) {
        if ((row < getRowCount()) && (column < getColumnCount())
                && (row >= 0) && (column >= 0)) {
            Player player = (Player)players.get(row);
            switch (column) {
                case 0:
                    return player.getName();
                case 1:
                    return player.getNationAsString();
                default:
                    return player.getColor();
            }
        }
        return null;
    }

    /**
    * Returns 'true' if the specified cell is editable, 'false' otherwise.
    * @param row The specified row.
    * @param column The specified column.
    * @return 'true' if the specified cell is editable, 'false' otherwise.
    */
    public boolean isCellEditable(int row, int column) {
        if ((column > 0) && (column < columnNames.length)
                && (players.size() > 0) && (row >= 0)
                && thisPlayer.getName().equals(((Player)players.get(row)).getName())
                && !thisPlayer.isReady()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
    * Sets the value at the specified location.
    * @param value The new value.
    * @param row The specified row.
    * @param column The specified column.
    */
    public void setValueAt(Object value, int row, int column) {
        if ((row < getRowCount()) && (column < getColumnCount())
                && (row >= 0) && (column >= 0)) {
            // Column 0 can't be updated.

            if (column == 1) {
                preGameController.setNation(((String)value).toLowerCase());
            }
            else if (column == 2) {
                preGameController.setColor((Color)value);
            }

            fireTableCellUpdated(row, column);
        }
    }
}
