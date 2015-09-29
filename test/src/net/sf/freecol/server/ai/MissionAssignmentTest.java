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

import java.util.Iterator;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class MissionAssignmentTest extends FreeColTestCase {

    private static final BuildingType stockadeType
        = spec().getBuildingType("model.building.stockade");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");

    final int MISSION_IMPOSSIBLE = Integer.MIN_VALUE;

    private LogBuilder lb = new LogBuilder(0); // dummy


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testImpossibleConditionsForTargetSelection() {
        Game game = ServerTestHelper.startServerGame(getCoastTestMap(plainsType));
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create attacking player and units
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        EuropeanAIPlayer aiDutch = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Tile tile1 = map.getTile(2, 2);
        Tile tile2 = map.getTile(2, 1);
        Unit soldier = new ServerUnit(game, tile1, dutch, veteranType);
        Unit friendlyColonist = new ServerUnit(game, tile2, dutch, colonistType);

        AIUnit aiUnit = aiMain.getAIUnit(soldier);
        assertNotNull(aiUnit);

        // Create defending player and unit
        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");

        Tile tile3 = map.getTile(1, 2);
        Unit enemyColonist = new ServerUnit(game, tile3, french, colonistType);

        Tile tile4 = map.getTile(12, 12); // in the water
        assertFalse("Tle should be water",tile4.isLand());

        Unit enemyGalleon = new ServerUnit(game, tile4, french, galleonType);
        //Make tests
        int turnsToReach = 1; // not important

        assertNotNull("Cannot attack own unit",
            UnitSeekAndDestroyMission.invalidReason(aiUnit, friendlyColonist));
        assertNotNull("Players are not at war",
            UnitSeekAndDestroyMission.invalidReason(aiUnit, enemyColonist));

        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);

        assertEquals("Unit should be able to attack land unit", null,
            UnitSeekAndDestroyMission.invalidReason(aiUnit, enemyColonist));
        assertNotNull("Land unit cannot attack naval unit",
            UnitSeekAndDestroyMission.invalidReason(aiUnit, enemyGalleon));
    }

    public void testIsTargetValidForSeekAndDestroy() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer incaPlayer = (ServerPlayer) game.getPlayerByNationId("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(incaPlayer);
        ServerPlayer dutchPlayer = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");

        Tile dutchUnitTile = map.getTile(9, 9);
        Tile braveUnitTile = map.getTile(9, 8);;

        Unit brave = new ServerUnit(game, braveUnitTile, incaPlayer, braveType);
        Unit soldier = new ServerUnit(game, dutchUnitTile, dutchPlayer, veteranType);

        Player.makeContact(incaPlayer, dutchPlayer);

        assertFalse("Target should NOT be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));

        incaPlayer.setTension(dutchPlayer, new Tension(Tension.Level.HATEFUL.getLimit()));
        assertTrue("Target should be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));

        incaPlayer.setStance(dutchPlayer, Stance.WAR);
        dutchPlayer.setStance(incaPlayer, Stance.WAR);
        assertTrue("Target should be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));
    }

    public void testAssignDefendSettlementMission() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        EuropeanAIPlayer aiDutch = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Tile tile1 = map.getTile(2, 2);
        Unit soldier = new ServerUnit(game, tile1, dutch, veteranType);

        AIUnit aiUnit = aiMain.getAIUnit(soldier);
        assertNotNull(aiUnit);

        // Add nearby colony in need of defense
        Tile colonyTile = map.getTile(2, 3);
        assertTrue(colonyTile != null);
        colonyTile.setExplored(dutch, true);
        Colony colony = FreeColTestUtils.getColonyBuilder().player(dutch).colonyTile(colonyTile).build();

        assertTrue(colonyTile.getSettlement() == colony);
        assertTrue(colony.getOwner() == dutch);
        assertTrue(colony.getUnitCount() == 1);
        aiUnit.setMission(null);
        assertEquals("DefendSettlementMission should be possible", null,
            DefendSettlementMission.invalidReason(aiUnit));
        assertEquals("DefendSettlementMission should work with colony", null,
            DefendSettlementMission.invalidReason(aiUnit, colony));
    }

    public void testSecureIndianSettlementMission() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer inca = (ServerPlayer) game.getPlayerByNationId("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(inca);
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");

        Tile settlementTile = map.getTile(9, 9);
        Tile adjacentTile = settlementTile.getNeighbourOrNull(Direction.N);
        assertTrue("Settlement tile should be land",
            settlementTile.isLand());
        assertTrue("Adjacent tile should be land",
            adjacentTile != null && adjacentTile.isLand());
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.player(inca)
            .settlementTile(settlementTile).initialBravesInCamp(8).build();
        assertEquals("One camp", 1, inca.getNumberOfSettlements());

        // Put one brave outside the camp, but in the settlement tile,
        // so that he may defend the settlement
        Unit braveOutside = new ServerUnit(game, settlementTile, inca, 
                                           braveType);
        braveOutside.setHomeIndianSettlement(camp);

        // Setup enemy units
        int enemyUnits = camp.getUnitCount() + 1;
        for (int i = 0; i < enemyUnits; i++) {
            new ServerUnit(game, adjacentTile, dutch, veteranType);
        }

        Iterator<Unit> campUnitIter = camp.getOwnedUnitsIterator();
        while(campUnitIter.hasNext()){
            Unit brave = campUnitIter.next();
            assertNotNull("Got null while getting the camps units", brave);
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull("Couldnt get the ai object for the brave", aiUnit);
            new UnitWanderHostileMission(aiMain, aiUnit);
            assertTrue("Should be UnitWanderHostileMission", 
                aiUnit.hasMission(UnitWanderHostileMission.class));
            assertEquals("Unit should be candidate for seek+destroy", null,
                UnitSeekAndDestroyMission.invalidReason(aiUnit));
            assertEquals("Unit should be candidate for defend", null,
                DefendSettlementMission.invalidReason(aiUnit));
        }

        inca.setStance(dutch, Stance.WAR);
        inca.setTension(dutch, new Tension(Tension.Level.HATEFUL.getLimit()));
        camp.setAlarm(dutch, inca.getTension(dutch));
        assertTrue("Indian player should be at war with dutch",
                   inca.getStance(dutch) == Stance.WAR);
        assertEquals("Wrong Indian player tension towards dutch",
                     Tension.Level.HATEFUL.getLimit(),
                     inca.getTension(dutch).getValue());
        aiInca.secureIndianSettlement(camp, lb);

        // Verify if a unit was assigned a UnitSeekAndDestroyMission
        boolean isSeekAndDestroyMission = false;
        for (Unit brave : inca.getUnits()) {
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull("Couldnt get aiUnit for players brave", aiUnit);
            assertNotNull("Unit missing mission", aiUnit.getMission());
            isSeekAndDestroyMission = aiUnit.hasMission(UnitSeekAndDestroyMission.class);
            if (isSeekAndDestroyMission) break;
        }
        assertTrue("A brave should have a UnitSeekAndDestroyMission",
                   isSeekAndDestroyMission);
    }

    /**
     * When searching for threats to a settlement, the indian player
     * should ignore naval threats, as he does not have naval power
     */
    public void testSecureIndianSettlementMissionIgnoreNavalThreat() {
        Game game = ServerTestHelper.startServerGame(getCoastTestMap(plainsType));
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        InGameController igc = ServerTestHelper.getInGameController();

        // Create player and unit
        ServerPlayer inca = (ServerPlayer) game.getPlayerByNationId("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(inca);
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");

        Tile settlementTile = map.getTile(9, 9);
        Tile seaTile = map.getTile(10, 9);
        assertTrue("Settlement tile should be land", settlementTile.isLand());
        assertFalse("Galleon tile should be ocean", seaTile.isLand());
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.player(inca)
            .settlementTile(settlementTile).initialBravesInCamp(10).build();
        ServerUnit galleon = new ServerUnit(game, seaTile, dutch, galleonType);
        int unitsInGalleon = 6;
        for (int i = 0; i < unitsInGalleon; i++) {
            ServerUnit artillery = new ServerUnit(game, settlementTile, dutch,
                artilleryType);
            igc.embarkUnit(dutch, artillery, galleon);
        }
        assertEquals("Wrong number of units onboard galleon", unitsInGalleon,
            galleon.getUnitCount());
        assertEquals("Galleon should be full", 0, galleon.getSpaceLeft());

        for (Unit brave : camp.getUnitList()) {
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull(aiUnit);
            new UnitWanderHostileMission(aiMain, aiUnit);

            assertTrue("No enemy units present",
                aiUnit.hasMission(UnitWanderHostileMission.class));
        }

        inca.setStance(dutch, Stance.WAR);
        inca.setTension(dutch, new Tension(Tension.Level.HATEFUL.getLimit()));
        assertTrue("Indian player should be at war with dutch",
                   inca.getStance(dutch) == Stance.WAR);
        assertEquals("Wrong Indian player tension towards dutch",
                     Tension.Level.HATEFUL.getLimit(),
                     inca.getTension(dutch).getValue());

        aiInca.abortInvalidMissions();
        aiInca.secureIndianSettlement(camp, lb);
        boolean seeking = false;
        for (Unit brave : inca.getUnits()) {
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull(aiUnit);
            if (aiUnit.hasMission(UnitSeekAndDestroyMission.class)) {
                seeking = true;
                break;
            }
        }
        assertFalse("Braves should not pursue naval units", seeking);
    }
}
