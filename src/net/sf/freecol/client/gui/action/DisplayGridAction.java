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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;


/**
 * An action to toggle the display of the map grid.
 */
public class DisplayGridAction extends SelectableAction {

    public static final String id = "displayGridAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public DisplayGridAction(FreeColClient freeColClient) {
        super(freeColClient, id, ClientOptions.DISPLAY_GRID);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        setSelected(((JCheckBoxMenuItem)ae.getSource()).isSelected());
        setOption(isSelected());
        getGUI().refresh();
    }
}
