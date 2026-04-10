/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.ai.colony.plan;

import java.util.List;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan.TentativeColonyTilePlan;

/**
 * Initial steps in a colony plan that is not ensured to take food consumption into account.
 * 
 * Always call {@see StandardColonyPlanner#produceFoodForConsumption(TentativeColonyPlan)} after
 * using one of these steps.
 */
public final class InitialSteps {

    private InitialSteps() {}

    /**
     * Places experts on every resource since this looks good. Later steps might
     * replace those experts as needed (but only then for a good reason).
     */
    public static void produceGoodsTypesEnhancedByBonusResources(TentativeColonyPlan tcp) {
        for (Tile t : tcp.unassignedWorkTiles()) {
            if (t.getResource() == null) {
                continue;
            }
            final GoodsType resourceGoodsType = t.getResource().getBestGoodsType();
            final TentativeColonyTilePlan tentativeColonyTilePlan = TentativeColonyTilePlan.expertProduction(t, resourceGoodsType);
            tcp.addOrReplace(tentativeColonyTilePlan);
        }
    }
    
    /**
     * Produce food if that's the only choice. This greatly speeds up the calculation of the
     * colony plan without having any ill effects. 
     */
    public static void produceFoodOnTilesWhereNoOtherPossibleOutputExists(TentativeColonyPlan tcp) {
        for (Tile t : tcp.unassignedWorkTiles()) {
            final List<AbstractGoods> sortedPotential = t.getSortedPotential();
            if (sortedPotential.isEmpty() || !sortedPotential.stream().allMatch(g -> g.getType().isFoodType())) {
                continue;
            }
            final GoodsType resourceGoodsType = sortedPotential.get(0).getType();
            final TentativeColonyTilePlan tentativeColonyTilePlan = TentativeColonyTilePlan.expertProduction(t, resourceGoodsType);
            tcp.addOrReplace(tentativeColonyTilePlan);
        }
    }
    
    /**
     * Produces unstorable building materials (hammers) along with the required raw materials.
     * 
     * Note that this step requires all raw materials (lumber) to be produced in the colony. If not,
     * then no additional buildings or worker plans are added.
     */
    public static void produceRawAndManufactoredUnstorableBuildingMaterials(TentativeColonyPlan tcp) {
        ProduceRawAndManufactoredUnstorableBuildingMaterialsStep.execute(tcp);
    }
}
