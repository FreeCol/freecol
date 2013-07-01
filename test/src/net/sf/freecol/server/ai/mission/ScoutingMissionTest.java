/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ScoutingMissionTest extends FreeColTestCase {

    private static final EquipmentType horsesEqType
        = spec().getEquipmentType("model.equipment.horses");

    private static final UnitType scoutType
        = spec().getUnitType("model.unit.seasonedScout");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testHorsesLost() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Create players, settlement and unit
        ServerPlayer inca = (ServerPlayer)game.getPlayer("model.nation.inca");
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");

        Tile settlementTile = map.getTile(2, 1);
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(inca).settlementTile(settlementTile).build();
        IndianSettlement is = settlementTile.getIndianSettlement();
        Player.makeContact(inca, dutch);

        Tile unitTile = map.getTile(2, 2);
        Unit scout = new ServerUnit(game, unitTile, dutch, scoutType,
                                    horsesEqType);

        AIUnit aiUnit = aiMain.getAIUnit(scout);
        aiUnit.abortMission("test");
        assertNotNull("The scout should be an AI unit", aiUnit);
        assertTrue("Scout should have the scout role",
            scout.hasAbility(Ability.SCOUT_INDIAN_SETTLEMENT));
        assertEquals("The Inca settlement should be a scouting target", null,
            ScoutingMission.invalidReason(aiUnit, is));
        assertEquals("The Inca settlement should be found as scouting target",
            is, ScoutingMission.findTarget(aiUnit, 10, false));
        assertEquals("Scouting mission should be assignable to scout", null,
            ScoutingMission.invalidReason(aiUnit));
        assertEquals("Scout should find the Inca settlement", is,
            ScoutingMission.findTarget(aiUnit, 10, false));
        aiUnit.setMission(new ScoutingMission(aiMain, aiUnit, is));
        assertTrue("Scout should have been assigned a Scouting mission",
            aiUnit.getMission() instanceof ScoutingMission);
        assertTrue("Scouting mission should be valid",
            aiUnit.getMission().isValid());
        assertEquals("Scouting mission target should be the Inca settlement",
            is, aiUnit.getMission().getTarget());

        // Invalidate the mission by losing the horses.
        scout.changeEquipment(horsesEqType, -1);
        assertFalse("Scout should not have the scout role",
            scout.hasAbility(Ability.SCOUT_INDIAN_SETTLEMENT));
        assertNotNull("Scouting mission should be invalid",
            aiUnit.getMission().invalidReason());
        assertNotNull("Scouting mission should be impossible for this unit",
            ScoutingMission.invalidReason(aiUnit));
        
        // Restore the horses.
        scout.changeEquipment(horsesEqType, 1);
        assertTrue("Scouting mission should be valid again",
            aiUnit.getMission().isValid());

        // Complete the mission, should be invalid.
        is.setScouted(dutch);
        assertFalse("Scouting mission should be invalid lacking target",
            aiUnit.getMission().isValid());

        // Add an LCR.  Mission could become valid again.
        Tile lcrTile = map.getTile(2, 3);
        lcrTile.addLostCityRumour(new LostCityRumour(game, lcrTile));
        assertEquals("Scouting mission should be possible for this unit", null,
            ScoutingMission.invalidReason(aiUnit));
        assertEquals("The LCR tile should be a scouting target",
            lcrTile, ScoutingMission.findTarget(aiUnit, 10, false));
    }
}
