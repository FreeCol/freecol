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

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;


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
    private static final GoodsType tobaccoType
        = spec().getGoodsType("model.goods.tobacco");
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

    private static final TileImprovementType clearForest
        = spec().getTileImprovementType("model.improvement.clearForest");
    private static final TileImprovementType fishBonusLand
        = spec().getTileImprovementType("model.improvement.fishBonusLand");
    private static final TileImprovementType fishBonusRiver
        = spec().getTileImprovementType("model.improvement.fishBonusRiver");
    private static final TileImprovementType plow
        = spec().getTileImprovementType("model.improvement.plow");
    private static final TileImprovementType river
        = spec().getTileImprovementType("model.improvement.river");
    private static final TileImprovementType road
        = spec().getTileImprovementType("model.improvement.road");

    private static final TileType arctic
        = spec().getTileType("model.tile.arctic");
    private static final TileType desert
        = spec().getTileType("model.tile.desert");
    private static final TileType desertForest
        = spec().getTileType("model.tile.scrubForest");
    private static final TileType grassland
        = spec().getTileType("model.tile.grassland");
    private static final TileType grasslandForest
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType highSeas
        = spec().getTileType("model.tile.highSeas");
    private static final TileType hills
        = spec().getTileType("model.tile.hills");
    private static final TileType marsh
        = spec().getTileType("model.tile.marsh");
    private static final TileType marshForest
        = spec().getTileType("model.tile.wetlandForest");
    private static final TileType mountains
        = spec().getTileType("model.tile.mountains");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType plainsForest
        = spec().getTileType("model.tile.mixedForest");
    private static final TileType prairie
        = spec().getTileType("model.tile.prairie");
    private static final TileType prairieForest
        = spec().getTileType("model.tile.broadleafForest");
    private static final TileType savannah
        = spec().getTileType("model.tile.savannah");
    private static final TileType savannahForest
        = spec().getTileType("model.tile.tropicalForest");
    private static final TileType swamp
        = spec().getTileType("model.tile.swamp");
    private static final TileType swampForest
        = spec().getTileType("model.tile.rainForest");
    private static final TileType tundra
        = spec().getTileType("model.tile.tundra");
    private static final TileType tundraForest
        = spec().getTileType("model.tile.borealForest");


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
    public void testAdjustProductionAndManufacture() {
        Map map = getTestMap(savannahType);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony(1);
        game.setCurrentPlayer(colony.getOwner());
        Tile t = colony.getTile().getAdjacentTile(Direction.N);
        ColonyTile colTile = colony.getColonyTile(t);
        Building distillery = colony.getBuildingsForConsuming(sugarType)
            .get(0);

        // Put the unit to work producing sugar.
        Unit u = colony.getUnitList().get(0);
        u.setLocation(colTile);
        u.setWorkType(sugarType);

        // Check that the plan is to produce grain and sugar.
        ColonyPlan plan = new ColonyPlan(aiMain, colony);
        plan.create();
        assertEquals("Wrong primary raw material", grainType,
            plan.getPrimaryRawMaterial());
        assertEquals("Wrong secondary raw material", sugarType,
            plan.getSecondaryRawMaterial());

        // Check that sugar production is underway and stable.
        assertEquals("Wrong number of units in colony tile", 1,
            colTile.getUnitCount());
        assertEquals("Wrong number of units in distillery", 0,
            distillery.getUnitCount());
        assertEquals("Unit should be producing sugar", sugarType,
            u.getWorkType());
        plan.adjustProductionAndManufacture();
        assertEquals("Unit should not have been shifted", colTile,
            u.getLocation());

        // Simulate that enough sugar has been gathered, re-adjust and
        // check that the unit has moved to the distillery.
        colony.addGoods(sugarType, GoodsContainer.CARGO_SIZE);
        plan.adjustProductionAndManufacture();
        assertEquals("Wrong number of units in colony tile", 0,
            colTile.getUnitCount());
        assertEquals("Wrong number of units in distillery", 1,
            distillery.getUnitCount());
        assertEquals("Unit should be manufacturing rum", rumType,
            u.getWorkType());
        assertEquals("Unit should have been shifted", distillery,
            u.getLocation());
    }

    public void testBestImprovements() throws Exception {

        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);

        tile1.setType(savannah);
        assertEquals(plow, WorkLocationPlan.findBestTileImprovementType(tile1, grainType));
        assertEquals(plow, WorkLocationPlan.findBestTileImprovementType(tile1, sugarType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, lumberType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, oreType));

        tile1.setType(marsh);
        assertEquals(plow, WorkLocationPlan.findBestTileImprovementType(tile1, grainType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, sugarType));
        assertEquals(plow, WorkLocationPlan.findBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, lumberType));
        assertEquals(road, WorkLocationPlan.findBestTileImprovementType(tile1, oreType));

        tile1.setType(savannahForest);
        assertEquals(clearForest, WorkLocationPlan.findBestTileImprovementType(tile1, grainType));
        assertEquals(clearForest, WorkLocationPlan.findBestTileImprovementType(tile1, sugarType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, tobaccoType));
        assertEquals(road, WorkLocationPlan.findBestTileImprovementType(tile1, lumberType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, oreType));

        tile1.setType(hills);
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, grainType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, sugarType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, lumberType));
        assertEquals(road, WorkLocationPlan.findBestTileImprovementType(tile1, oreType));

        tile1.setType(arctic);
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, grainType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, sugarType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, lumberType));
        assertEquals(null, WorkLocationPlan.findBestTileImprovementType(tile1, oreType));

    }

}
