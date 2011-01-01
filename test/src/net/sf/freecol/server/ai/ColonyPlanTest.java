/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;


public class ColonyPlanTest extends FreeColTestCase {

    private static final BuildingType warehouse
        = spec().getBuildingType("model.building.warehouse");

    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType clothType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType sugarType
        = spec().getGoodsType("model.goods.sugar");
    private static final GoodsType rumType
        = spec().getGoodsType("model.goods.rum");

    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType oreType
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final TileType forestType
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType mountainType
        = spec().getTileType("model.tile.mountains");
    private static final TileType prairieType
        = spec().getTileType("model.tile.prairie");
    private static final TileType savannahType
        = spec().getTileType("model.tile.savannah");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    /**
     * Creates the special map for the tests
     * map will have:
     *    - a colony in (5,8) (built after)
     *    - a forest in (4,8) for lumber
     *    - a mountain in (6,8) for ore
     */
    private Map buildMap(boolean withBuildRawMat) {
        MapBuilder builder = new MapBuilder(getGame());
        if(withBuildRawMat){
            builder.setTile(4, 8, forestType);
            builder.setTile(6, 8, mountainType);
        }
        return builder.build();
    }


    public void testPlanFoodProductionBeforeWorkerAllocation() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        assertEquals(1, colony.getUnitCount());

        // get food production of central colony tile
        int expAmount = 0;
        for (GoodsType foodType : spec().getFoodGoodsTypeList()) {
            expAmount += colony.getTile().getMaximumPotential(foodType, null);
        }

        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        //plan.create();
        int amount = plan.getFoodProduction();
        assertEquals(expAmount, plan.getProductionOf(grainType));
        assertEquals("Wrong initial food amount",expAmount,amount);
    }

    public void testReqLumberAndHammersForBuild(){
        Map map = buildMap(true);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        colony.setCurrentlyBuilding(warehouse);

        ColonyPlan plan = new ColonyPlan(aiMain,colony);

        plan.create();

        int lumber = plan.getProductionOf(lumberType);
        assertTrue("The colony should plan to produce lumber", lumber > 0);
        int hammers = plan.getProductionOf(hammersType);
        assertTrue("The colony should plan to produce hammers", hammers > 0);
    }

    public void testReqOreAndToolsWithEnoughHammersForBuild(){
        Map map = buildMap(true);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();

        // colony has enough hammers, requires tools
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
        Map map = buildMap(true);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();

        // colony isnt building anything
        colony.setCurrentlyBuilding(null);

        ColonyPlan plan = new ColonyPlan(aiMain,colony);

        plan.create();

        int hammers = plan.getProductionOf(hammersType);
        assertFalse("The colony should not produce hammers, building nothing", hammers > 0);
    }

    /*
     * This test verifies behavior when the colony has no tiles that
     * provide the raw materials for the build, but has them in stock
     */
    public void testNoBuildRawMatTiles(){
        Map map = buildMap(false);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        final int fullStock = 100;
        Colony colony = getStandardColony();

        // Add enough raw materials for build
        colony.addGoods(lumberType, fullStock);
        colony.addGoods(oreType, fullStock);

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
        Map map = getTestMap(savannahType);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        final int fullStock = 100;
        Colony colony = getStandardColony(1);
        Tile t = colony.getTile().getAdjacentTile(Direction.N);
        Unit u = colony.getUnitList().get(0);
        ColonyTile colTile = colony.getColonyTile(t);

        u.setLocation(colTile);
        u.setWorkType(sugarType);

        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        plan.create();

        assertEquals("Wrong primary raw material",grainType, plan.getPrimaryRawMaterial());
        assertEquals("Wrong secondary raw material",sugarType, plan.getSecondaryRawMaterial());

        assertEquals("Wrong number of units in colony tile", 1, colTile.getUnitCount());
        assertEquals("Unit should be picking cotton", sugarType, u.getWorkType());
        plan.adjustProductionAndManufacture();
        assertEquals("Unit should not have been shifted", sugarType, u.getWorkType());

        // Simulate that enough cotton have been gathered, re-adjust and re-check
        colony.addGoods(sugarType, fullStock);
        Building distillery = colony.getBuildingsForConsuming(sugarType).get(0);
        assertEquals("Wrong number of units in waever house", 0, distillery.getUnitCount());
        plan.adjustProductionAndManufacture();
        assertEquals("Wrong number of units in colony tile", 0, colTile.getUnitCount());
        assertEquals("Unit should have been shifted", rumType, u.getWorkType());
        assertEquals("Wrong number of units in waever house", 1, distillery.getUnitCount());
    }
}
