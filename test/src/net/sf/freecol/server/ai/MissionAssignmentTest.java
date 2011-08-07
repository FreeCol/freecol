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

import java.util.Iterator;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
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


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testImpossibleConditionsForTargetSelection() {
        Map map = getCoastTestMap(plainsType);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create attacking player and units
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        EuropeanAIPlayer aiDutch = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Tile tile1 = map.getTile(2, 2);
        Tile tile2 = map.getTile(2, 1);
        Unit soldier = new ServerUnit(game, tile1, dutch, veteranType, UnitState.ACTIVE);
        Unit friendlyColonist = new ServerUnit(game, tile2, dutch, colonistType, UnitState.ACTIVE);

        AIUnit aiUnit = aiMain.getAIUnit(soldier);
        assertNotNull(aiUnit);

        // Create defending player and unit
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");

        Tile tile3 = map.getTile(1, 2);
        Unit enemyColonist = new ServerUnit(game, tile3, french, colonistType, UnitState.ACTIVE);

        Tile tile4 = map.getTile(12, 12); // in the water
        assertFalse("Tle should be water",tile4.isLand());

        Unit enemyGalleon = new ServerUnit(game, tile4, french, galleonType, UnitState.ACTIVE);
        //Make tests
        int turnsToReach = 1; // not important

        assertTrue("Cannot attack own unit", aiDutch.getUnitSeekAndDestroyMissionValue(soldier, friendlyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
        assertTrue("Players are not at war", aiDutch.getUnitSeekAndDestroyMissionValue(soldier, enemyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);

        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);

        assertFalse("Unit should be able to attack land unit", aiDutch.getUnitSeekAndDestroyMissionValue(soldier, enemyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
        assertTrue("Land unit cannot attack naval unit", aiDutch.getUnitSeekAndDestroyMissionValue(soldier, enemyGalleon.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
    }

    public void testIsTargetValidForSeekAndDestroy() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer incaPlayer = (ServerPlayer) game.getPlayer("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(incaPlayer);
        ServerPlayer dutchPlayer = (ServerPlayer) game.getPlayer("model.nation.dutch");

        Tile dutchUnitTile = map.getTile(9, 9);
        Tile braveUnitTile = map.getTile(9, 8);;

        Unit brave = new ServerUnit(game, braveUnitTile, incaPlayer, braveType, UnitState.ACTIVE);
        Unit soldier = new ServerUnit(game, dutchUnitTile, dutchPlayer, veteranType, UnitState.ACTIVE);

        Player.makeContact(incaPlayer, dutchPlayer);

        assertFalse("Target should NOT be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));

        incaPlayer.setTension(dutchPlayer, new Tension(Tension.Level.HATEFUL.getLimit()));
        assertTrue("Target should be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));

        incaPlayer.setStance(dutchPlayer, Stance.WAR);
        dutchPlayer.setStance(incaPlayer, Stance.WAR);
        assertTrue("Target should be valid for UnitSeekAndDestroyMission", aiInca.isTargetValidForSeekAndDestroy(brave, soldier.getTile()));
    }

    public void testGiveMilitaryMission() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create attacking player and units
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        EuropeanAIPlayer aiDutch = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Tile tile1 = map.getTile(2, 2);
        Unit soldier = new ServerUnit(game, tile1, dutch, veteranType, UnitState.ACTIVE);

        AIUnit aiUnit = aiMain.getAIUnit(soldier);
        assertNotNull(aiUnit);

        // Create defending player
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");

        //Make tests

        aiDutch.giveMilitaryMission(aiUnit);

        boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

        assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);

        // Add non-hostile unit
        Tile tile2 = map.getTile(2, 1);
        new ServerUnit(game, tile2, french, colonistType, UnitState.ACTIVE);

        // reassign mission and check
        aiDutch.giveMilitaryMission(aiUnit);
        isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

        assertTrue("No hostile units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);

        // Make unit hostile by changing stance to War,reassign mission and check
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
        aiDutch.giveMilitaryMission(aiUnit);

        isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
        boolean isSeekAndDestroyMission = aiUnit.getMission() instanceof UnitSeekAndDestroyMission;

        assertFalse("Enemy unit is present, should not be UnitWanderHostileMission", isUnitWanderHostileMission);
        assertTrue("Enemy unit is present, should be UnitSeekAndDestroyMission", isSeekAndDestroyMission);
    }

    public void testAssignDefendSettlementMission() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        EuropeanAIPlayer aiDutch = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Tile tile1 = map.getTile(2, 2);
        Unit soldier = new ServerUnit(game, tile1, dutch, veteranType, UnitState.ACTIVE);

        AIUnit aiUnit = aiMain.getAIUnit(soldier);
        assertNotNull(aiUnit);

        aiDutch.giveMilitaryMission(aiUnit);

        boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

        assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);

        // Add nearby colony in need of defense
        Tile colonyTile = map.getTile(2, 3);
        assertTrue(colonyTile != null);
        colonyTile.setExploredBy(dutch, true);
        Colony colony = FreeColTestUtils.getColonyBuilder().player(dutch).colonyTile(colonyTile).build();

        assertTrue(colonyTile.getSettlement() == colony);
        assertTrue(colony.getOwner() == dutch);
        assertTrue(colony.getUnitCount() == 1);

        int turnsToColony = 1;
        // reassign mission and check

        aiDutch.giveMilitaryMission(aiUnit);

        boolean missionIsValid = aiDutch.getDefendColonyMissionValue(soldier, colony, turnsToColony) > 0;
        isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
        boolean isDefendSettlementMission = aiUnit.getMission() instanceof DefendSettlementMission;

        assertTrue("DefendColonyMission should be possible",missionIsValid);
        assertFalse("Colony in need of defense, should no longer be UnitWanderHostileMission", isUnitWanderHostileMission);
        assertTrue("Colony in need of defense, should be DefendSettlementMission", isDefendSettlementMission);

        //TODO: Increase defense to a point where the soldier does best to patrol

        // Right now (08/05/2008), the algorithm that calculates the value of a colony defense mission
        //does not take into account the various levels of fortification, and requires too many units to
        //guard the colony
    }

    public void testSecureIndianSettlementMission() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create player and unit
        ServerPlayer inca = (ServerPlayer) game.getPlayer("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(inca);
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        Tile settlementTile = map.getTile(9, 9);
        Tile adjacentTile = settlementTile.getNeighbourOrNull(Direction.N);
        assertTrue("Settlement tile should be land",settlementTile.isLand());
        assertTrue("Adjacent tile should be land", adjacentTile != null && adjacentTile.isLand());
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.player(inca).settlementTile(settlementTile).build();

        // put one brave outside the camp, but in the settlement tile, so that he may defend the settlement
        Unit braveOutside = new ServerUnit(game, settlementTile, inca, braveType, UnitState.ACTIVE,braveType.getDefaultEquipment());
        braveOutside.setIndianSettlement(camp);

        // Setup enemy units
        int enemyUnits = camp.getUnitCount() + 1;
        for(int i=0; i< enemyUnits; i++){
            new ServerUnit(game, adjacentTile, dutch, veteranType, UnitState.ACTIVE);
        }

        Iterator<Unit> campUnitIter = camp.getOwnedUnitsIterator();
        while(campUnitIter.hasNext()){
            Unit brave = campUnitIter.next();
            assertNotNull("Got null while getting the camps units", brave);
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull("Couldnt get the ai object for the brave", aiUnit);

            aiInca.giveMilitaryMission(aiUnit);

            boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

            assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
        }

        inca.setStance(dutch, Stance.WAR);
        inca.setTension(dutch, new Tension(Tension.Level.HATEFUL.getLimit()));
        assertTrue("Indian player should be at war with dutch",
                   inca.getStance(dutch) == Stance.WAR);
        assertEquals("Wrong Indian player tension towards dutch",
                     Tension.Level.HATEFUL.getLimit(),
                     inca.getTension(dutch).getValue());

        aiInca.secureIndianSettlement(camp);

        // Verify if a unit was assigned a UnitSeekAndDestroyMission
        boolean isSeekAndDestroyMission = false;
        for(Unit brave : inca.getUnits()){
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull("Couldnt get aiUnit for players brave",aiUnit);
            assertNotNull("Unit missing mission",aiUnit.getMission());

            isSeekAndDestroyMission = aiUnit.getMission() instanceof UnitSeekAndDestroyMission;

            // found unit
            if(isSeekAndDestroyMission){
                break;
            }
        }
        assertTrue("One brave should have a UnitSeekAndDestroyMission", isSeekAndDestroyMission);
    }

    /**
     * When searching for threats to a settlement, the indian player
     * should ignore naval threats, as he does not have naval power
     */
    public void testSecureIndianSettlementMissionIgnoreNavalThreath() {
        Map map = getCoastTestMap(plainsType);
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        InGameController igc = ServerTestHelper.getInGameController();

        // Create player and unit
        ServerPlayer inca = (ServerPlayer) game.getPlayer("model.nation.inca");
        NativeAIPlayer aiInca = (NativeAIPlayer)aiMain.getAIPlayer(inca);
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        Tile settlementTile = map.getTile(9, 9);
        Tile seaTile = map.getTile(10, 9);
        assertTrue("Settlement tile should be land",settlementTile.isLand());
        assertFalse("Galleon tile should be ocean",seaTile.isLand());
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.player(inca).settlementTile(settlementTile).initialBravesInCamp(10).build();

        Unit galleon = new ServerUnit(game, seaTile, dutch, galleonType, UnitState.ACTIVE);

        int unitsInGalleon = 6;
        for(int i=0; i < unitsInGalleon; i ++){
            Unit artillery = new ServerUnit(game, seaTile, dutch, artilleryType, UnitState.SENTRY);
            igc.embarkUnit(dutch, artillery, galleon);
        }
        assertEquals("Wrong number of units onboard galleon",unitsInGalleon,galleon.getUnitCount());
        assertEquals("Galleon should be full",0,galleon.getSpaceLeft());

        for(Unit brave : camp.getUnitList()){
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull(aiUnit);

            aiInca.giveMilitaryMission(aiUnit);

            boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

            assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
        }

        inca.setStance(dutch, Stance.WAR);
        inca.setTension(dutch, new Tension(Tension.Level.HATEFUL.getLimit()));
        assertTrue("Indian player should be at war with dutch",
                   inca.getStance(dutch) == Stance.WAR);
        assertEquals("Wrong Indian player tension towards dutch",
                     Tension.Level.HATEFUL.getLimit(),
                     inca.getTension(dutch).getValue());

        aiInca.secureIndianSettlement(camp);

        boolean notUnitWanderHostileMission = false;
        for(Unit brave : inca.getUnits()){
            AIUnit aiUnit = aiMain.getAIUnit(brave);
            assertNotNull(aiUnit);

            boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;

            if(!isUnitWanderHostileMission){
                notUnitWanderHostileMission = true;
                break;
            }
        }
        assertFalse("Braves should not pursue naval units, or units aboard naval units", notUnitWanderHostileMission);
    }
}
