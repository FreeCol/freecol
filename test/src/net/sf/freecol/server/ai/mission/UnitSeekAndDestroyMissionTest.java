/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class UnitSeekAndDestroyMissionTest extends FreeColTestCase {

    private static final EquipmentType muskets
        = spec().getEquipmentType("model.equipment.muskets");

    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }
	
	
    public void testCapturedUnitsLoseMission() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        
        // Create attacking player and unit
        ServerPlayer player1
            = (ServerPlayer)game.getPlayer("model.nation.dutch");
        Tile tile1 = map.getTile(2, 2);
        Unit attacker = new ServerUnit(game, tile1, player1, veteranType);
        AIUnit aiUnit = aiMain.getAIUnit(attacker);
        assertNotNull(aiUnit);
        
        // Create defending player and unit
        ServerPlayer player2
            = (ServerPlayer)game.getPlayer("model.nation.french");
        Tile tile2 = map.getTile(2, 1);
        Unit defender = new ServerUnit(game, tile2, player2,
                                       veteranType, muskets);
        
        player1.setStance(player2, Stance.WAR);
        player2.setStance(player1, Stance.WAR);
        
        UnitSeekAndDestroyMission mission
            = new UnitSeekAndDestroyMission(aiMain,aiUnit,defender);
        aiUnit.setMission(mission);
        assertTrue("Attacker should have a UnitSeekAndDestroyMission",
                   aiUnit.getMission() instanceof UnitSeekAndDestroyMission);
                
        // simulate capture
        attacker.setOwner(player2);
        assertEquals("Attacking unit should have been captured",
                     attacker.getOwner(), player2);
        
        // re-check unit mission
        aiUnit = aiMain.getAIUnit(attacker);
        assertNull("Captured unit should lose previous mission",
                   aiUnit.getMission());
    }
	
    public void testDoNotPursueUnitsInColonies(){
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create attacking player and unit
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile1 = map.getTile(2, 2);
        Unit attacker = new ServerUnit(game, tile1, player1, veteranType);
        AIUnit aiUnit = aiMain.getAIUnit(attacker);
        assertNotNull(aiUnit);
        
        // Create defending player and unit
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile defenderTile = map.getTile(2, 1);
        Unit defender = new ServerUnit(game, defenderTile, player2, veteranType, muskets);
        
        player1.setStance(player2, Stance.WAR);
        player2.setStance(player1, Stance.WAR);
        
        UnitSeekAndDestroyMission mission = new UnitSeekAndDestroyMission(aiMain,aiUnit,defender);
        aiUnit.setMission(mission);
        boolean isSeekAndDestroyMission = aiUnit.getMission() instanceof UnitSeekAndDestroyMission;
        assertTrue("Attacker should have a UnitSeekAndDestroyMission", isSeekAndDestroyMission);
        assertTrue("UnitSeekAndDestroyMission should be valid", aiUnit.getMission().isValid());
        
        // add colony to the defender tile, to simulate the unit entering it
        getStandardColony(1, defenderTile.getX(),defenderTile.getY());
        String errMsg = "UnitSeekAndDestroyMission should NOT be valid anymore, defender in colony";
        assertFalse(errMsg, aiUnit.getMission().isValid());
    }
}
