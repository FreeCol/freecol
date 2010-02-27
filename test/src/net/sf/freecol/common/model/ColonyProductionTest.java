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

import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class ColonyProductionTest extends FreeColTestCase {

    public void testProductionSoldier() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, FreeCol.getSpecification().getTileType("model.tile.plains"), x, y);
            }
        }

        Map map = new Map(game, tiles);

        Resource grain = new Resource(game, map.getTile(5, 8),
                                      FreeCol.getSpecification().getResourceType("model.resource.Grain"));
        map.getTile(5, 8).setResource(grain);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
                
        game.setMap(map);
        UnitType veteran = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, veteran, UnitState.ACTIVE, veteran.getDefaultEquipment());

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        GoodsType foodType = spec().getGoodsType("model.goods.food");
        soldier.setWorkType(foodType);
        soldier.buildColony(colony);

        // Test the colony
        assertEquals(map.getTile(6, 8), colony.getTile());

        assertEquals("New Amsterdam", colony.getName());

        assertEquals(colony, colony.getTile().getSettlement());

        assertEquals(dutch, colony.getTile().getOwner());

        // Should have 50 Muskets and nothing else
        GoodsType muskets = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        assertNotNull(muskets);
            
        for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()){
            if (type == muskets)
                assertEquals(50, colony.getGoodsCount(type));
            else
                assertEquals(type.toString(), 0, colony.getGoodsCount(type));
        }

        // Test the state of the soldier
        // Soldier should be working on the field with the bonus
        assertEquals(foodType, soldier.getWorkType());
        
        assertEquals(colony.getColonyTile(map.getTile(5,8)).getTile(), soldier.getLocation().getTile());
        
        assertEquals(0, soldier.getMovesLeft());

        assertEquals(false, soldier.isArmed());
    }

    public void testProductionPioneer() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, FreeCol.getSpecification().getTileType("model.tile.plains"), x, y);
            }
        }

        Map map = new Map(game, tiles);

        Resource grain = new Resource(game, map.getTile(5, 8),
                                      FreeCol.getSpecification().getResourceType("model.resource.Grain"));
        map.getTile(5, 8).setResource(grain);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
                
        game.setMap(map);
        UnitType pioneerType = FreeCol.getSpecification().getUnitType("model.unit.hardyPioneer");
        GoodsType foodType = spec().getGoodsType("model.goods.food");
        Unit pioneer = new Unit(game, map.getTile(6, 8), dutch, pioneerType, UnitState.ACTIVE,
                                pioneerType.getDefaultEquipment());

        Colony colony = new Colony(game, dutch, "New Amsterdam", pioneer.getTile());
        pioneer.setWorkType(foodType);
        pioneer.buildColony(colony);

        // Test the colony
        assertEquals(map.getTile(6, 8), colony.getTile());

        assertEquals("New Amsterdam", colony.getName());

        assertEquals(colony, colony.getTile().getSettlement());

        assertEquals(dutch, colony.getTile().getOwner());

        // Should have 100 tools and nothing else
        GoodsType tools = FreeCol.getSpecification().getGoodsType("model.goods.tools");
        assertNotNull(tools);
            
        for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()){
            if (type == tools)
                assertEquals(100, colony.getGoodsCount(type));
            else
                assertEquals(type.toString(), 0, colony.getGoodsCount(type));
        }

        // Test the state of the pioneer
        // Pioneer should be working on the field with the bonus
        assertEquals(foodType, pioneer.getWorkType());
        
        assertEquals(colony.getColonyTile(map.getTile(5,8)).getTile(), pioneer.getLocation().getTile());
        
        assertEquals(0, pioneer.getMovesLeft());

        assertEquals(false, pioneer.isArmed());
    }
    
    public void testBellNetProduction(){
    	GoodsType bellsType = FreeCol.getSpecification().getGoodsType("model.goods.bells");
    	    	
    	Game game = getStandardGame();
    	
    	game.setMap(getTestMap());
    	
    	Colony colony = getStandardColony(7);
    	    	
    	int initialBellCount = colony.getGoodsCount(bellsType);
    	int expectedBellCount = 0;
    	int bellsProdPerTurn = colony.getProductionOf(bellsType);
    	int expectedBellProd = 1;
    	int bellsUpkeep = colony.getConsumption(bellsType);
    	int expectedBellUpkeep =  colony.getUnitCount() - 2;
    	int bellsNetProdPerTurn = colony.getProductionNetOf(bellsType);
    	int expectedBellNetProd = expectedBellProd - expectedBellUpkeep;
    	
    	assertEquals("Wrong bell count", expectedBellCount, initialBellCount);
    	assertEquals("Wrong bell production",expectedBellProd,bellsProdPerTurn);
    	assertEquals("Wrong bell upkeep",expectedBellUpkeep,bellsUpkeep);
    	assertEquals("Wrong bell net production",expectedBellNetProd,bellsNetProdPerTurn);
    }
    /**
     * Tests that there is no over production of horses, to avoid being thrown out
     * A test of the proper production of horses is in <code>BuildingTest</code>
     */
    public void testNoHorsesOverProduction() {
        Game game = getGame();
        game.setMap(getTestMap());
        
        Colony colony = getStandardColony(1);
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");

        Building pasture = colony.getBuilding(spec().getBuildingType("model.building.Country"));
        assertEquals(horsesType, pasture.getGoodsOutputType());
        assertEquals("Wrong warehouse capacity in colony",100,colony.getWarehouseCapacity());
        
        // Still room for more
        colony.addGoods(horsesType, 99);
        
        assertEquals("Wrong horse production",1, pasture.getProductionOf(horsesType));
        assertEquals("Wrong maximum horse production",9, pasture.getMaximumProduction());
        assertEquals("Wrong net horse production",1, colony.getProductionNetOf(horsesType));
        
        // No more room available
        colony.addGoods(horsesType, 1);
        assertEquals("Wrong number of horses in colony",colony.getWarehouseCapacity(), colony.getGoodsCount(horsesType));
        assertEquals("Wrong horse production",0, pasture.getProductionOf(horsesType));
        assertEquals("Wrong maximum horse production",10, pasture.getMaximumProduction());
        assertEquals("Wrong net horse production",0, colony.getProductionNetOf(horsesType));
    }
}
