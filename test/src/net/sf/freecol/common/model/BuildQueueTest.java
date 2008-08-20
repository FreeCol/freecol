/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildQueueTest extends FreeColTestCase {

    BuildingType warehouse = spec().getBuildingType("model.building.Depot");
    BuildingType weaverHouse = spec().getBuildingType("model.building.WeaverHouse");
    BuildingType weaverShop = spec().getBuildingType("model.building.WeaverShop");
    BuildingType textileMill = spec().getBuildingType("model.building.TextileMill");
    BuildingType armory = spec().getBuildingType("model.building.Armory");
    BuildingType shipyard = spec().getBuildingType("model.building.Shipyard");
    UnitType wagonTrain = spec().getUnitType("model.unit.wagonTrain");
    UnitType artillery = spec().getUnitType("model.unit.artillery");
    UnitType caravel = spec().getUnitType("model.unit.caravel");

    public void testQueueWithoutColony() {

        BuildQueue queue = new BuildQueue();
        
        assertEquals(0, queue.findMinimumIndex(warehouse));
        assertEquals(0, queue.findMinimumIndex(weaverHouse));
        assertEquals(-1, queue.findMinimumIndex(weaverShop));
        assertEquals(-1, queue.findMinimumIndex(textileMill));

        queue.add(weaverHouse);
        assertEquals(1, queue.findMinimumIndex(weaverShop));
        assertEquals(-1, queue.findMinimumIndex(textileMill));

        queue.add(weaverShop);
        // we don't check for required abilities
        assertEquals(2, queue.findMinimumIndex(textileMill));

    }

    public void testQueueWithColony() {

        Colony colony = getStandardColony();
        assertNotNull(colony.getBuilding(weaverHouse));
        BuildQueue queue = new BuildQueue();
        queue.setColony(colony);
        
        assertEquals(0, queue.findMinimumIndex(warehouse));
        assertEquals(0, queue.findMinimumIndex(weaverShop));
        assertEquals(-1, queue.findMinimumIndex(textileMill));

        colony.addBuilding(new Building(getGame(), colony, weaverShop));
        // we don't check for required abilities
        assertEquals(0, queue.findMinimumIndex(textileMill));

    }

    public void testUnitQueue() {

        Colony colony = getStandardColony();
        BuildQueue queue = new BuildQueue();
        queue.setColony(colony);
        
        assertEquals(0, queue.findMinimumIndex(wagonTrain));
        assertEquals(-1, queue.findMinimumIndex(artillery));
        assertEquals(-1, queue.findMinimumIndex(caravel));

        queue.add(armory);
        assertEquals(1, queue.findMinimumIndex(artillery));
        assertEquals(-1, queue.findMinimumIndex(caravel));

        queue.add(shipyard);
        assertEquals(2, queue.findMinimumIndex(caravel));

    }

}
