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
import net.sf.freecol.util.test.FreeColTestCase;

public class ColonyTest extends FreeColTestCase {
    BuildingType warehouseType = FreeCol.getSpecification().getBuildingType("model.building.Depot");
    BuildingType churchType = FreeCol.getSpecification().getBuildingType("model.building.Chapel");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    
    public void testCurrentlyBuilding() {
        Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
    	Colony colony = getStandardColony();
        assertEquals("Colony should no be building nothing",BuildableType.NOTHING,colony.getCurrentlyBuilding());
    	    	
    	colony.setCurrentlyBuilding(warehouseType);
    	assertEquals("Colony should be building a warehouse",warehouseType,colony.getCurrentlyBuilding());
    	
        colony.setCurrentlyBuilding(churchType);
        assertEquals("Colony should be building a church",churchType,colony.getCurrentlyBuilding());        
    }
    
    public void testBuildQueueDoesNotAcceptBuildingDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony();
        // Building queue is never empty, always has a last entry = BuildableType.NOTHING
        assertEquals("Building queue have 1 entry",1,colony.getBuildQueue().size());
                
        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should have 2 entries",2,colony.getBuildQueue().size());
        
        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 2 entries",2,colony.getBuildQueue().size());
        
        colony.setCurrentlyBuilding(churchType);
        assertEquals("Building queue should have 3 entries",3,colony.getBuildQueue().size());
        
        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 3 entries",3,colony.getBuildQueue().size());
    }
    
    public void testBuildQueueAcceptsUnitDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony();
        // Building queue is never empty, always has a last entry = BuildableType.NOTHING
        assertEquals("Building queue have 1 entry",1,colony.getBuildQueue().size());
                
        colony.setCurrentlyBuilding(wagonTrainType);
        assertEquals("Building queue should have 2 entries",2,colony.getBuildQueue().size());
                
        colony.setCurrentlyBuilding(wagonTrainType);
        assertEquals("Building queue should have 3 entries",3,colony.getBuildQueue().size());
    }
}
