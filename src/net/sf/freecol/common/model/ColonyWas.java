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
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony.ColonyChangeEvent;


/**
 * Helper container to remember a colony state prior to some
 * change, and fire off any consequent property changes.
 */
public class ColonyWas {

    private static final Logger logger = Logger.getLogger(ColonyWas.class.getName());

    private final Colony colony;
    private final int population;
    private final int productionBonus;
    private final List<BuildableType> buildQueue;


    /**
     * Record the state of a colony.
     *
     * @param colony The <code>Colony</code> to remember.
     */
    public ColonyWas(Colony colony) {
        this.colony = colony;
        this.population = colony.getUnitCount();
        this.productionBonus = colony.getProductionBonus();
        this.buildQueue = new ArrayList<>(colony.getBuildQueue());
        if (colony.getGoodsContainer() != null) {
            colony.getGoodsContainer().saveState();
        }
    }

    /**
     * Fire any property changes resulting from actions within a
     * colony.
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        boolean ret = false;
        int newPopulation = colony.getUnitCount();
        if (newPopulation != population) {
            String pc = ColonyChangeEvent.POPULATION_CHANGE.toString();
            colony.firePropertyChange(pc, population, newPopulation);
            ret = true;
        }
        int newProductionBonus = colony.getProductionBonus();
        if (newProductionBonus != productionBonus) {
            String pc = ColonyChangeEvent.BONUS_CHANGE.toString();
            colony.firePropertyChange(pc, productionBonus,
                newProductionBonus);
            ret = true;
        }
        List<BuildableType> newBuildQueue = colony.getBuildQueue();
        if (!newBuildQueue.equals(buildQueue)) {
            String pc = ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString();
            colony.firePropertyChange(pc, buildQueue, newBuildQueue);
            ret = true;
        }
        if (colony.getGoodsContainer() != null) {
            colony.getGoodsContainer().fireChanges();
            ret = true;
        }
        return true;
    }
}
