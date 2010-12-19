/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import java.util.Comparator;
import java.util.List;


/**
 * Objects implementing the Consumer interface consume Goods. Examples
 * include units that eat food, buildings that convert Goods into
 * other Goods, or buildings that store Goods.
 *
 */
public interface Consumer {

    public static final Comparator<Consumer> COMPARATOR = new Comparator<Consumer>() {
        public int compare(Consumer c1, Consumer c2) {
            return c2.getPriority() - c1.getPriority();
        }
    };

    /**
     * Default consumption priority for buildings. Individual building
     * types may have different priorities.
     */
    public static final int BUILDING_PRIORITY = 500;

    /**
     * The consumption priority of a Colony building a Unit or
     * Building.
     */
    public static final int COLONY_PRIORITY = 800;

    /**
     * Default consumption priority for units. Individual unit types
     * may have different priorities. Slave units, or converts, or
     * petty criminals, for example, might have a lower priority.
     */
    public static final int UNIT_PRIORITY = 1000;

    /**
     * Returns the number of units of the given GoodsType this
     * UnitType consumes per turn (when in a settlement).
     *
     * @return units consumed
     */
    public int getConsumedAmount(GoodsType goodsType);

    /**
     * Returns true if this Consumer consumes the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType);

    /**
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods();

    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority();

}