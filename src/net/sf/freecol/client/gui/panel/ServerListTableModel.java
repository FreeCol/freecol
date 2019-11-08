/**
 * Copyright (C) 2002-2019ar}   The FreeCol Team
 * <p>
 * This file is part of FreeCol.
 * <p>
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;

import javax.swing.table.AbstractTableModel;
import java.util.List;

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
     * @param items The {@code ArrayList} containing the items.
     */
    public void setItems(List<ServerInfo> items) {
        this.items = items;
    }

    /**
     * Gets the given item.
     *
     * @param row The row-number identifying a {@code ServerInfo}-line.
     * @return The {@code ServerInfo}.
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
        if (0 <= row && 0 <= column
            && row < getRowCount() && column < getColumnCount()) {
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
