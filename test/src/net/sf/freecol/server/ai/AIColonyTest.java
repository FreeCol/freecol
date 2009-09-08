/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.server.ai;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.MockMapGenerator;

public class AIColonyTest extends FreeColTestCase {	
    final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    final UnitType lumberJackType = spec().getUnitType("model.unit.expertLumberJack");
    final TileType forestType = spec().getTileType("model.tile.coniferForest");
    final TileType mountainType = spec().getTileType("model.tile.mountains");
    final TileType savannahType = spec().getTileType("model.tile.savannah");
    final GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
    final GoodsType oreType = spec().getGoodsType("model.goods.ore");
    final GoodsType hammersType = spec().getGoodsType("model.goods.hammers");
    final GoodsType toolsType = spec().getGoodsType("model.goods.tools");
    final GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
    final BuildingType warehouse = spec().getBuildingType("model.building.Warehouse");
    final EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
    final int fullStock = 100;
    
    FreeColServer server = null;
	
    public void tearDown() throws Exception {
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }
        
    // creates the special map for the tests
    // map will have: 
    //    - a colony in (5,8) (built after)
    //    - a forest in (4,8) for lumber
    //    - a mountain in (6,8) for ore
    private Map buildMap(boolean withBuildRawMat){
        MapBuilder builder = new MapBuilder(getGame());
        builder.setBaseTileType(savannahType);
        if(withBuildRawMat){
            builder.setTile(4, 8, forestType);
            builder.setTile(6, 8, mountainType);
        }
        return builder.build();
    }
        
