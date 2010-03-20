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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class ColonyPlanTest extends FreeColTestCase {	
    
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
        final TileType forestType = spec().getTileType("model.tile.coniferForest");
        final TileType mountainType = spec().getTileType("model.tile.mountains");
        if(withBuildRawMat){
            builder.setTile(4, 8, forestType);
            builder.setTile(6, 8, mountainType);
        }
        return builder.build();
    }
	
    public void testPlanFoodProductionBeforeWorkerAllocation() {
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
        	
        Colony colony = getStandardColony();
        assertEquals(1, colony.getUnitCount());
        
        // get food production of central colony tile
        int expAmount = 0;
        for (GoodsType foodType : spec().getGoodsFood()) {
            expAmount += colony.getTile().getMaximumPotential(foodType, null);
        }
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        //plan.create();
        int amount = plan.getFoodProduction();
        assertEquals(expAmount, plan.getProductionOf(spec().getGoodsType("model.goods.food")));
        assertEquals("Wrong initial food amount",expAmount,amount);
    }
	
    public void testReqLumberAndHammersForBuild(){
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
            
        Colony colony = getStandardColony();
        final BuildingType warehouse = spec().getBuildingType("model.building.warehouse");
        colony.setCurrentlyBuilding(warehouse);
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        
        plan.create();
        
        final GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
        int lumber = plan.getProductionOf(lumberType);
        assertTrue("The colony should plan to produce lumber", lumber > 0);
        
        final GoodsType hammersType = spec().getGoodsType("model.goods.hammers");
        int hammers = plan.getProductionOf(hammersType);
        assertTrue("The colony should plan to produce hammers", hammers > 0);
    }
	
    public void testReqOreAndToolsWithEnoughHammersForBuild(){
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
            
        Colony colony = getStandardColony();
        
        // colony has enough hammers, requires tools
        final BuildingType warehouse = spec().getBuildingType("model.building.warehouse");
        final GoodsType oreType = spec().getGoodsType("model.goods.ore");
        final GoodsType hammersType = spec().getGoodsType("model.goods.hammers");
        final GoodsType toolsType = spec().getGoodsType("model.goods.tools");
        
        colony.setCurrentlyBuilding(warehouse);
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        
        plan.create();

        int ore = plan.getProductionOf(oreType);
        assertTrue("The colony should plan to produce ore", ore > 0);
     
        int tools = plan.getProductionOf(toolsType);
        assertTrue("The colony should plan to produce tools", tools > 0);
        
        int hammers = plan.getProductionOf(hammersType);
        assertFalse("The colony should not produce hammers, has enough", hammers > 0);
    }
	
    /*
     * This test verifies behavior when the colony isnt building anything
     */
    public void testNoBuildNoHammers(){
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
            
        Colony colony = getStandardColony();
        
        // colony isnt building anything
        colony.setCurrentlyBuilding(null);
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        
        plan.create();
        
        final GoodsType hammersType = spec().getGoodsType("model.goods.hammers");
        int hammers = plan.getProductionOf(hammersType);
        assertFalse("The colony should not produce hammers, building nothing", hammers > 0);
    }
	
    /*
     * This test verifies behavior when the colony has no tiles that provide 
     *the raw materials for the build, but has them in stock
     */
    public void testNoBuildRawMatTiles(){
        final int fullStock = 100;
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
            
        Colony colony = getStandardColony();
        final GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
        final GoodsType oreType = spec().getGoodsType("model.goods.ore");
        final GoodsType hammersType = spec().getGoodsType("model.goods.hammers");
        final GoodsType toolsType = spec().getGoodsType("model.goods.tools");

        // Add enough raw materials for build
        colony.addGoods(lumberType, fullStock);
        colony.addGoods(oreType, fullStock);
        
        final BuildingType warehouse = spec().getBuildingType("model.building.warehouse");
        colony.setCurrentlyBuilding(warehouse);

        ColonyPlan plan = new ColonyPlan(aiMain,colony);        
        plan.create();
        
        int lumber = plan.getProductionOf(lumberType);
        int hammers = plan.getProductionOf(hammersType);
        assertFalse("The colony no produce lumber, no forests available", lumber > 0);
        assertTrue("The colony should produce hammers, has lumber in stock", hammers > 0);

        // Simulate that enough hammers have been gathered, re-plan and re-check
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));
        plan.create();
        
        hammers = plan.getProductionOf(hammersType);
        int ore = plan.getProductionOf(oreType);
        int tools = plan.getProductionOf(toolsType);
        assertFalse("The colony should not produce hammers, has enough", hammers > 0);
        assertFalse("The colony cannot produce ore, none available", ore > 0);
        assertTrue("The colony should produce tools, has ore in stock", tools > 0);
    }
	
    /*
     * This test verifies adjustments to manufactured goods production
     */
    public void testAdjustProductionAndManufacture(){
        final int fullStock = 100;
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        final TileType prairieType = spec().getTileType("model.tile.prairie");
        Map map = getTestMap(prairieType);
        
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
        Tile t = map.getAdjacentTile(colony.getTile().getPosition(), Direction.N);
        Unit u = colony.getUnitList().get(0);
        ColonyTile colTile = colony.getColonyTile(t);

        final GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        final GoodsType clothType = spec().getGoodsType("model.goods.cloth");
        final GoodsType foodType = spec().getGoodsType("model.goods.food");

        u.work(colTile);
        u.setWorkType(cottonType);
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);        
        plan.create();
        
        assertEquals("Wrong primary raw material",foodType, plan.getPrimaryRawMaterial());
        assertEquals("Wrong secondary raw material",cottonType, plan.getSecondaryRawMaterial());

        assertEquals("Wrong number of units in colony tile", 1, colTile.getUnitCount());
        assertEquals("Unit should be picking cotton", cottonType, u.getWorkType());
        plan.adjustProductionAndManufacture();
        assertEquals("Unit should not have been shifted", cottonType, u.getWorkType());
        
        // Simulate that enough cotton have been gathered, re-adjust and re-check
        colony.addGoods(cottonType, fullStock);
        Building weaverHouse = colony.getBuildingsForConsuming(cottonType).get(0);
        assertEquals("Wrong number of units in waever house", 0, weaverHouse.getUnitCount());
        plan.adjustProductionAndManufacture();
        assertEquals("Wrong number of units in colony tile", 0, colTile.getUnitCount());
        assertEquals("Unit should have been shifted", clothType, u.getWorkType());
        assertEquals("Wrong number of units in waever house", 1, weaverHouse.getUnitCount());
    }
}
