/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

/**
 * This interface marks the locations where a <code>Unit</code> can work.
 */
public interface WorkLocation extends Location {

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public int getProductionOf(GoodsType goodsType);

    /**
     * Returns the <code>Colony</code> this <code>WorkLocation</code> is
     * located in.
     *
     * This method always returns a colony != null (in contrast to
     * Location.getColony(), which might return null).
     *
     * @return The <code>Colony</code> this <code>WorkLocation</code> is
     *         located in.
     *
     * @see Location#getColony
     */
    public Colony getColony();
}
