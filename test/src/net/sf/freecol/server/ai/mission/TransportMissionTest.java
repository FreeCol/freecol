/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTest;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;

public class TransportMissionTest extends ServerTest {
    
    public void testTransportMission() {
        // start a server
        FreeColServer server = startServer(false, true, SERVER_PORT, SERVER_NAME);
        
        // generate a random map
        Controller c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail();
        }
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        
        Game game = server.getGame();
        assertNotNull(game);
        Map map = game.getMap();
        assertNotNull(map);
        
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer = (AIPlayer)aiMain.getAIObject(player.getId());

        // create a ship carrying a colonist
        Tile tile = map.getTile(6, 9);
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, tile, player, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        assertTrue(galleon.hasAbility("model.ability.navalUnit"));
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new Unit(game, galleon, player, colonistType, UnitState.SENTRY);
        assertTrue(colonist.getLocation()==galleon);
        
        // assign transport mission to the ship
        aiUnit.setMission(new TransportMission(aiMain, aiUnit));
        
        // Now, we simulate an attack on this ship from an enemy ship or fortress
        // Setting the hit points to zero should result in a return to Europe for repairs
        // and also invalidate the transport mission as side effect
        galleon.setHitpoints(0);
        assertTrue(galleon.isUnderRepair());
        assertFalse(aiUnit.getMission().isValid());
        
        try {
            // this will call AIPlayer.abortInvalidMissions() and should kill the transported colonist
            // Disposing the colonist triggers a ConcurrentModificationException
            // because it attempts disposing an AIUnit while iterating on the list of AIUnits
            aiPlayer.startWorking();
        } finally {
            this.stopServer(server);
        }
    }
}
