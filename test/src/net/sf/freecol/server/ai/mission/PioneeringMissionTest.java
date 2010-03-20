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
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class PioneeringMissionTest extends FreeColTestCase {
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    EquipmentType toolsEqType = spec().getEquipmentType("model.equipment.tools");
    
    public void testImprovementNoLongerValid() {
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

            // Create player and unit
            ServerPlayer player = (ServerPlayer) game.getPlayer("model.nation.dutch");

            Tile unitTile = map.getTile(2, 2);
            Unit colonist = new Unit(game, unitTile, player, colonistType, UnitState.ACTIVE);
            colonist.equipWith(toolsEqType,true);
            
            // Setup mission
            AIUnit aiUnit = (AIUnit) aiMain.getAIObject(colonist);
            assertNotNull(aiUnit);
            Tile improvementTarget = map.getTile(10, 10);
            TileImprovementType roadImprovement = spec().getTileImprovementType("model.improvement.road");
            TileImprovementPlan improvement =  new TileImprovementPlan(aiMain, improvementTarget, roadImprovement, 100);                        
            improvement.setPioneer(aiUnit);
            PioneeringMission mission = new PioneeringMission(aiMain,aiUnit);
            mission.setTileImprovementPlan(improvement);            
            aiUnit.setMission(mission);
            
            //Verify assigned mission
            Mission unitMission = aiUnit.getMission();
            assertNotNull("Colonist should have been assigned a mission", unitMission);
            boolean hasPioneeringMission = unitMission instanceof PioneeringMission;
            assertTrue("Colonist should have been assigned a Pioneering mission",hasPioneeringMission);
            assertTrue("Pioneering mission should be valid", aiUnit.getMission().isValid());

            // Simulate improvement tile getting road other than by unit 
            TileImprovement tileRoad = new TileImprovement(game, improvementTarget, roadImprovement);
            tileRoad.setTurnsToComplete(0);
            improvementTarget.setTileItemContainer(new TileItemContainer(game, improvementTarget));
            improvementTarget.getTileItemContainer().addTileItem(tileRoad);
            assertTrue("Tile should have road", improvementTarget.hasRoad());            

            // Verify that mission no longer valid
            assertFalse("Pioneering mission should not be valid anymore", aiUnit.getMission().isValid());
        }finally {
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
        }
    }
}
