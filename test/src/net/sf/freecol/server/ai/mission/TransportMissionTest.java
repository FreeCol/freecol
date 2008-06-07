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
import net.sf.freecol.common.model.CombatModel;
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
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;

public class TransportMissionTest extends FreeColTestCase {
    
    public void testTransportMission() {
        // start a server
        FreeColServer server = ServerTestHelper.startServer(false, true);
        
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
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer = (AIPlayer)aiMain.getAIObject(player1.getId());

        
        
        // create a ship carrying a colonist
        Tile tile1 = map.getTile(6, 9);
        
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, tile1, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        assertTrue(galleon.hasAbility("model.ability.navalUnit"));
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new Unit(game, galleon, player1, colonistType, UnitState.SENTRY);
        assertTrue(colonist.getLocation()==galleon);
        
        //Create the attacker
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile2 = map.getTile(5, 9);
        UnitType privateerType = spec().getUnitType("model.unit.privateer");
        Unit privateer = new Unit(game, tile2, player2, privateerType, UnitState.ACTIVE);
                
        // assign transport mission to the ship
        aiUnit.setMission(new TransportMission(aiMain, aiUnit));
        
        //Simulate the combat
        CombatModel combatModel = game.getCombatModel();
        CombatModel.CombatResult combatResult = new CombatModel.CombatResult(CombatModel.CombatResultType.WIN,galleon.getHitpoints());
        combatModel.attack(privateer, galleon, combatResult , 0);

        // Verify that the outcome of the combat is  a return to Europe for repairs
        // and also invalidation of the transport mission as side effect
        assertTrue(galleon.isUnderRepair());
        assertFalse(aiUnit.getMission().isValid());
        
        try {
            // this will call AIPlayer.abortInvalidMissions() and change the carrier mission
            aiPlayer.startWorking();
            assertFalse(aiUnit.getMission() instanceof TransportMission);
        } finally {
            ServerTestHelper.stopServer(server);
        }
    }
}
