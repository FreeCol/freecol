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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collection;

import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/** 
 * Simple container to define where and what a unit is working on.
 */
public class Occupation {

    public WorkLocation workLocation;
    public ProductionType productionType;
    public GoodsType workType;


    /**
     * Create an Occupation.
     *
     * @param workLocation The {@code WorkLocation} to work at.
     * @param productionType The {@code ProductionType} to
     *     use at the work location.
     * @param workType The {@code GoodsType} to produce at the
     *     work location with the production type.
     */
    public Occupation(WorkLocation workLocation,
        ProductionType productionType,
        GoodsType workType) {
        this.workLocation = workLocation;
        this.productionType = productionType;
        this.workType = workType;
    }

    /**
     * Install a unit into this occupation.
     *
     * @param unit The {@code Unit} to establish.
     * @return True if the unit is installed.
     */
    public boolean install(Unit unit) {
        if (!unit.setLocation(workLocation)) return false;
        if (productionType != workLocation.getProductionType()) {
            workLocation.setProductionType(productionType);
        }
        if (workType != unit.getWorkType()) {
            unit.changeWorkType(workType);
        }
        return true;
    }

    /**
     * Calculates the maximum possible input available for a production type.
     *
     * This method determines the production ceiling by checking the colony's 
     * current net production and existing stock for all required input goods. 
     *
     * @param pt The {@code ProductionType} defining the required inputs.
     * @param colony The {@code Colony} where the production is occurring.
     * @return The limiting factor (minimum) among all required input goods.
     */
    private int computeMinInput(ProductionType pt, Colony colony) {
        return min(pt.getInputs(),
            ag -> Math.max(colony.getNetProductionOf(ag.getType()),
                           colony.getGoodsCount(ag.getType())));
    }

    /**
     * Improve this occupation to the best available production for the
     * given unit type.
     *
     * @param unitType The {@code UnitType} to produce the goods.
     * @param wl The {@code WorkLocation} to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of {@code GoodsType} to
     *     consider producing.
     * @param alone True if the unit is alone, and can set the
     *     production type.
     * @param lb A {@code LogBuilder} to log to.
     * @return The best amount of production found.
     */
    private int improve(UnitType unitType, WorkLocation wl, int bestAmount,
            Collection<GoodsType> workTypes, boolean alone, LogBuilder lb) {

        lb.add(" alone=", alone);

        // Determine which production types we are allowed to use
        var productionTypes = new ArrayList<ProductionType>();
        if (alone) {
            productionTypes.addAll(wl.getAvailableProductionTypes(false));
        } else {
            productionTypes.add(wl.getProductionType());
        }

        final Colony colony = wl.getColony();

        // Stage best results
        WorkLocation bestWL = this.workLocation;
        ProductionType bestPT = this.productionType;
        GoodsType bestGT = this.workType;

        for (ProductionType pt : transform(productionTypes, isNotNull())) {
            final int minInput = computeMinInput(pt, colony);
            lb.add("\n      try=", pt);

            for (GoodsType gt : transform(workTypes, isNotNull(g -> pt.getOutput(g)))) {
                int potential = wl.getPotentialProduction(gt, unitType);
                int amount = Math.min(minInput, potential);

                lb.add(" ", gt.getSuffix(), "=", amount, "/", minInput,
                       "/", potential, (bestAmount < amount ? "!" : ""));

                if (bestAmount < amount) {
                    bestAmount = amount;
                    bestWL = wl;
                    bestPT = pt;
                    bestGT = gt;
                }
            }
        }

        // Commit best results AFTER loops
        this.workLocation = bestWL;
        this.productionType = bestPT;
        this.workType = bestGT;

        return bestAmount;
    }

    /**
     * Determines if a unit has the authority to define the production type.
     *
     * @param unit The {@code Unit} to check.
     * @param wl The {@code WorkLocation} to check.
     * @return True if the location is empty or the unit is the sole occupant.
     */
    private boolean isUnitAlone(Unit unit, WorkLocation wl) {
        boolean present = unit.getLocation() == wl;
        return wl.getProductionType() == null
            || wl.isEmpty()
            || (present && wl.getUnitCount() == 1);
    }

    /**
     * Improve this occupation to the best available production for the
     * given unit.
     *
     * @param unit The {@code Unit} to produce the goods.
     * @param wl The {@code WorkLocation} to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of {@code GoodsType} to
     *     consider producing.
     * @param lb A {@code LogBuilder} to log to.
     * @return The best amount of production found.
     */
    public int improve(Unit unit, WorkLocation wl, int bestAmount,
                       Collection<GoodsType> workTypes, LogBuilder lb) {
        // Can the unit work at the wl?
        boolean present = unit.getLocation() == wl;
        lb.add("\n    ", wl,
            ((!present && !wl.canAdd(unit)) ? " no-add" : ""));
        if (!present && !wl.canAdd(unit)) return bestAmount;

        // Can the unit determine the production type at this WL?
        // This will be true if the unit is going to be alone or if
        // the production type is as yet unset.  Set the
        // productionTypes list accordingly.
        boolean alone = isUnitAlone(unit, wl);
        return improve(unit.getType(), wl, bestAmount, workTypes, alone, lb);
    }

    /**
     * Improve this occupation to the best available production for the
     * given unit type.
     *
     * @param unitType The {@code UnitType} to produce the goods.
     * @param wl The {@code WorkLocation} to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of {@code GoodsType} to
     *     consider producing.
     * @param lb A {@code LogBuilder} to log to.
     * @return The best amount of production found.
     */
    public int improve(UnitType unitType, WorkLocation wl, int bestAmount,
        Collection<GoodsType> workTypes, LogBuilder lb) {
        return improve(unitType, wl, bestAmount, workTypes, wl.isEmpty(), lb);
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("[Occupation %s %s]",
                workLocation != null ? workLocation : "null",
                workType != null ? workType.getSuffix() : "");
    }
}
