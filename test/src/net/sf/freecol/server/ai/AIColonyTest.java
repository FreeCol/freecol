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

import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class AIColonyTest extends FreeColTestCase {

    private static final BuildingType blacksmithsHouseType
        = spec().getBuildingType("model.building.blacksmithHouse");
    private static final BuildingType carpentersHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType warehouseType
        = spec().getBuildingType("model.building.warehouse");

    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType oreType
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType rumType
        = spec().getGoodsType("model.goods.rum");
    private static final GoodsType sugarType
        = spec().getGoodsType("model.goods.sugar");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final TileType forestType
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType savannahType
        = spec().getTileType("model.tile.savannah");
    private static final TileType mountainType
        = spec().getTileType("model.tile.mountains");

    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType lumberJackType
        = spec().getUnitType("model.unit.expertLumberJack");


    private LogBuilder lb = new LogBuilder(0); // dummy


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    // creates the special map for the tests
    // map will have:
    //    - a colony in (5,8) (built after)
    //    - a forest in (4,8) for lumber
    //    - a mountain in (6,8) for ore
    private Colony decorateMap(Game game, boolean full) {
        Map map = game.getMap();
        if (full) {
            map.getTile(4, 8).setType(forestType);
            map.getTile(6, 8).setType(mountainType);
        }

        // Needs a decent sized colony.
        Colony colony = getStandardColony(6);
        game.setCurrentPlayer(colony.getOwner());
        return colony;
    }

    // Add buildings until the next buildable requires tools
    private BuildableType getToolsBuilder(AIColony aiColony) {
        Colony colony = aiColony.getColony();
        for (BuildableType b : aiColony.getPlannedBuildableTypes()) {
            if (colony.canBuild(b)
                && b.getRequiredAmountOf(toolsType) > 0) return b;
            if (b instanceof BuildingType) {
                colony.addBuilding(new ServerBuilding(colony.getGame(), colony,
                        (BuildingType)b));
            } else if (b instanceof UnitType) {
                new ServerUnit(colony.getGame(), colony.getTile(),
                    colony.getOwner(), (UnitType)b);
            }
        }
        return null;
    }

    /**
     * Tests worker allocation regarding building tasks
     */
    public void testBuildersAllocation() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        Colony colony = decorateMap(game, true);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        final Building carpenterHouse
            = colony.getBuilding(carpentersHouseType);
        final Building blacksmithHouse
            = colony.getBuilding(blacksmithsHouseType);
        AIColony aiColony = aiMain.getAIColony(colony);
        ServerPlayer player = (ServerPlayer) colony.getOwner();

        // Add food so that the starvation avoidance is not triggered
        colony.addGoods(foodType, GoodsContainer.CARGO_SIZE);

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertEquals("Colony should be building lumber mill",
            lumberMillType, colony.getCurrentlyBuilding());
        assertTrue("Colony should be producing lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertTrue("Colony should have been assigned a carpenter",
            carpenterHouse.getUnitCount() > 0);
        assertTrue("Colony should be producing sugar",
            colony.getNetProductionOf(sugarType) > 0);

        // Simulate that enough hammers have been gathered, re-arrange
        // and re-check.
        colony.addGoods(hammersType,
            lumberMillType.getRequiredAmountOf(hammersType));
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertEquals("Colony should be building lumber mill",
            lumberMillType, colony.getCurrentlyBuilding());
        assertEquals("Colony does not need a carpenter", 0,
            carpenterHouse.getUnitCount());
        assertTrue("Colony should be producing sugar",
            colony.getNetProductionOf(sugarType) > 0);
        assertTrue("Colony should be producing rum",
            colony.getNetProductionOf(rumType) > 0);

        // Fill the warehouse with sugar, re-arrange and re-check.
        colony.addGoods(sugarType, GoodsContainer.CARGO_SIZE);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertEquals("Colony does not need a carpenter", 0,
            carpenterHouse.getUnitCount());
        TileType tileType = colony.getTile().getType();
        assertEquals("Colony should not be producing sugar, except center",
            tileType.getPotentialProduction(sugarType, colonistType),
            colony.getTotalProductionOf(sugarType));
        assertTrue("Colony should be producing rum",
            colony.getNetProductionOf(rumType) > 0);

        // Change to building something that needs tools.
        for (;;) {
            BuildableType toolsBuild = getToolsBuilder(aiColony);
            assertNotNull(toolsBuild);
            aiColony.propertyChange(null); // force rearranging workers
            aiColony.rearrangeWorkers(lb);
            if (colony.getCurrentlyBuilding() == toolsBuild) break;
        }

        assertEquals("Colony does not need a carpenter", 0,
            carpenterHouse.getUnitCount());
        assertTrue("Colony should be producing ore",
            colony.getTotalProductionOf(oreType) > 0);
        assertTrue("Colony should be producing tools",
            colony.getTotalProductionOf(toolsType) > 0);
    }

    /**
     * Tests worker allocation regarding building tasks when the
     * colony does not have tiles that provide the raw materials for
     * the build.
     */
    public void testBuildersAllocNoRawMatTiles() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        Colony colony = decorateMap(game, false);

        // The number needs to be high to ensure allocation
        final Building carpenterHouse
            = colony.getBuilding(carpentersHouseType);
        final Building blacksmithHouse
            = colony.getBuilding(blacksmithsHouseType);
        AIColony aiColony = aiMain.getAIColony(colony);
        ServerPlayer player = (ServerPlayer) colony.getOwner();

        // Add food so that the starvation avoidance is not triggered
        colony.addGoods(foodType, GoodsContainer.CARGO_SIZE);

        // We need to ensure that there are no tiles with production of
        // the raw materials.
        for (ColonyTile t : colony.getColonyTiles()) {
            Tile tile = t.getTile();
            assertEquals("The colony can not have tiles that produce lumber",
                0, tile.getPotentialProduction(lumberType, colonistType));
            assertEquals("The colony can not have tiles that produce ore",
                0, tile.getPotentialProduction(oreType, colonistType));
        }
        assertEquals("Colony has no lumber", 0,
            colony.getGoodsCount(lumberType));
        assertEquals("Colony has no ore", 0,
            colony.getGoodsCount(oreType));

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertFalse("Colony can not have a lumberjack, no lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertEquals("Colony can not have a carpenter, no lumber",
            0, carpenterHouse.getUnitCount());
        assertFalse("Colony can not have an ore miner, no ore",
            colony.getNetProductionOf(oreType) > 0);
        assertEquals("Colony can not have a blacksmith, no ore",
            0, blacksmithHouse.getUnitCount());
        assertEquals("Colony should be building lumber mill",
            lumberMillType, colony.getCurrentlyBuilding());

        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, GoodsContainer.CARGO_SIZE);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertEquals("Colony should be building lumber mill",
            lumberMillType, colony.getCurrentlyBuilding());
        assertFalse("Colony can not have a lumberjack, no lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertTrue("Colony should have a carpenter, has lumber in stock",
            carpenterHouse.getUnitCount() > 0);
        assertFalse("Colony can not have an ore miner, no ore",
            colony.getNetProductionOf(oreType) > 0);
        assertEquals("Colony can not have a blacksmith, no ore",
            0, blacksmithHouse.getUnitCount());

        // Simulate that enough hammers have been gathered, re-arrange
        // and re-check
        colony.addGoods(hammersType,
            lumberMillType.getRequiredAmountOf(hammersType));
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertFalse("Colony can not have a lumberjack, no lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertEquals("Colony does not need a carpenter",
            0, carpenterHouse.getUnitCount());
        assertFalse("Colony can not have an ore miner, no ore",
            colony.getNetProductionOf(oreType) > 0);
        assertEquals("Colony can not have a blacksmith, no ore",
            0, blacksmithHouse.getUnitCount());

        // Change to building something that needs tools.
        BuildableType toolsBuild = getToolsBuilder(aiColony);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertFalse("Colony can not have a lumberjack, no lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertEquals("Colony does not need a carpenter",
            0, carpenterHouse.getUnitCount());
        assertFalse("Colony can not have an ore miner, no ore",
            colony.getNetProductionOf(oreType) > 0);
        assertEquals("Colony can not have a blacksmith, no ore",
            0, blacksmithHouse.getUnitCount());
        assertEquals("Colony should be building tools-requirer", toolsBuild,
            colony.getCurrentlyBuilding());

        // Add ore to stock, re-arrange and re-check
        colony.addGoods(oreType, GoodsContainer.CARGO_SIZE);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertFalse("Colony can not have a lumberjack, no lumber",
            colony.getNetProductionOf(lumberType) > 0);
        assertEquals("Colony does not need a carpenter",
            0, carpenterHouse.getUnitCount());
        assertFalse("Colony can not have an ore miner, no ore",
            colony.getNetProductionOf(oreType) > 0);
        assertTrue("Colony should have a blacksmith, has ore in stock",
            blacksmithHouse.getUnitCount() > 0);
    }

    /**
     * Tests expert allocation regarding raw materials where there are
     * plenty already in stock.
     */
    public void testExpertAllocColonyHasEnoughRawMat() {
        Game game = ServerTestHelper.startServerGame(getTestMap(forestType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        FreeColTestUtils.ColonyBuilder builder
            = FreeColTestUtils.getColonyBuilder();
        Colony colony = builder.addColonist(lumberJackType).build();
        AIColony aiColony = aiMain.getAIColony(colony);
        game.setCurrentPlayer(colony.getOwner());

        ServerPlayer player = (ServerPlayer) colony.getOwner();
        assertEquals("Wrong number of units in colony", 1,
            colony.getUnitCount());
        Unit lumberjack = colony.getUnitList().get(0);

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertEquals("Lumberjack should have been assigned to collect lumber",
            lumberType, lumberjack.getWorkType());

        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, GoodsContainer.CARGO_SIZE);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(lb);

        assertTrue("Lumberjack should not collect lumber, in stock",
            lumberType != lumberjack.getWorkType());
    }

    public void testBestDefender() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        assertEquals(artilleryType, colony.getBestDefenderType());
    }
}