    /*
     * Tests worker allocation regarding building tasks
     */
    public void testBuildersAllocation() {
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = buildMap(true);
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        
        FreeColTestCase.setGame(game);
        
        AIMain aiMain = server.getAIMain();

        //the number needs to be high to ensure allocation
        Colony colony = getStandardColony(6);

        colony.setCurrentlyBuilding(warehouse);
        
        final Building carpenterHouse = colony.getBuildingForProducing(hammersType);
        final Building blacksmithHouse = colony.getBuildingForProducing(toolsType);
       
        AIColony aiColony = (AIColony) aiMain.getAIObject(colony); 
        ServerPlayer player = (ServerPlayer) colony.getOwner();
        
        aiColony.rearrangeWorkers(player.getConnection());
        
        assertTrue("Colony should have been assigned a lumberjack",colony.getProductionOf(lumberType) > 0);
        assertTrue("Colony should have been assigned a carpenter",carpenterHouse.getUnitCount() > 0);
        
        // Simulate that enough hammers have been gathered, re-arrange and re-check
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));
        
        aiColony.rearrangeWorkers(player.getConnection());
        assertFalse("Colony does not need a carpenter",carpenterHouse.getUnitCount() > 0);
        assertTrue("Colony should have been assigned a ore miner",colony.getProductionOf(oreType) > 0);
        assertTrue("Colony should have been assigned a blacksmith",blacksmithHouse.getUnitCount() > 0);
    }
        
    /*
     * Tests worker allocation regarding building tasks when the colony
     *does not have tiles that provide the raw materials for the build
     */
    public void testBuildersAllocNoRawMatTiles() {
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = buildMap(false);
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        
        FreeColTestCase.setGame(game);
        
        AIMain aiMain = server.getAIMain();

        //the number needs to be high to ensure allocation
        Colony colony = getStandardColony(6);

        // we need to ensure that there arent tiles with production of the raw materials
        // if this fails, the type of the tile in buildMap() must be changed to meet this
        //requirements
        String msg1 = "For the test to work, the colony cannot have tiles that produce lumber";
        String msg2 = "For the test to work, the colony cannot have tiles that produce ore";
        for(ColonyTile t : colony.getColonyTiles()){
            Tile tile = t.getTile();
            assertTrue(msg1,tile.potential(lumberType, colonistType) == 0);
            assertTrue(msg2,tile.potential(oreType, colonistType) == 0);
        }
        
        colony.setCurrentlyBuilding(warehouse);
        
        final Building carpenterHouse = colony.getBuildingForProducing(hammersType);
        final Building blacksmithHouse = colony.getBuildingForProducing(toolsType);
       
        AIColony aiColony = (AIColony) aiMain.getAIObject(colony); 
        ServerPlayer player = (ServerPlayer) colony.getOwner();
        
        aiColony.rearrangeWorkers(player.getConnection());
        
        assertFalse("Colony couldnt have been assigned a lumberjack, no lumber",colony.getProductionOf(lumberType) > 0);
        assertFalse("Colony couldnt have been assigned a carpenter, no lumber",carpenterHouse.getUnitCount() > 0);
        
        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, fullStock);
        aiColony.rearrangeWorkers(player.getConnection());
        
        assertFalse("Colony couldnt have been assigned a lumberjack, no lumber",colony.getProductionOf(lumberType) > 0);
        assertTrue("Colony should have been assigned a carpenter, has lumber in stock",carpenterHouse.getUnitCount() > 0);
        
        // Simulate that enough hammers have been gathered, re-arrange and re-check
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));
        
        aiColony.rearrangeWorkers(player.getConnection());
        assertFalse("Colony does not need a carpenter",carpenterHouse.getUnitCount() > 0);
        assertFalse("Colony couldnt have been assigned a ore miner, no ore",colony.getProductionOf(oreType) > 0);
        assertFalse("Colony couldnt have been assigned a blacksmith, no ore",blacksmithHouse.getUnitCount() > 0);
               
        // Add ore to stock, re-arrange and re-check
        colony.addGoods(oreType, fullStock);
        aiColony.rearrangeWorkers(player.getConnection());
        
        assertFalse("Colony couldnt have been assigned a ore miner, no ore",colony.getProductionOf(oreType) > 0);
        assertTrue("Colony should have been assigned a blacksmith, has ore in stock",blacksmithHouse.getUnitCount() > 0);
    }
    
    /*
     * Tests expert allocation regarding raw materials where there are plenty already in stock
     */
    public void testExpertAllocColonyHasEnoughRawMat() {
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = getTestMap(forestType);
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        
        FreeColTestCase.setGame(game);
        
        AIMain aiMain = server.getAIMain();

        
        FreeColTestUtils.ColonyBuilder builder = FreeColTestUtils.getColonyBuilder();
        Colony colony = builder.addColonist(lumberJackType).build();
        
        ServerPlayer player = (ServerPlayer) colony.getOwner();
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        Unit lumberjack = colony.getUnitList().get(0);
                       
        AIColony aiColony = (AIColony) aiMain.getAIObject(colony); 

        aiColony.rearrangeWorkers(player.getConnection());
                
        assertTrue("Lumberjack should have been assigned to collect lumber",lumberjack.getWorkType() == lumberType);
                
        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, fullStock);
        aiColony.rearrangeWorkers(player.getConnection());
        
        String errMsg = "Lumberjack should not have been assigned to collect lumber, enough lumber in the colony";
        assertFalse(errMsg, lumberType == lumberjack.getWorkType());
    }
    
    public void testCheckConditionsForHorseBreed(){
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = getTestMap();
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        
        FreeColTestCase.setGame(game);
        
        AIMain aiMain = server.getAIMain();

        Colony colony = getStandardColony(1);
        AIColony aiColony = (AIColony) aiMain.getAIObject(colony);
        
        GoodsType reqGoodsType = horsesType.getRawMaterial();
        int foodSuplus = colony.getProductionOf(reqGoodsType) - colony.getFoodConsumptionByType(reqGoodsType);
        assertTrue("Setup error, colony does not have food surplus", foodSuplus > 0);
                
        Unit scout = new Unit(getGame(), colony.getTile(), colony.getOwner(), colonistType, UnitState.ACTIVE, horsesEqType);
        assertTrue("Scout should be mounted",scout.isMounted());
        
        assertTrue("Setup error, colony should not have horses in stock",colony.getGoodsCount(horsesType) == 0);
        
        aiColony.checkConditionsForHorseBreed();
        
        assertTrue("Colony should now have horses in stock",colony.getGoodsCount(horsesType) > 0);
        assertFalse("Scout should not be mounted",scout.isMounted());
    }
}
