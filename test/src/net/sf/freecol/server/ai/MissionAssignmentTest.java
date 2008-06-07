package net.sf.freecol.server.ai;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTest;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.MockMapGenerator;

public class MissionAssignmentTest extends ServerTest {
	TileType plainsType = spec().getTileType("model.tile.plains");
	
	UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
	UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
	UnitType galleonType = spec().getUnitType("model.unit.galleon");
	
	final int MISSION_IMPOSSIBLE = Integer.MIN_VALUE;
	
	public void testImpossibleConditionsForTargetSelection() {
		// start a server
        FreeColServer server = startServer(false, true, SERVER_PORT, SERVER_NAME);
        
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
            this.stopServer(server);
        }
	}
}
