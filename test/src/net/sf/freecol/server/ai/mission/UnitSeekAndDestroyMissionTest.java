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

package net.sf.freecol.server.ai.mission;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class UnitSeekAndDestroyMissionTest extends FreeColTestCase {
	UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
	
	FreeColServer server = null;
	
	public void tearDown(){
		if(server != null){
			// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
		}
	}
	
	public void testCapturedUnitsLoseMission() {
		// start a server
        server = ServerTestHelper.startServer(false, true);
        
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
        AIMain aiMain = server.getAIMain();
        
        // Create attacking player and unit
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile1 = map.getTile(2, 2);
        Unit attacker = new Unit(game, tile1, player1, veteranType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(attacker);
        assertNotNull(aiUnit);
        
        // Create defending player and unit
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile2 = map.getTile(2, 1);
        Unit defender = new Unit(game, tile2, player2, veteranType, UnitState.ACTIVE);
        defender.equipWith(muskets, true);
        
        player1.changeRelationWithPlayer(player2, Stance.WAR);
        
        UnitSeekAndDestroyMission mission = new UnitSeekAndDestroyMission(aiMain,aiUnit,defender);
        aiUnit.setMission(mission);
        boolean isSeekAndDestroyMission = aiUnit.getMission() instanceof UnitSeekAndDestroyMission;
        assertTrue("Attacker should have a UnitSeekAndDestroyMission", isSeekAndDestroyMission);
                
        // simulate capture
        attacker.setOwner(player2);
        assertTrue("Attacking unit should have been captured", attacker.getOwner() == player2);
        
        // re-check unit mission
        aiUnit = (AIUnit) aiMain.getAIObject(attacker);
        assertFalse("Captured unit should lose previous mission", aiUnit.getMission() == null);
	}
}
