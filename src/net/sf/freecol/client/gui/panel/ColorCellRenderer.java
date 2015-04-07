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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.freecol.common.model.StringTemplate;


/**
 * A table cell renderer that should be used to display the chosen
 * color in a table.  It is being used in the players table
 * (StartGamePanel).
 */
public class ColorCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * The constructor to use.
     *
     * @param useBorder Indicated whether or not a border should be drawn.
     */
    public ColorCellRenderer(boolean useBorder) {
        if (useBorder) setBorder(Utility.COLOR_CELL_BORDER);
        // This must be done because the background displays the actual color:
        setOpaque(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object color,
        boolean isSelected, boolean hasFocus, int row, int column) {
        final Color myColor = (Color)color;
        setBackground(myColor);
        Utility.localizeToolTip(this, StringTemplate
            .template("info.rgb")
            .addAmount("%red%", myColor.getRed())
            .addAmount("%green%", myColor.getGreen())
            .addAmount("%blue%", myColor.getBlue()));
        return this;
    }
}
