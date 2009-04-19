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
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.NationOptions.Advantages;

/**
* A table cell renderer that should be used to display the chosen nation in a table.
* It is being used in the players table (StartGamePanel).
*/
public final class AdvantageCellRenderer implements TableCellRenderer {


    private static Vector<EuropeanNationType> europeans = 
        new Vector<EuropeanNationType>(FreeCol.getSpecification().getEuropeanNationTypes());
    private static final JComboBox standardNationsComboBox = new JComboBox(europeans);

    private List<Player> players;
    private Player thisPlayer;
    private Advantages advantages;

    /**
    * The default constructor.
    */
    public AdvantageCellRenderer() {
    }


    /**
    * Gives this table model the data that is being used in the table.
    *
    * @param players The players that should be rendered in the table.
    * @param owningPlayer The player running the client that is displaying the table.
    * @param advantages Indicates whether advantages can be selected.
    */
    public void setData(List<Player> players, Player owningPlayer, Advantages advantages) {
        this.players = players;
        thisPlayer = owningPlayer;
        this.advantages = advantages;
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
        component = standardNationsComboBox;
        if (player == thisPlayer) {
            switch(advantages) {
            case FIXED:
                component = new JLabel(player.getNationType().getName());
                break;
            case SELECTABLE:
                component = standardNationsComboBox;
                ((JComboBox) component).setSelectedItem(player.getNationType());
                break;
            case NONE:
            default:
                component = new JLabel(Messages.message("model.nationType.none.name"));
                break;
            }
        } else if (player.isEuropean()) {
            component = new JLabel(player.getNationType().getName());
        } else {
            component = new JLabel();
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
