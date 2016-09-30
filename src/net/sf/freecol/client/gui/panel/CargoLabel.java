/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import net.sf.freecol.common.model.Unit;

import java.awt.Component;

/**
 * An interface used for updating a {@code CargoPanel} to add or remove a {@code Component}
 */
public interface CargoLabel {

    /**
     * The {@code Component} to add to a {@code CargoPanel}.
     *
     * @param comp
     * @param carrier
     * @param cargoPanel
     * @return
     */
    public Component addCargo(Component comp, Unit carrier, CargoPanel cargoPanel);


    /**
     * The {@code Component} to remove from a {@code CargoPanel}.
     *
     * @param comp
     * @param cargoPanel
     * @return
     */
    public default void removeCargo(Component comp, CargoPanel cargoPanel) {
        cargoPanel.update();
    }

}
