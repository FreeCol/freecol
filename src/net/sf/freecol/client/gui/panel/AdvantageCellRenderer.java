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

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.NationOptions.Advantages;

/**
* A table cell renderer that should be used to display the chosen nation in a table.
* It is being used in the players table (StartGamePanel).
*/
public final class AdvantageCellRenderer implements TableCellRenderer {

    private static Vector<EuropeanNationType> europeans = 
        new Vector<EuropeanNationType>(Specification.getSpecification().getEuropeanNationTypes());

    private Advantages advantages;

    /**
    * The default constructor.
    */
    public AdvantageCellRenderer(Advantages advantages) {
        this.advantages = advantages;
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

        Player player = (Player) table.getValueAt(row, PlayersTable.PLAYER_COLUMN);
        NationType nationType = ((Nation) table.getValueAt(row, PlayersTable.NATION_COLUMN)).getType();

        JLabel label;
        switch(advantages) {
        case FIXED:
            label = new JLabel(Messages.message(nationType.getNameKey()));
            break;
        case SELECTABLE:
            if (player == null) {
                return new JLabel(Messages.message(nationType.getNameKey()));
            } else {
                return new JLabel(Messages.message(player.getNationType().getNameKey()));
            }
        case NONE:
        default:
            label = new JLabel(Messages.message("model.nationType.none.name"));
            break;
        }
        if (player != null && player.isReady()) {
            label.setForeground(Color.GRAY);
        } else {
            label.setForeground(table.getForeground());
        }
        label.setBackground(table.getBackground());

        return label;
    }
}
