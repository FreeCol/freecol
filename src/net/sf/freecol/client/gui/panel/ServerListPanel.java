/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.ServerInfo;


/**
 * This panel is used to display the information received from the meta-server.
 */
public final class ServerListPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(ServerListPanel.class.getName());

    private static final int CONNECT = 0, CANCEL = 1;

    private final ConnectController connectController;

    private final JTable table;

    private final ServerListTableModel tableModel;

    private String username;

    private JButton connect;

    
    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param connectController The controller responsible for creating new
     *            connections.
     */
    public ServerListPanel(FreeColClient freeColClient, GUI gui, ConnectController connectController) {
        super(freeColClient, gui);
        this.connectController = connectController;

        JButton cancel = new JButton("Cancel");
        JScrollPane tableScroll;

        setCancelComponent(cancel);

        connect = new JButton(Messages.message("connect"));

        tableModel = new ServerListTableModel(new ArrayList<ServerInfo>());
        table = new JTable(tableModel);

        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object o, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                setOpaque(isSelected);
                return super.getTableCellRendererComponent(t, o, isSelected, hasFocus, row, column);
            }
        };
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
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

        connect.setActionCommand(String.valueOf(CONNECT));
        connect.addActionListener(this);

        cancel.setActionCommand(String.valueOf(CANCEL));
        cancel.addActionListener(this);

        setLayout(new MigLayout("", "", ""));
        add(tableScroll, "width 400:, height 350:");
        add(connect, "newline 20, split 2");
        add(cancel, "tag cancel");

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        connect.requestFocus();
    }

    /**
     * Initializes the data that is displayed in this panel.
     * 
     * @param username The username to be used when connecting to a server.
     * @param arrayList A list of <code>ServerInfo</code>-objects to be
     *            displayed.
     */
    public void initialize(String username, ArrayList<ServerInfo> arrayList) {
        this.username = username;

        // TODO: This should be added as a filtering rule:
        // Remove servers with an incorrect version from the list:
        Iterator<ServerInfo> it = arrayList.iterator();
        while (it.hasNext()) {
            ServerInfo si = it.next();
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

        table.setEnabled(enabled);
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
            case CONNECT:
                ServerInfo si = tableModel.getItem(table.getSelectedRow());
                connectController.joinMultiplayerGame(username, si.getAddress(), si.getPort());
                break;
            case CANCEL:
                getCanvas().remove(this);
                getCanvas().showNewPanel();
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
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

    private static final String[] columnNames = { Messages.message("name"), Messages.message("host"),
            Messages.message("port"), Messages.message("players"), Messages.message("gameState"), };

    private List<ServerInfo> items;


    public ServerListTableModel(List<ServerInfo> items) {
        this.items = items;
    }

    /**
     * Sets the items that should be contained by this model.
     * 
     * @param items The <code>ArrayList</code> containing the items.
     */
    public void setItems(List<ServerInfo> items) {
        this.items = items;
    }

    /**
     * Gets the given item.
     * 
     * @param row The row-number identifying a <code>ServerInfo</code>-line.
     * @return The <code>ServerInfo</code>.
     */
    public ServerInfo getItem(int row) {
        return items.get(row);
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
        return items.size();
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
            ServerInfo si = items.get(row);
            switch (column) {
            case 0:
                return si.getName();
            case 1:
                return si.getAddress();
            case 2:
                return Integer.toString(si.getPort());
            case 3:
                return Integer.toString(si.getCurrentlyPlaying()) + "/"
                        + Integer.toString(si.getCurrentlyPlaying() + si.getSlotsAvailable());
            case 4:
                return Messages.message("gameState." + Integer.toString(si.getGameState()));
            default:
                return null;
            }
        }
        return null;
    }
}
