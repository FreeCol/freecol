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

import java.awt.Component;
import java.awt.event.ActionEvent;
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
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.ServerInfo;


/**
 * This panel is used to display the information received from the meta-server.
 */
public final class ServerListPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(ServerListPanel.class.getName());

    private static final int CONNECT = 0, CANCEL = 1;

    private final ConnectController connectController;

    private final JTable table;

    private final ServerListTableModel tableModel;

    private final JButton connect;

    
    /**
     * Creates a panel to display the meta-server.
     * 
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param connectController The controller responsible for creating new
     *     connections.
     */
    public ServerListPanel(FreeColClient freeColClient,
                           ConnectController connectController) {
        super(freeColClient, new MigLayout("", "", ""));

        this.connectController = connectController;

        JButton cancel = Utility.localizedButton("cancel");
        JScrollPane tableScroll;

        setCancelComponent(cancel);

        connect = Utility.localizedButton("connect");

        tableModel = new ServerListTableModel(new ArrayList<ServerInfo>());
        table = new JTable(tableModel);

        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
            @Override
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

        add(tableScroll, "width 400:, height 350:");
        add(connect, "newline 20, split 2");
        add(cancel, "tag cancel");

        setSize(getPreferredSize());
    }


    @Override
    public void requestFocus() {
        connect.requestFocus();
    }

    /**
     * Initializes the data that is displayed in this panel.
     * 
     * @param servers A list of <code>ServerInfo</code>-objects to be
     *            displayed.
     */
    public void initialize(List<ServerInfo> servers) {
        // FIXME: This should be added as a filtering rule:
        // Remove servers with an incorrect version from the list:
        Iterator<ServerInfo> it = servers.iterator();
        while (it.hasNext()) {
            ServerInfo si = it.next();
            if (!si.getVersion().equals(FreeCol.getVersion())) {
                it.remove();
            }
        }

        tableModel.setItems(servers);
        setEnabled(true);
        if (servers.isEmpty()) {
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
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }

        table.setEnabled(enabled);
    }

    /**
     * Refreshes the table.
     */
    public void refreshTable() {
        tableModel.fireTableDataChanged();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        try {
            switch (Integer.parseInt(command)) {
            case CONNECT:
                ServerInfo si = tableModel.getItem(table.getSelectedRow());
                connectController.joinMultiplayerGame(si.getAddress(), si.getPort());
                break;
            case CANCEL:
                getGUI().removeFromCanvas(this);
                getGUI().showNewPanel();
                break;
            default:
                super.actionPerformed(ae);
            }
        } catch (NumberFormatException nfe) {
            logger.warning("Invalid ActionEvent, not a number: " + command);
        }
    }
}

class ServerListTableModel extends AbstractTableModel {

    private static final String[] columnNames = {
        Messages.message("name"),
        Messages.message("host"),
        Messages.message("port"),
        Messages.message("serverListPanel.players"),
        Messages.message("serverListPanel.gameState"),
    };

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
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Returns the name of the specified column.
     * 
     * @return The name of the specified column.
     */
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    /**
     * Returns the amount of rows in this statesTable.
     * 
     * @return The amount of rows in this statesTable.
     */
    @Override
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
    @Override
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
                return Messages.message("serverListPanel.gameState." + Integer.toString(si.getGameState()));
            default:
                return null;
            }
        }
        return null;
    }
}
