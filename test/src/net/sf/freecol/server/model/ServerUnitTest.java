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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


public class ServerUnitTest extends FreeColTestCase {

    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");

    private static final TileImprovementType road
        = spec().getTileImprovementType("model.improvement.road");
    private static final TileImprovementType plow
        = spec().getTileImprovementType("model.improvement.plow");
    private static final TileImprovementType clear
        = spec().getTileImprovementType("model.improvement.clearForest");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType savannah
        = spec().getTileType("model.tile.savannah");
    private static final TileType savannahForest
        = spec().getTileType("model.tile.tropicalForest");

    private static final Role pioneerRole
        = spec().getRole("model.role.pioneer");
    private static final Role scoutRole
        = spec().getRole("model.role.scout");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType soldierType
        = spec().getUnitType("model.unit.veteranSoldier");


    /**
     * Does a tile have a completed improvement of the given type?
     *
     * @param tile The <code>Tile</code> to check.
     * @param type The <code>TileImprovementType</code> to check.
     * @return True if this <code>Tile</code> has the improvement type and
     *      it is complete.
     */
    private static boolean hasImprovement(Tile tile, TileImprovementType type) {
        if (type.changeContainsTarget(tile.getType())) {
            return true;
        } else if (tile.getTileItemContainer() != null) {
            return tile.getTileItemContainer().hasImprovement(type);
        }
        return false;
    }

    public void testToggleHorses() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile tile1 = game.getMap().getTile(5, 8);
        tile1.setExplored(dutch, true);
        ServerUnit scout = new ServerUnit(game, tile1, dutch, colonistType);

        // make sure unit has all moves left
        ServerTestHelper.newTurn();

        assertEquals(scout.getInitialMovesLeft(), scout.getMovesLeft());
        int colonistMoves = scout.getMovesLeft();
        scout.changeRole(scoutRole, 1);

        ServerTestHelper.newTurn();

        assertTrue("Scout should have more moves than a colonist",
                   scout.getMovesLeft() > colonistMoves);
        scout.changeRole(spec().getDefaultRole(), 0);

        ServerTestHelper.newTurn();

