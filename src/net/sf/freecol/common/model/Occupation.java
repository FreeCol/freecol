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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * @param workLocation The <code>WorkLocation</code> to work at.
     * @param productionType The <code>ProductionType</code> to
     *     use at the work location.
     * @param workType The <code>GoodsType</code> to produce at the
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
     * @param unit The <code>Unit</code> to establish.
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
     * Improve this occupation to the best available production for the
     * given unit type.
     *
     * @param unitType The <code>UnitType</code> to produce the goods.
     * @param wl The <code>WorkLocation</code> to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @param alone True if the unit is alone, and can set the
     *     production type.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The best amount of production found.
     */
    private int improve(UnitType unitType, WorkLocation wl, int bestAmount,
        Collection<GoodsType> workTypes, boolean alone, LogBuilder lb) {

        lb.add(" alone=", alone);
        List<ProductionType> productionTypes = new ArrayList<>();
        if (alone) {
            productionTypes.addAll(wl.getAvailableProductionTypes(false));
        } else {
            productionTypes.add(wl.getProductionType());
        }

        // Try the available production types for the best production.
        final Colony colony = wl.getColony();
        for (ProductionType pt : productionTypes) {
            lb.add("\n      try=", pt);
            if (pt != null) {
                for (GoodsType gt : workTypes) {
                    if (pt.getOutput(gt) == null) continue;
                    int minInput = FreeColObject.INFINITY;
                    List<AbstractGoods> inputs = pt.getInputs();
                    for (AbstractGoods ag : inputs) {
                        int input = Math.max(colony.getGoodsCount(ag.getType()),
                            colony.getNetProductionOf(ag.getType()));
                        minInput = Math.min(minInput, input);
                    }
                    int potential = wl.getPotentialProduction(gt, unitType);
                    int amount = Math.min(minInput, potential);
                    lb.add(" ", gt.getSuffix(), "=", amount, "/", minInput,
                        "/", potential, ((bestAmount < amount) ? "!" : ""));
                    if (bestAmount < amount) {
                        bestAmount = amount;
                        this.workLocation = wl;
                        this.productionType = pt;
                        this.workType = gt;
                    }
                }
            }
        }
        return bestAmount;   
    }

    /**
     * Improve this occupation to the best available production for the
     * given unit.
     *
     * @param unit The <code>Unit</code> to produce the goods.
     * @param wl The <code>WorkLocation</code> to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @param lb A <code>LogBuilder</code> to log to.
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
        boolean alone = wl.getProductionType() == null
            || wl.isEmpty()
            || (present && wl.getUnitCount() == 1);
        return improve(unit.getType(), wl, bestAmount, workTypes, alone, lb);
    }

    /**
     * Improve this occupation to the best available production for the
     * given unit type.
     *
     * @param unitType The <code>UnitType</code> to produce the goods.
     * @param wl The <code>WorkLocation</code> to check.
     * @param bestAmount The best amount of goods produced found so far.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The best amount of production found.
     */
    public int improve(UnitType unitType, WorkLocation wl, int bestAmount,
        Collection<GoodsType> workTypes, LogBuilder lb) {
        return improve(unitType, wl, bestAmount, workTypes, wl.isEmpty(), lb);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[Occupation ").append(workLocation);
            //.append(" ").append(productionType)
        if (workType != null) sb.append(" ").append(workType.getSuffix());
        sb.append("]");
        return sb.toString();
    }
}
