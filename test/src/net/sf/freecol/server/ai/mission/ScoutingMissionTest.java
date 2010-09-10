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

public class ScoutingMissionTest extends FreeColTestCase {
    
    public void testHorsesLost() {
        
        UnitType scoutType = spec().getUnitType("model.unit.seasonedScout");
        EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");
        
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
        
        try{
            Game game = server.getGame();
            map = game.getMap();  // update reference

            server.getController();

            AIMain aiMain = server.getAIMain();

            // Create players, settlement and unit
            ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.inca");
            ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.dutch");
            Tile settlementTile = map.getTile(2, 1);
            FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
            builder.player(player1).settlementTile(settlementTile).build();

            Tile unitTile = map.getTile(2, 2);
            Unit scout = new Unit(game, unitTile, player2, scoutType,
                                  UnitState.ACTIVE, horsesEqType);
            
            // Setup mission
            // this will call AIPlayer.giveNormalMissions() and set the scout mission
            
            AIUnit aiUnit = (AIUnit) aiMain.getAIObject(scout);
            assertNotNull(aiUnit);
            assertTrue("Scouting mission should be assignable to scout",ScoutingMission.isValid(aiUnit));
            aiUnit.setMission(new ScoutingMission(aiMain,aiUnit));
            Mission unitMission = aiUnit.getMission();
            assertNotNull("Scout should have been assigned a mission", unitMission);
            boolean hasScoutingMission = unitMission instanceof ScoutingMission;
            assertTrue("Scout should have been assigned a Scouting mission",hasScoutingMission);
            
            assertTrue("Scouting mission should be valid",unitMission.isValid());

            // Simulate unit losing horses
            scout.changeEquipment(horsesEqType, -1);
            
            // Verify that mission no longer valid
            assertFalse("Scouting mission should no longer be valid",unitMission.isValid());
        }finally {
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
        }
    }
}