        assertEquals(scout.getMovesLeft(), colonistMoves);
    }

    /**
     * Test Plowing with a hardy pioneer
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Tile plain = game.getMap().getTile(5, 8);
        plain.setExplored(dutch, true);
        plain.setOwner(dutch);

        ServerUnit hardyPioneer = new ServerUnit(game, plain, dutch,
                                                 pioneerType);
        hardyPioneer.setRoleCount(1);

        // Before
        assertFalse(hasImprovement(plain, plow));
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(pioneerRole, hardyPioneer.getRole());

        //TileImprovement plowImprovement
        //    = new TileImprovement(game, plain, plow);
        //plain.add(plowImprovement);
        igc.changeWorkImprovementType(dutch, hardyPioneer, plow);

        assertFalse(hasImprovement(plain, plow));
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.UnitState.IMPROVING, hardyPioneer.getState());
        assertEquals(5, hardyPioneer.getWorkLeft());
        assertEquals(pioneerRole, hardyPioneer.getRole());

        // Advance to finish
        while (hardyPioneer.getWorkLeft() > 0) {
            ServerTestHelper.newTurn();
        }

        // Pioneer finished work
        assertTrue(hasImprovement(plain, plow));
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(spec().getDefaultRole(), hardyPioneer.getRole());
    }

    public void testColonyProfitFromEnhancement() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Map map = game.getMap();
        map.getTile(5, 8).setExplored(dutch, true);
        map.getTile(6, 8).setExplored(dutch, true);
        Tile plain58 = map.getTile(5, 8);

        //assertEquals(2, dutch.getDifficulty().getIndex());
        //assertEquals("model.difficulty.medium", dutch.getDifficulty().getId());
        assertEquals(6, spec().getInteger(GameOptions.BAD_GOVERNMENT_LIMIT));
        assertEquals(10, spec().getInteger(GameOptions.VERY_BAD_GOVERNMENT_LIMIT));

        // Found colony on 6,8
        ServerUnit soldier = new ServerUnit(game, map.getTile(6, 8), dutch,
                                            soldierType);

        ServerColony colony = new ServerColony(game, dutch, "New Amsterdam",
                                               soldier.getTile());
        dutch.addSettlement(colony);

        soldier.changeWorkType(foodType);
        nonServerBuildColony(soldier, colony);
        soldier.setLocation(colony.getColonyTile(plain58));
        ServerUnit hardyPioneer = new ServerUnit(game, plain58, dutch,
                                                 pioneerType);

        // Before
        assertEquals(0, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertFalse(hasImprovement(plain58, plow));
        assertEquals(0, colony.getProductionBonus());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        ServerTestHelper.newTurn();

        assertFalse(hasImprovement(plain58, plow));
        assertEquals(8, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(0, colony.getProductionBonus());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        //TileImprovement plowImprovement
        //    = new TileImprovement(game, plain58, plow);
        //plain58.add(plowImprovement);
        igc.changeWorkImprovementType(dutch, hardyPioneer, plow);

        int n = 0;
        while (hardyPioneer.getWorkLeft() > 0) {
            ServerTestHelper.newTurn();
            n++;
        }
        colony.invalidateCache();

        assertTrue(hasImprovement(plain58, plow));
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(2, colony.getFoodConsumption());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + n * 8, colony.getGoodsCount(foodType));

        // In game, this should happen via a Tile update
        colony.invalidateCache();
        // Advance last turn
        ServerTestHelper.newTurn();

        assertTrue(hasImprovement(plain58, plow));
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(8 + n * 8 + 9, colony.getGoodsCount(foodType));
    }

    /**
     * Test Building a road with a hardy pioneer.
     *
     * The road is available directly, but the pioneer can only move on the next
     * turn.
     *
     */
    public void testDoAssignedWorkHardyPioneerBuildRoad() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahForest));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Map map = game.getMap();
        Tile tile = map.getTile(5, 8);
        map.getTile(5, 8).setExplored(dutch, true);

        ServerUnit hardyPioneer1 = new ServerUnit(game, tile, dutch,
                                                  pioneerType, pioneerRole);
        ServerUnit hardyPioneer2 = new ServerUnit(game, tile, dutch,
                                                  pioneerType, pioneerRole);
        ServerUnit hardyPioneer3 = new ServerUnit(game, tile, dutch,
                                                  pioneerType, pioneerRole);

        // Before
        assertEquals(false, tile.hasRoad());
        assertEquals(3, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(5, hardyPioneer1.getRoleCount());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer1.getState());
        assertEquals(3, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(5, hardyPioneer2.getRoleCount());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer2.getState());

        // Now do it
        tile.setOwner(dutch);
        igc.changeWorkImprovementType(dutch, hardyPioneer1, road);
        igc.changeWorkImprovementType(dutch, hardyPioneer2, road);
        igc.changeWorkImprovementType(dutch, hardyPioneer3, clear);
        assertEquals(6, hardyPioneer1.getWorkLeft());
        assertEquals(6, hardyPioneer2.getWorkLeft());
        assertEquals(8, hardyPioneer3.getWorkLeft());

        TileImprovement roadImprovement = tile.getRoad();
        while (roadImprovement.getTurnsToComplete() > 0) {
            ServerTestHelper.newTurn();
        }

        // After: both pioneers building road have used up their tools
        assertTrue(tile.hasRoad());
        assertTrue(roadImprovement.isComplete());
        assertEquals(savannahForest, tile.getType());

        //assertEquals(0, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer1.getState());

        //assertEquals(0, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer2.getState());

        assertEquals(9,
            hardyPioneer1.getRoleCount() + hardyPioneer2.getRoleCount());

        // Pioneer clearing forest is not affected
        assertEquals(3, hardyPioneer3.getMovesLeft());
        assertEquals(4, hardyPioneer3.getWorkLeft());
        assertEquals(Unit.UnitState.IMPROVING, hardyPioneer3.getState());
        assertEquals(5, hardyPioneer3.getRoleCount());

        // Finish
        while (hardyPioneer3.getWorkLeft() > 0) {
            ServerTestHelper.newTurn();
        }

        assertEquals(savannah, tile.getType());
        assertEquals(0, hardyPioneer3.getMovesLeft());
        assertEquals(-1, hardyPioneer3.getWorkLeft());
        assertEquals(Unit.UnitState.ACTIVE, hardyPioneer3.getState());
        assertEquals(4, hardyPioneer3.getRoleCount());
    }

    public void testUnitGetsExperienceThroughWork() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Colony colony = getStandardColony();
        Unit colonist = colony.getUnitList().get(0);

        assertEquals("Colonist should not have any experience",
                     0, colonist.getExperience());
        assertEquals("Colonist should be free colonist",
                     colonistType, colonist.getType());

        // make sure colonist is on a colony tile
        WorkLocation loc = colonist.getWorkLocation();
        if (!(loc instanceof ColonyTile)) {
            colonist.setLocation(colony.getWorkLocationFor(colonist, foodType));
        }
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertNotNull(colonist.getWorkType());
        assertTrue(colonist.getWorkType().isFoodType());

        // produces goods
        ServerTestHelper.newTurn();

        assertTrue("Colonist should have gained some experience",
                   colonist.getExperience() > 0);
    }

    public void testUnitPromotionWorkingInWorkTile() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));

        Colony colony = getStandardColony();
        assertTrue("Colony should only have 1 colonist for test setup",
                   colony.getUnitCount() == 1);

        Unit colonist = colony.getUnitList().get(0);
        String errMsg = "Error setting test, colonist should not be an expert";
        assertTrue(errMsg, colonist.getType() == colonistType);

        // set colonist as farmer
        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        assertTrue(colony.getOwner().canOwnTile(tile));
        ColonyTile colonyTile = colony.getColonyTile(tile);
        if (!colonyTile.isEmpty()) {
            colonyTile.getUnitList().get(0).setLocation(colony.getBuilding(townHallType));
        }
        assertTrue(colonyTile.canBeWorked());
        colonist.setLocation(colonyTile);
        colonist.changeWorkType(grainType);
        assertEquals("Wrong work allocation",
                     grainType, colonist.getWorkType());
        assertEquals(colonyTile.getUnitList().get(0), colonist);
        // Will colonist gain experience?
        assertTrue(colonyTile.getTotalProductionOf(colonist.getWorkType()) > 0);
        // Can colonist be upgraded
        UnitType learn = spec().getExpertForProducing(colonist.getWorkType());
        assertNotNull(learn);
        assertTrue(learn != colonist.getType());
        assertTrue(colonist.getType().canBeUpgraded(learn,
                                                    ChangeType.EXPERIENCE));

        // set some experience
        int expectXP = 10;
        colonist.modifyExperience(expectXP);
        assertEquals("Wrong colonist experience",
                     expectXP, colonist.getExperience());

        // We need a deterministic random
        List<Integer> setValues = new ArrayList<>();
        setValues.add(1);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues, true);
        ServerTestHelper.setRandom(mockRandom);

        // Verify initial state
        boolean isExpert = colonist.getType() == expertFarmerType;
        assertFalse("Unit should not be an expert", isExpert);

        // Make upgrade
        ServerTestHelper.newTurn();
        assertTrue(colonist.getExperience() > expectXP);

        // verify upgrade
        isExpert = colonist.getType() == expertFarmerType;
        assertTrue("Unit should now be an expert", isExpert);
    }

    public void testExposeResource() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahForest));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Map map = game.getMap();
        Tile tile = map.getTile(5, 8);
        tile.setOwner(dutch);
        tile.setExplored(dutch, true);

        // Almost clear the tile
        Role pioneerRole = spec().getRole("model.role.pioneer");
        ServerUnit hardyPioneer = new ServerUnit(game, tile, dutch,
                                                 pioneerType, pioneerRole);
        //TileImprovement clearImprovement
        //    = new TileImprovement(game, tile, clear);
        //tile.add(clearImprovement);
        igc.changeWorkImprovementType(dutch, hardyPioneer, clear);

        // Verify initial state
        assertEquals(8, hardyPioneer.getWorkLeft());
        assertEquals(savannahForest, tile.getType());
        assertFalse(tile.hasResource());

        // Almost finish clearing
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();

        // We need a deterministic random
        List<Integer> setValues = new ArrayList<>();
        setValues.add(1);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues, true);
        ServerTestHelper.setRandom(mockRandom);

        // Finish clearing
        ServerTestHelper.newTurn();

        // Verify clearing succeeded and has revealed a resource
        assertEquals(savannah, tile.getType());
        assertTrue(tile.hasResource());
    }

    public void testUnitLumberDelivery() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahForest));
        InGameController igc = ServerTestHelper.getInGameController();
        Colony colony = getStandardColony(3);
        ServerPlayer player = (ServerPlayer)colony.getOwner();
        Tile tile = colony.getTile();
        
        // Set up a hardy pioneer to clear the colony tile
        Role pioneerRole = spec().getRole("model.role.pioneer");
        ServerUnit hardyPioneer = new ServerUnit(game, tile, player,
                                                 pioneerType, pioneerRole);
        igc.changeWorkImprovementType(player, hardyPioneer, clear);

        // Verify initial state
        assertEquals(8, hardyPioneer.getWorkLeft());
        assertEquals(savannahForest, tile.getType());
        assertEquals(colony, tile.getOwningSettlement());
        
        // Almost finish clearing
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();

        // Lumber should be delivered on this turn
        int lumber = colony.getGoodsCount(lumberType);
        ServerTestHelper.newTurn();
        assertEquals("Lumber delivery with hardy pioneer", lumber + 20 * 2,
                     colony.getGoodsCount(lumberType));

        // Upgrade to lumber mill
        assertEquals(0,
            colony.getModifiers(Modifier.TILE_TYPE_CHANGE_PRODUCTION).size());
        colony.getBuilding(carpenterHouseType).upgrade();
        assertEquals(1,
            colony.getModifiers(Modifier.TILE_TYPE_CHANGE_PRODUCTION).size());

        // Almost clear another tile
        Tile tile2 = tile.getNeighbourOrNull(Direction.N);
        assertEquals(colony, tile2.getOwningSettlement());
        hardyPioneer.setLocation(tile2);
        hardyPioneer.setMovesLeft(1);
        igc.changeWorkImprovementType(player, hardyPioneer, clear);
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();

        // Lumber should be delivered on this turn
        lumber = colony.getGoodsCount(lumberType);
        ServerTestHelper.newTurn();
        assertEquals("Lumber delivered with hardy pioneer and mill",
                     lumber + 20 * 2 * 3, colony.getGoodsCount(lumberType));
    }
}
