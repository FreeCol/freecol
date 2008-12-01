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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class MissionAssignmentTest extends FreeColTestCase {
	TileType plainsType = spec().getTileType("model.tile.plains");
	
	BuildingType stockadeType = spec().getBuildingType("model.building.Stockade");
	
	UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
	UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
	UnitType artilleryType = spec().getUnitType("model.unit.artillery");
	UnitType galleonType = spec().getUnitType("model.unit.galleon");
	
	final int MISSION_IMPOSSIBLE = Integer.MIN_VALUE;
	
	//FreeColServer server = null;
	
	public void setUp(){
		// start a server
        //server = ServerTestHelper.startServer(false, true);
	}
	
	public void tearDown(){
		//if(server != null)
			// must make sure that the server is stopped
            //ServerTestHelper.stopServer(server);
	}
	
	public void testImpossibleConditionsForTargetSelection() {
		// start a server
        FreeColServer server = ServerTestHelper.startServer(false, true);
        
        Map map = getCoastTestMap(plainsType);
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
                
        server.getController();
        
        AIMain aiMain = server.getAIMain();
        
        // Create attacking player and units
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer1 = (AIPlayer)aiMain.getAIObject(player1.getId());

        Tile tile1 = map.getTile(2, 2);
        Tile tile2 = map.getTile(2, 1);
        Unit soldier = new Unit(game, tile1, player1, veteranType, UnitState.ACTIVE);
        Unit friendlyColonist = new Unit(game, tile2, player1, colonistType, UnitState.ACTIVE);
        
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(soldier);
        assertNotNull(aiUnit);
        
        // Create defending player and unit
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");

        Tile tile3 = map.getTile(1, 2);
        Unit enemyColonist = new Unit(game, tile3, player2, colonistType, UnitState.ACTIVE);
        
        Tile tile4 = map.getTile(12, 12); // in the water
        assertFalse("Tle should be water",tile4.isLand());
        
        Unit enemyGalleon = new Unit(game, tile4, player2, galleonType, UnitState.ACTIVE);
        //Make tests
        int turnsToReach = 1; // not important
        
        try{
        	assertTrue("Cannot attack own unit", aiPlayer1.getUnitSeekAndDestroyMissionValue(soldier, friendlyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
        	assertTrue("Players are not at war", aiPlayer1.getUnitSeekAndDestroyMissionValue(soldier, enemyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
        	        	
        	player1.changeRelationWithPlayer(player2, Stance.WAR);
        	
        	assertFalse("Unit should be able to attack land unit", aiPlayer1.getUnitSeekAndDestroyMissionValue(soldier, enemyColonist.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
        	assertTrue("Land unit cannot attack naval unit", aiPlayer1.getUnitSeekAndDestroyMissionValue(soldier, enemyGalleon.getTile(), turnsToReach) == MISSION_IMPOSSIBLE);
      
        
        }finally {
        	// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
        }
        
	}
	
	public void testGiveMilitaryMission() {
		// start a server
        FreeColServer server = ServerTestHelper.startServer(false, true);
        
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
        
        server.getController();
        
        AIMain aiMain = server.getAIMain();
        
        // Create attacking player and units
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer1 = (AIPlayer)aiMain.getAIObject(player1.getId());

        Tile tile1 = map.getTile(2, 2);
        Unit soldier = new Unit(game, tile1, player1, veteranType, UnitState.ACTIVE);
        
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(soldier);
        assertNotNull(aiUnit);
        
        // Create defending player
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");

        //Tile tile3 = map.getTile(1, 2);
        //Unit enemyGalleon = new Unit(game, tile4, player2, galleonType, UnitState.ACTIVE);
        //Make tests        
        try{
        	aiPlayer1.giveMilitaryMission(aiUnit);
        	
        	boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
        	
        	assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
        	
        	// Add non-hostile unit 
        	Tile tile2 = map.getTile(2, 1);
            new Unit(game, tile2, player2, colonistType, UnitState.ACTIVE);
            
            // reassign mission and check
            aiPlayer1.giveMilitaryMission(aiUnit);
            isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
            
        	assertTrue("No hostile units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
            
        	// Make unit hostile by changing stance to War,reassign mission and check
        	player1.changeRelationWithPlayer(player2, Stance.WAR);
        	aiPlayer1.giveMilitaryMission(aiUnit);
        	
        	isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
        	boolean isSeekAndDestroyMission = aiUnit.getMission() instanceof UnitSeekAndDestroyMission;
        	
        	assertFalse("Enemy unit is present, should not be UnitWanderHostileMission", isUnitWanderHostileMission);
        	assertTrue("Enemy unit is present, should be UnitSeekAndDestroyMission", isSeekAndDestroyMission);      	
        }finally {
        	// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
        }
	}
	
	public void testAssignDefendSettlementMission() {
		// start a server
        FreeColServer server = ServerTestHelper.startServer(false, true);
        
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
        map = game.getMap();  // update reference
        
        server.getController();
        
        AIMain aiMain = server.getAIMain();
        
        // Create player and unit
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer1 = (AIPlayer)aiMain.getAIObject(player1.getId());

        Tile tile1 = map.getTile(2, 2);
        Unit soldier = new Unit(game, tile1, player1, veteranType, UnitState.ACTIVE);
        
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(soldier);
        assertNotNull(aiUnit);
                
        try{
        	aiPlayer1.giveMilitaryMission(aiUnit);
        	
        	boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
        	
        	assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
        	
        	// Add nearby colony in need of defense
        	Tile colonyTile = map.getTile(2, 3);
        	assertTrue(colonyTile != null);
        	colonyTile.setExploredBy(player1, true);
        	Colony colony = new Colony(game,player1,"TestColony",colonyTile);
        	Unit colonist = new Unit(game, colonyTile, player1, colonistType, UnitState.ACTIVE);
        	
        	assertTrue(colonyTile.getMap() != null);
        	
        	colonist.buildColony(colony);
        	
        	assertTrue(colonyTile.getSettlement() == colony);
        	assertTrue(colony.getOwner() == player1);
        	assertTrue(colony.getUnitCount() == 1);
        	
        	int turnsToColony = 1;
        	// reassign mission and check
 
            aiPlayer1.giveMilitaryMission(aiUnit);
            
            boolean missionIsValid = aiPlayer1.getDefendColonyMissionValue(soldier, colony, turnsToColony) > 0;
            isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
            boolean isDefendSettlementMission = aiUnit.getMission() instanceof DefendSettlementMission;
        	
            assertTrue("DefendColonyMission should be possible",missionIsValid);
            assertFalse("Colony in need of defense, should no longer be UnitWanderHostileMission", isUnitWanderHostileMission);
            assertTrue("Colony in need of defense, should be DefendSettlementMission", isDefendSettlementMission);
        	
            //TODO: Increase defense to a point where the soldier does best to patrol
            
            // Right now (08/05/2008), the algorithm that calculates the value of a colony defense mission
            //does not take into account the various levels of fortification, and requires too many units to
            //guard the colony
                       
        }finally {
        	// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
        }
	}
	
	/**
	 * When searching for threats to a settlement, the indian player
	 *should ignore naval threats, as he does not have naval power
	 */
	public void testSecureIndianSettlementMissionIgnoreNavalThreath() {
	    // start a server
	    FreeColServer server = ServerTestHelper.startServer(false, true);

	    Map map = getCoastTestMap(plainsType);

	    server.setMapGenerator(new MockMapGenerator(map));

	    Controller c = server.getController();
	    PreGameController pgc = (PreGameController)c;

	    try {
	        pgc.startGame();
	    } catch (FreeColException e) {
	        fail("Failed to start game");
	    }

	    Game game = server.getGame();
	    map = game.getMap();  // update reference

	    server.getController();

	    AIMain aiMain = server.getAIMain();

	    try{
	        // Create player and unit
	        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.inca");
	        AIPlayer aiPlayer1 = (AIPlayer)aiMain.getAIObject(player1.getId());
	        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.dutch");

	        Tile settlementTile = map.getTile(9, 9);
	        Tile seaTile = map.getTile(10, 9);
	        assertTrue("Settlement tile should be land",settlementTile.isLand());
	        assertFalse("Galleon tile should be ocean",seaTile.isLand());
	        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
	        IndianSettlement camp = builder.player(player1).settlementTile(settlementTile).initialBravesInCamp(10).build();

	        Unit galleon = new Unit(game, seaTile, player2, galleonType, UnitState.ACTIVE);

	        int unitsInGalleon = 6;
	        for(int i=0; i < unitsInGalleon; i ++){
	            Unit artillery = new Unit(game, seaTile, player2, artilleryType, UnitState.SENTRY);
	            artillery.embark(galleon);
	        }
	        assertEquals("Wrong number of units onboard galleon",unitsInGalleon,galleon.getUnitCount());
	        assertEquals("Galleon should be full",0,galleon.getSpaceLeft());
	        
	        for(Unit brave : camp.getUnitList()){
	            AIUnit aiUnit = (AIUnit) aiMain.getAIObject(brave);
	            assertNotNull(aiUnit);
	            
	            aiPlayer1.giveMilitaryMission(aiUnit);
	            
	            boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
	            
	            assertTrue("No enemy units are present, should be UnitWanderHostileMission", isUnitWanderHostileMission);
	        }

	        player1.changeRelationWithPlayer(player2, Stance.WAR);
	        player1.setTension(player2, new Tension(Tension.Level.HATEFUL.getLimit()));
	        assertTrue("Indian player should be at war with dutch",player1.getStance(player2) == Stance.WAR);
	        assertEquals("Wrong Indian player tension towards dutch",Tension.Level.HATEFUL.getLimit(),player1.getTension(player2).getValue());
	        
	        aiPlayer1.secureIndianSettlement(camp);
	        
	        boolean notUnitWanderHostileMission = false;
	        for(Unit brave : player1.getUnits()){
                AIUnit aiUnit = (AIUnit) aiMain.getAIObject(brave);
                assertNotNull(aiUnit);

                boolean isUnitWanderHostileMission = aiUnit.getMission() instanceof UnitWanderHostileMission;
                
                if(!isUnitWanderHostileMission){
                    notUnitWanderHostileMission = true;
                    break;
                }
            }
            assertFalse("Braves should not pursue naval units, or units aboard naval units", notUnitWanderHostileMission);
	    }finally {
	        // must make sure that the server is stopped
	        ServerTestHelper.stopServer(server);
	    }
	}
}
