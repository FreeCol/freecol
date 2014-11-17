/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;


/**
 * A wrapper interface for a location that can be on a trade route.
 *
 * @see TradeRoute
 */
public interface TradeLocation {

    /**
     * Gets the amount of a given goods type that can be exported from
     * this trade location.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The amount of goods to export.
     */
    public int getExportAmount(GoodsType goodsType);

    /**
     * Gets the amount of a given goods type that can be imported to
     * this trade location.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The amount of goods to import.
     */
    public int getImportAmount(GoodsType goodsType);
}
