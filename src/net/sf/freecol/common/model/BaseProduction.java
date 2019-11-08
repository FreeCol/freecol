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


import java.util.List;

/**
 * The BaseProduction interface eliminates duplicate code in
 *      {@link BuildingType} and {@link TileType} as the
 *      {@link #getBaseProduction(ProductionType, GoodsType, UnitType)}
 *      method originally was identical methods in seperate classes
 */
public interface BaseProduction {

    /**
     * Get the base production of a given goods type for an optional
     * unit type.
     *
     * @param productionType An optional {@code ProductionType} to use,
     *     if null the best available one is used.
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} that is to do
     *     the work, if null the unattended production is considered.
     * @return The amount of goods produced.
     */
    public default int getBaseProduction(ProductionType productionType,
                                         GoodsType goodsType, UnitType unitType) {
        if (goodsType == null) return 0;
        if (productionType == null) {
            productionType = ProductionType.getBestProductionType(goodsType,
                    getAvailableProductionTypes(unitType == null));
        }
        if (productionType == null) return 0;
        AbstractGoods best = productionType.getOutput(goodsType);
        return (best == null) ? 0 : best.getAmount();
    }

    /**
     * At the interface level, this code is here simply to let the
     *      {@link #getBaseProduction(ProductionType, GoodsType, UnitType)}
     *      method know to reference the subclass's method.
     *
     * @param unattended Whether the production is unattended.
     * @return A list of {@code ProductionType}s.
     */
    public abstract List<ProductionType> getAvailableProductionTypes(boolean unattended);
}
