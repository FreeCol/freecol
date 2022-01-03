/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;


public interface DropTarget {

    /**
     * Can the given goods be dropped on this target?
     *
     * @param goods The {@code Goods} to check.
     * @return True if the goods are acceptable.
     */
    public default boolean accepts(Goods goods) {
        return false;
    }

    /**
     * Can the given type of goods be dropped on this target?
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return True if the goods type is acceptable.
     */
    public default boolean accepts(GoodsType goodsType) {
        return false;
    }

    /**
     * Can the given unit be dropped on this target?
     *
     * @param unit The {@code Unit} to check.
     * @return True if the unit is acceptable.
     */
    public default boolean accepts(Unit unit) {
        return false;
    }

    /**
     * Adds a component to this container and makes sure that the unit or
     * goods that the component represents gets modified so that it is on
     * board the currently selected carrier.
     *
     * @param comp The component to add to this container.
     * @param editState Must be set to 'true' if the state of the
     *     component that is added (which should be a dropped
     *     component representing a unit or goods) should be changed
     *     so that the underlying unit or goods are on board the
     *     currently selected carrier.
     * @return The component argument on success, null on failure.
     */
    public Component add(Component comp, boolean editState);

    /**
     * Get a suggested amount of goods to add, used when partial
     * amounts are selected.
     *
     * @param goodsType The {@code GoodsType} proposed to add.
     * @return A good amount of goods to add.
     */
    public default int suggested(GoodsType goodsType) {
        return -1; // Not applicable
    }
}
