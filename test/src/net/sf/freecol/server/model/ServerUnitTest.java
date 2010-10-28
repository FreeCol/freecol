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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.util.test.MockModelController;
import net.sf.freecol.util.test.MockPseudoRandom;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class ServerUnitTest extends FreeColTestCase {

    private static final EquipmentType horsesType
        = spec().getEquipmentType("model.equipment.horses");
    private static final EquipmentType toolsType
        = spec().getEquipmentType("model.equipment.tools");

    private static final GoodsType foodType
        = spec().getGoodsType("model.goods.grain");

    private static final TileImprovementType road
        = spec().getTileImprovementType("model.improvement.road");
    private static final TileImprovementType plow
        = spec().getTileImprovementType("model.improvement.plow");
    private static final TileImprovementType clear
        = spec().getTileImprovementType("model.improvement.clearForest");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType savannahForest
        = spec().getTileType("model.tile.tropicalForest");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType soldierType
        = spec().getUnitType("model.unit.veteranSoldier");


    public void testToggleHorses() {
        Map map = getTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        ServerUnit scout = new ServerUnit(game, tile1, dutch, colonistType,
                                          UnitState.ACTIVE);

        // make sure unit has all moves left
        ServerTestHelper.newTurn((ServerPlayer)scout.getOwner());

        assertEquals(scout.getInitialMovesLeft(), scout.getMovesLeft());
        int colonistMoves = scout.getMovesLeft();
        scout.changeEquipment(horsesType, 1);

        ServerTestHelper.newTurn((ServerPlayer)scout.getOwner());

        assertTrue("Scout should have more moves than a colonist",
                   scout.getMovesLeft() > colonistMoves);
        scout.changeEquipment(horsesType, -1);

        ServerTestHelper.newTurn((ServerPlayer)scout.getOwner());

        assertEquals(scout.getMovesLeft(), colonistMoves);
    }

    /**
     * Test Plowing with a hardy pioneer
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {
        Map map = getTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);

        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        Tile plain = map.getTile(5, 8);
        plain.setExploredBy(dutch, true);
        plain.setOwner(dutch);

        ServerUnit hardyPioneer = new ServerUnit(game, plain, dutch,
                                                 pioneerType,
                                                 UnitState.ACTIVE);

        // Before
        assertFalse(plain.hasImprovement(plow));
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);

        TileImprovement plowImprovement
            = new TileImprovement(game, plain, plow);
        plain.add(plowImprovement);
        hardyPioneer.work(plowImprovement);

        assertFalse(plain.hasImprovement(plow));
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.IMPROVING, hardyPioneer.getState());
        assertEquals(5, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);

        // Advance to finish
        while (hardyPioneer.getWorkLeft() > 0) {
            ServerTestHelper.newTurn((ServerPlayer)dutch);
        }

        // Pioneer finished work
        assertTrue(plain.hasImprovement(plow));
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
    }

    public void testColonyProfitFromEnhancement() {
        Map map = getTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);

        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
        Tile plain58 = map.getTile(5, 8);

        //assertEquals(2, dutch.getDifficulty().getIndex());
        //assertEquals("model.difficulty.medium", dutch.getDifficulty().getId());
        assertEquals(6, spec().getIntegerOption("model.option.badGovernmentLimit").getValue());
        assertEquals(10, spec().getIntegerOption("model.option.veryBadGovernmentLimit").getValue());

        // Found colony on 6,8
        ServerUnit soldier = new ServerUnit(game, map.getTile(6, 8), dutch,
                                            soldierType, UnitState.ACTIVE);

        ServerColony colony = new ServerColony(game, dutch, "New Amsterdam",
                                               soldier.getTile());
        GoodsType foodType = spec().getGoodsType("model.goods.food");

        soldier.setWorkType(foodType);
        nonServerBuildColony(soldier, colony);
        soldier.setLocation(colony.getColonyTile(plain58));
        ServerUnit hardyPioneer = new ServerUnit(game, plain58, dutch,
                                                 pioneerType,
                                                 UnitState.ACTIVE);

        // Before
        assertEquals(0, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(0, colony.getProductionBonus());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        ServerTestHelper.newTurn((ServerPlayer)dutch);

        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(8, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(0, colony.getProductionBonus());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        TileImprovement plowImprovement
            = new TileImprovement(game, plain58, plow);
        plain58.add(plowImprovement);
        hardyPioneer.work(plowImprovement);

        int n = 0;
        while (hardyPioneer.getWorkLeft() > 0) {
            ServerTestHelper.newTurn((ServerPlayer)dutch);
            n++;
        }

        assertEquals(true, plain58.hasImprovement(plow));
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(2, colony.getFoodConsumption());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + n * 8, colony.getGoodsCount(foodType));

        // Advance last turn
        ServerTestHelper.newTurn((ServerPlayer)dutch);

        assertEquals(true, plain58.hasImprovement(plow));
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
        Map map = getTestMap(savannahForest);
        Game game = ServerTestHelper.startServerGame(map);

        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        ServerUnit hardyPioneer1 = new ServerUnit(game, plain, dutch,
                                                  pioneerType,
                                                  UnitState.ACTIVE);
        ServerUnit hardyPioneer2 = new ServerUnit(game, plain, dutch,
                                                  pioneerType,
                                                  UnitState.ACTIVE);
        ServerUnit hardyPioneer3 = new ServerUnit(game, plain, dutch,
                                                  pioneerType,
                                                  UnitState.ACTIVE);

        // Before
        assertEquals(false, plain.hasRoad());
        assertEquals(3, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(100, hardyPioneer1.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer1.getState());
        assertEquals(3, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(100, hardyPioneer2.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer2.getState());

        // Now do it
        plain.setOwner(dutch);
        TileImprovement roadImprovement = new TileImprovement(game, plain, road);
        TileImprovement clearImprovement = new TileImprovement(game, plain, clear);
        plain.add(roadImprovement);
        plain.add(clearImprovement);
        hardyPioneer1.work(roadImprovement);
        hardyPioneer2.work(roadImprovement);
        hardyPioneer3.work(clearImprovement);
        assertEquals(6, hardyPioneer1.getWorkLeft());
        assertEquals(6, hardyPioneer2.getWorkLeft());
        assertEquals(8, hardyPioneer3.getWorkLeft());

        while (roadImprovement.getTurnsToComplete() > 0) {
            ServerTestHelper.newTurn((ServerPlayer)dutch);
        }

        // After: both pioneers building road have used up their tools
        assertTrue(plain.hasRoad());
        assertTrue(roadImprovement.isComplete());
        assertFalse(clearImprovement.isComplete());

        //assertEquals(0, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer1.getState());

        //assertEquals(0, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer2.getState());

        assertEquals(180, 20 * (hardyPioneer1.getEquipmentCount(toolsType)
                                + hardyPioneer2.getEquipmentCount(toolsType)));

        // Pioneer clearing forest is not affected
        assertEquals(3, hardyPioneer3.getMovesLeft());
        assertEquals(4, hardyPioneer3.getWorkLeft());
        assertEquals(UnitState.IMPROVING, hardyPioneer3.getState());
        assertEquals(100, hardyPioneer3.getEquipmentCount(toolsType) * 20);

        // Finish
        while (hardyPioneer3.getWorkLeft() > 0) {
            ServerTestHelper.newTurn((ServerPlayer)dutch);
        }

        assertTrue(clearImprovement.isComplete());
        assertEquals(0, hardyPioneer3.getMovesLeft());
        assertEquals(-1, hardyPioneer3.getWorkLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer3.getState());
        assertEquals(80, hardyPioneer3.getEquipmentCount(toolsType) * 20);
    }

    public void testUnitGetsExperienceThroughWork() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);

        Colony colony = getStandardColony();
        Unit colonist = colony.getRandomUnit();

        assertEquals("Colonist should not have any experience",
                     0, colonist.getExperience());

        // colonist either in building or colony work tile
        WorkLocation loc = colonist.getWorkLocation();
        if (loc == null) loc = colonist.getWorkTile();

        // produces goods
        ServerTestHelper.newTurn((ServerPlayer)colonist.getOwner());

        assertTrue("Colonist should have gained some experience",
                   colonist.getExperience() > 0);
    }

    public void testUnitPromotionWorkingInWorkTile() {
        Map map = getTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);

        Colony colony = getStandardColony();
        assertTrue("Colony should only have 1 colonist for test setup",
                   colony.getUnitCount() == 1);

        Unit colonist = colony.getRandomUnit();
        String errMsg = "Error setting test, colonist should not be an expert";
        assertTrue(errMsg, colonist.getType() == colonistType);

        // set colonist as farmer
        ColonyTile workTile = colony.getColonyTile(colony.getTile().getNeighbourOrNull(Direction.N));
        colonist.setLocation(workTile);
        colonist.setWorkType(foodType);
        assertEquals("Wrong work allocation", foodType,colonist.getWorkType());

        // set some experience
        int expectXP = 10;
        colonist.modifyExperience(expectXP);
        assertEquals("Wrong colonist experience",
                     expectXP, colonist.getExperience());

        // We need a deterministic random
        List<Integer> setValues = new ArrayList<Integer>();
        setValues.add(1);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues, true);
        ServerTestHelper.setRandom(mockRandom);

        // Verify initial state
        boolean isExpert = colonist.getType() == expertFarmerType;
        assertFalse("Unit should not be an expert", isExpert);

        // Make upgrade
        ServerTestHelper.newTurn((ServerPlayer)colonist.getOwner());
        assertTrue(colonist.getExperience() > expectXP);

        // verify upgrade
        isExpert = colonist.getType() == expertFarmerType;
        assertTrue("Unit should now be an expert", isExpert);
    }
}
