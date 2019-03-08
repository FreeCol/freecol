/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.common.model;


import javax.swing.JLabel;

/**
 * A wrapper interface for a location that can be on a trade route.
 *
 * @see TradeRoute
 */
public interface TradeLocation {

    /**
     * Get the amount of a given goods type at this trade location.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The amount of goods present.
     */
    public int getAvailableGoodsCount(GoodsType goodsType);

    /**
     * Gets the amount of a given goods type that can be exported from
     * this trade location after a given number of turns.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @param turns The number of turns before the goods is required.
     * @return The amount of goods to export.
     */
    public int getExportAmount(GoodsType goodsType, int turns);

    /**
     * Gets the amount of a given goods type that can be imported to
     * this trade location after a given number of turns.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @param turns The number of turns before the goods will arrive.
     * @return The amount of goods to import.
     */
    public int getImportAmount(GoodsType goodsType, int turns);

    /**
     * Function for returning the name of a TradeLocation
     *
     * @param tradeLocation The {@code TradeLocation} to return the name.
     * @return The name.
     */
    public String getLocationName(TradeLocation tradeLocation);

    /**
     * Get the name of this instance as a JLabel.
     *
     * @return The {@code JLabel} with the result of
     *     {@link #getLocationName(TradeLocation)}
     */
    public default JLabel getNameAsJlabel() {
        return new JLabel(getLocationName(this));
    }

    /**
     * Can a TradeLocation be set as the input location on a
     * TradeRouteInputPanel
     *
     * @return True if possible, false by default.
     */
    public default boolean canBeInput() {
        return false;
    }
}
