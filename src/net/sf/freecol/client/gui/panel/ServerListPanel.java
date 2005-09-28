
package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.ServerInfo;


/**
* This panel is used to display the information received from
* the meta-server. 
*/
public final class ServerListPanel extends FreeColPanel implements ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(ServerListPanel.class.getName());

    private static final int    CONNECT = 0,
                                CANCEL = 1;

    private final Canvas        parent;
    private final FreeColClient freeColClient;
    private final ConnectController connectController;

    private final JTable            table;
    private final ServerListTableModel tableModel;
    
    private String username;

    private JButton connect;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public ServerListPanel(Canvas parent, FreeColClient freeColClient, ConnectController connectController) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.connectController = connectController;

        JButton     cancel = new JButton("Cancel");
        JScrollPane tableScroll;

        setCancelComponent(cancel);

        connect = new JButton("Connect");

        tableModel = new ServerListTableModel(new ArrayList());
        table = new JTable(tableModel);

        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object o, boolean isSelected, boolean hasFocus, int row, int column) {
                setOpaque(isSelected);
                return super.getTableCellRendererComponent(t, o, isSelected, hasFocus, row, column);
            }
        };
        for (int i=0; i<table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(dtcr);
        }

        table.setRowHeight(22);

        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableScroll = new JScrollPane(table);
        table.addNotify();
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getColumnHeader().setOpaque(false);

        connect.setSize(110, 20);
        cancel.setSize(80, 20);
        tableScroll.setSize(400, 350);

        connect.setLocation(15, 370);
        cancel.setLocation(155, 370);
        tableScroll.setLocation(10, 10);

        setLayout(null);

        connect.setActionCommand(String.valueOf(CONNECT));
        cancel.setActionCommand(String.valueOf(CANCEL));

        connect.addActionListener(this);
        cancel.addActionListener(this);

        add(connect);
        add(cancel);
        add(tableScroll);

        setSize(420, 400);
    }



    public void requestFocus() {
        connect.requestFocus();
    }


    /**
    * Initializes the data that is displayed in this panel.
    */
    public void initialize(String username, ArrayList arrayList) {
        this.username = username;
        
        // TODO: This should be added as a filtering rule:
        // Remove servers with an incorrect version from the list:
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ServerInfo si = (ServerInfo) it.next();
            if (!si.getVersion().equals(FreeCol.getVersion())) {
                it.remove();
            }
        }

        tableModel.setItems(arrayList);
        setEnabled(true);
        if (arrayList.size() == 0) {
            connect.setEnabled(false);
        } else {
            table.setRowSelectionInterval(0, 0);
        }
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
                case CONNECT:
                    ServerInfo si = tableModel.getItem(table.getSelectedRow());
                    connectController.joinMultiplayerGame(username, si.getAddress(), si.getPort());
                    break;
                case CANCEL:
                    parent.remove(this);
                    //parent.showMainPanel();
                    parent.showNewGamePanel();
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
    * Refreshes the table.
    */
    public void refreshTable() {
        tableModel.fireTableDataChanged();
    }
}




class ServerListTableModel extends AbstractTableModel {

    private static final String[] columnNames = {"Name", "Address", "Port", "Players", "Game state"};

    private ArrayList items;

    public ServerListTableModel(ArrayList items) {
        this.items = items;
    }


    /**
    * Sets the items that should be contained by this model.
    * @param items The <code>ArrayList</code> containing the items.
    */
    public void setItems(ArrayList items) {
        this.items = items;
    }
    
    
    /**
    * Gets the given item.
    */
    public ServerInfo getItem(int row) {
        return (ServerInfo) items.get(row);
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
        return items.size();
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
            ServerInfo si = (ServerInfo) items.get(row);
            switch (column) {
                case 0:
                    return si.getName();
                case 1:
                    return si.getAddress();
                case 2:
                    return Integer.toString(si.getPort());
                case 3:
                    return Integer.toString(si.getCurrentlyPlaying()) + "/" + Integer.toString(si.getCurrentlyPlaying()+si.getSlotsAvailable());
                case 4:
                    return Messages.message("gameState." + Integer.toString(si.getGameState()));
                default:
                    return null;
            }
        }
        return null;
    }
}
