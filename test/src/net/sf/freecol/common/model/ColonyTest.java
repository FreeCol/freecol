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
    BuildingType depotType = FreeCol.getSpecification().getBuildingType("model.building.Depot");
    BuildingType warehouseType = FreeCol.getSpecification().getBuildingType("model.building.Warehouse");
    BuildingType warehouseExpansionType = FreeCol.getSpecification().getBuildingType("model.building.WarehouseExpansion");
    BuildingType churchType = FreeCol.getSpecification().getBuildingType("model.building.Chapel");
    BuildingType carpenterHouseType =  FreeCol.getSpecification().getBuildingType("model.building.CarpenterHouse");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    GoodsType hammerGoodsType = spec().getGoodsType("model.goods.hammers");
    GoodsType lumberGoodsType = spec().getGoodsType("model.goods.lumber");
    
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
    
    /**
     * Tests completion of buildable
     */
    public void testBuildingCompletion() {
        Game game = getGame();
        game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony();
        Building initialWarehouse = new Building(getGame(), colony, depotType);
        colony.addBuilding(initialWarehouse);
        assertTrue("Colony should be able to build warehouse",colony.canBuild(warehouseType));
        colony.setCurrentlyBuilding(warehouseType);
        colony.addGoods(hammerGoodsType, 90);
                
        // Simulate that the build is done
        assertFalse("Colony should not have warehouse",colony.getWarehouse().getType() == warehouseType);
        colony.checkBuildableComplete();
        assertTrue("Colony should have warehouse",colony.getWarehouse().getType() == warehouseType);
        
        // Verify that upgrade is next to build
        assertEquals("Colony should not be building nothing", BuildableType.NOTHING, colony.getCurrentlyBuilding()); 
    }
    
    public void testNoBuildingMaterialsProductionWhenBuildingNothing(){
        Game game = getGame();
        game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony();
        Building carpenterHouse = new Building(getGame(), colony, carpenterHouseType);
        colony.addBuilding(carpenterHouse);
        Unit unit = colony.getUnitList().get(0);
        colony.getBuilding(carpenterHouseType).add(unit);
        // necessary for work production
        int initialLumber = 100;
        int initialHammers = 0;
        colony.addGoods(lumberGoodsType, initialLumber);
        
        assertEquals("Wrong initial lumber quantity, ",initialLumber,colony.getGoodsCount(lumberGoodsType));
        assertEquals("Colony should not be building nothing, ", BuildableType.NOTHING, colony.getCurrentlyBuilding());
        assertTrue("Colony shoud be able to produce work (hammers)",colony.getProductionOf(hammerGoodsType)>0);
        assertEquals("Colony shold not have any work production(hammers) initially, ",initialHammers,colony.getGoodsCount(hammerGoodsType));
        colony.newTurn();
        assertEquals("Colony shold not have any work production(hammers) after, ",initialHammers,colony.getGoodsCount(hammerGoodsType));
        assertEquals("Wrong final lumber quantity, ",initialLumber,colony.getGoodsCount(lumberGoodsType));
    }
}
