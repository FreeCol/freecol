/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyPlanTest extends FreeColTestCase {

    private static final BuildingType blacksmithShopType
        = spec().getBuildingType("model.building.blacksmithShop");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
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

    private static final UnitType farmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType servantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType convertType
        = spec().getUnitType("model.unit.indianConvert");
    private static final UnitType criminalType
        = spec().getUnitType("model.unit.pettyCriminal");
    private static final UnitType sugarPlanterType
        = spec().getUnitType("model.unit.masterSugarPlanter");

    private static final LogBuilder lb = new LogBuilder(0);

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
    
    /**
     * Should a plan produce a type of goods?
     *
     * @param plan The <code>ColonyPlan</code> to examine.
     * @param type The <code>GoodsType</code> to check.
     * @return True is there is a work location plan for the required goods
     *     type in the colony plan.
     */
    private boolean shouldProduce(ColonyPlan plan, GoodsType type) {
        List<WorkLocationPlan> plans = (type.isFoodType()) ? plan.getFoodPlans()
            : plan.getWorkPlans();
        for (WorkLocationPlan wlp : plans) {
            if (wlp.getGoodsType() == type) return true;
        }
        return false;
    }

    public void testReqLumberAndHammersForBuild() {
        Game game = ServerTestHelper.startServerGame(buildMap(true));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        Colony colony = getStandardColony(4);

        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        plan.update();

        assertEquals("The colony should plan to build the lumber mill",
            lumberMillType, plan.getBuildableTypes().get(0));
        assertTrue("The colony should plan to produce lumber",
            shouldProduce(plan, lumberType));
        assertTrue("The colony should plan to produce hammers",
            shouldProduce(plan, hammersType));
    }

    public void testReqOreAndToolsWithEnoughHammersForBuild() {
        Game game = ServerTestHelper.startServerGame(buildMap(true));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        Colony colony = getStandardColony(4);

        // Set colony to have enough hammers.  Should still require tools.
        ColonyPlan plan = new ColonyPlan(aiMain, colony);
        plan.update();
        colony.addGoods(hammersType,
            blacksmithShopType.getRequiredAmountOf(hammersType));
        colony.setCurrentlyBuilding(blacksmithShopType);
        plan.refine(blacksmithShopType, lb);

        assertTrue("The colony should plan to produce ore",
            shouldProduce(plan, oreType));
        assertTrue("The colony should plan to produce tools",
            shouldProduce(plan, toolsType));
        assertFalse("The colony should not produce hammers, has enough",
            shouldProduce(plan, hammersType));
    }

    /**
     * This test verifies behavior when the colony has no tiles that
     * provide the raw materials for the build, but has them in stock
     */
    public void testNoBuildRawMatTiles() {
        Game game = ServerTestHelper.startServerGame(buildMap(false));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        Colony colony = getStandardColony(4);

        // Add enough raw materials for build
        colony.addGoods(lumberType, GoodsContainer.CARGO_SIZE);
        colony.addGoods(oreType, GoodsContainer.CARGO_SIZE);

        ColonyPlan plan = new ColonyPlan(aiMain, colony);
        plan.update();
        plan.refine(colony.getCurrentlyBuilding(), lb);

        assertFalse("The colony can not produce lumber, none available",
            shouldProduce(plan, lumberType));
        assertTrue("The colony should produce hammers, has lumber in stock",
            shouldProduce(plan, hammersType));

        // Simulate that enough hammers have been gathered, re-plan building
        // with a tools requirement.
        plan.update();
        colony.addGoods(hammersType,
            blacksmithShopType.getRequiredAmountOf(hammersType));
        colony.setCurrentlyBuilding(blacksmithShopType);
        plan.refine(blacksmithShopType, lb);

        assertFalse("The colony should not produce hammers, has enough",
            shouldProduce(plan, hammersType));
        assertFalse("The colony can not produce ore, none available",
            shouldProduce(plan, oreType));
        assertTrue("The colony should produce tools, has ore in stock",
            shouldProduce(plan, toolsType));
    }

    public void testGetBestWorker() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        game.setCurrentPlayer(colony.getOwner());
        Player dutch = getGame().getPlayerByNationId("model.nation.dutch");
        List<Unit> units = new ArrayList<>();
        Unit servant = new ServerUnit(getGame(), colony.getTile(),
                                      dutch, servantType);
        units.add(servant);

        Building townHall = colony.getBuilding(townHallType);
        ColonyTile colonyTile = null;
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (!ct.isFull()) {
                colonyTile = ct;
                break;
            }
        }
        assertNotNull(colonyTile);
        assertNull(ColonyPlan.getBestWorker(colonyTile, sugarType, null));
        assertNull(ColonyPlan.getBestWorker(colonyTile, sugarType,
                                            new ArrayList<Unit>()));

        // Should always pick a singleton as long as it is productive.
        assertEquals(servant, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(servant, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(servant, ColonyPlan.getBestWorker(townHall, bellsType, units));
        assertNull(ColonyPlan.getBestWorker(townHall, grainType, units));

        // The criminal should be preferred to the servant when their
        // productivity is equal because it has lower skill.
        Unit criminal = new ServerUnit(getGame(), colony.getTile(), dutch, criminalType);
        units.add(criminal);
        assertEquals(criminal, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(criminal, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(servant, ColonyPlan.getBestWorker(townHall, bellsType, units));

        // The colonist will displace the servant in the town hall
        // because it is more productive, and elsewhere because it can
        // upgrade.
        Unit colonist1 = new ServerUnit(getGame(), colony.getTile(), dutch, colonistType);
        units.add(colonist1);
        assertEquals(colonist1, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(townHall, bellsType, units));

        // When there is a colonist with relevant experience it will win,
        // otherwise it will lose to the inexperienced colonist.
        Unit colonist2 = new ServerUnit(getGame(), colony.getTile(), dutch, colonistType);
        units.add(colonist2);
        colonist2.changeWorkType(sugarType);
        colonist2.modifyExperience(100);
        assertEquals(colonist2, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(townHall, bellsType, units));

        // colonist1 still has *less* experience to waste.  Experience
        // now causes a preference when production is zero, but only for
        // singletons.
        colonist1.changeWorkType(lumberType);
        colonist1.modifyExperience(80);
        assertNull(ColonyPlan.getBestWorker(colonyTile, lumberType, units));
        assertEquals(colonist2, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(townHall, bellsType, units));
        Unit convert = new ServerUnit(getGame(), colony.getTile(), dutch, convertType);
        units.add(convert);
        assertEquals(convert, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(convert, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        units.remove(convert);

        // The sugarPlanter will prevail in its expertise, and in the
        // town hall because it can not be upgraded.
        Unit sugarPlanter = new ServerUnit(getGame(), colony.getTile(), dutch, sugarPlanterType);
        units.add(sugarPlanter);
        assertEquals(sugarPlanter, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(colonist1, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(sugarPlanter, ColonyPlan.getBestWorker(townHall, bellsType, units));
        units.remove(sugarPlanter);

        // Similarly with the farmer.
        Unit farmer = new ServerUnit(getGame(), colony.getTile(), dutch, farmerType);
        units.add(farmer);
        assertEquals(colonist2, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(farmer, ColonyPlan.getBestWorker(colonyTile, grainType, units));
        assertEquals(farmer, ColonyPlan.getBestWorker(townHall, bellsType, units));

        // But with both we can not expect to tell who wins the town hall.
        units.add(convert);
        units.add(sugarPlanter);

        assertEquals(sugarPlanter, ColonyPlan.getBestWorker(colonyTile, sugarType, units));
        assertEquals(farmer, ColonyPlan.getBestWorker(colonyTile, grainType, units));
    }

    public void testBestImprovements() throws Exception {
        Map map = getTestMap(plains);
        Game game = getStandardGame();
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);

        tile1.setType(savannah);
        assertEquals(plow, TileImprovementPlan.getBestTileImprovementType(tile1, grainType));
        assertEquals(plow, TileImprovementPlan.getBestTileImprovementType(tile1, sugarType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, lumberType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, oreType));

        tile1.setType(marsh);
        assertEquals(plow, TileImprovementPlan.getBestTileImprovementType(tile1, grainType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, sugarType));
        assertEquals(plow, TileImprovementPlan.getBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, lumberType));
        assertEquals(road, TileImprovementPlan.getBestTileImprovementType(tile1, oreType));

        tile1.setType(savannahForest);
        assertEquals(clearForest, TileImprovementPlan.getBestTileImprovementType(tile1, grainType));
        assertEquals(clearForest, TileImprovementPlan.getBestTileImprovementType(tile1, sugarType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, tobaccoType));
        assertEquals(road, TileImprovementPlan.getBestTileImprovementType(tile1, lumberType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, oreType));

        tile1.setType(hills);
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, grainType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, sugarType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, lumberType));
        assertEquals(road, TileImprovementPlan.getBestTileImprovementType(tile1, oreType));

        tile1.setType(arctic);
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, grainType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, sugarType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, tobaccoType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, lumberType));
        assertEquals(null, TileImprovementPlan.getBestTileImprovementType(tile1, oreType));
    }
}
