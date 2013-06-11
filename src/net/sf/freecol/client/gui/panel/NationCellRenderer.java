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
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;


/**
 * A table cell renderer that should be used to display the chosen
 * nation in a table.  It is being used in the players table
 * (StartGamePanel).
 */
public final class NationCellRenderer implements TableCellRenderer {


    private final Nation[] nations;
    private final JComboBox comboBox;
    private List<Player> players;
    private Player thisPlayer;


    /**
     * The default constructor.
     *
     * @param nations array of <code>Nation</code>
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public NationCellRenderer(Nation[] nations) {
        this.nations = nations;
        this.comboBox = new JComboBox(nations);
    }


    /**
     * Gives this table model the data that is being used in the table.
     *
     * @param players The players that should be rendered in the table.
     * @param owningPlayer The player running the client that is displaying the table.
     */
    public void setData(List<Player> players, Player owningPlayer) {
        this.players = players;
        thisPlayer = owningPlayer;
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
     * Returns the component used to render the cell's value.
     * @param table The table whose cell needs to be rendered.
     * @param value The value of the cell being rendered.
     * @param hasFocus Indicates whether or not the cell in question has focus.
     * @param row The row index of the cell that is being rendered.
     * @param column The column index of the cell that is being rendered.
     * @return The component used to render the cell's value.
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Player player = getPlayer(row);

        Component component;
        if (player == thisPlayer) {
            for (int index = 0; index < nations.length; index++) {
                if (nations[index].getId().equals(player.getNationId())) {
                    comboBox.setSelectedIndex(index);
                    break;
                }
            }
            component = comboBox;
        } else {
            component = new JLabel(Messages.message(player.getNationName()));
        }

        if (player.isReady()) {
            component.setForeground(Color.GRAY);
        } else {
            component.setForeground(table.getForeground());
        }
        component.setBackground(table.getBackground());

        return component;
    }
}
