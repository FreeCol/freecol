/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.util.List;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class PioneeringMissionTest extends FreeColTestCase {

    private static final GoodsType toolsGoodsType
        = spec().getGoodsType("model.goods.tools");

    private static final Role pioneerRole
        = spec().getRole("model.role.pioneer");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");

    private LogBuilder lb = new LogBuilder(0);


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testImprovementNoLongerValid() {
        final Game game = ServerTestHelper.startServerGame(getTestMap());
        final AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        // Get player, colony and unit
        final ServerPlayer player
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        final EuropeanAIPlayer aiPlayer
            = (EuropeanAIPlayer)aiMain.getAIPlayer(player);
        final Colony colony = getStandardColony();
        final AIColony aiColony = aiMain.getAIColony(colony);
        final Unit colonist = new ServerUnit(game, colony.getTile(), player,
                                             colonistType);
        final AIUnit aiUnit = aiMain.getAIUnit(colonist);
        assertNotNull(aiUnit);
        aiUnit.setMission(null);

        // Check there are improvements to be made.
        aiColony.updateTileImprovementPlans(lb);
        List<TileImprovementPlan> improvements
            = aiColony.getTileImprovementPlans();
        assertTrue("There should be valid improvements",
                   !improvements.isEmpty());
        aiPlayer.buildTipMap(lb);
        assertTrue("The player should need pioneers",
                   aiPlayer.pioneersNeeded() > 0);

        // Setup mission
        assertFalse("Colonist can improve",
                    colonist.hasAbility(Ability.IMPROVE_TERRAIN));
        assertEquals("Pioneering should be valid (despite no tools)", null,
                     PioneeringMission.invalidReason(aiUnit));
        assertNull("Pioneering should find no targets though",
                   PioneeringMission.findTarget(aiUnit, 10, false));

        // Add some tools to the colony, mission should become viable.
        colony.addGoods(toolsGoodsType, 100);
        assertTrue("Colony can provide tools",
                   colony.canProvideGoods(pioneerRole.getRequiredGoods()));
        assertEquals("Colony found", colony,
                     PioneeringMission.findTarget(aiUnit, 10, false));
        assertNull("Pioneer has no mission",
                   aiUnit.getMission());
        assertNull("Pioneering should be valid (tools present in colony)",
                   PioneeringMission.invalidReason(aiUnit));

        // Remove the tools as if to the unit and try again.
        colony.addGoods(toolsGoodsType, -100);
        colonist.setRole(pioneerRole);
        assertEquals("Should be a pioneer", pioneerRole,
                     colonist.getRole());
        assertTrue("Colonist can improve",
                   colonist.hasAbility(Ability.IMPROVE_TERRAIN));
        assertNotNull("TileImprovementPlan found",
                      PioneeringMission.findTarget(aiUnit, 10, false));
        assertNull("Pioneering should be valid (unit has tools)",
                   PioneeringMission.invalidReason(aiUnit));

        Location loc = PioneeringMission.findTarget(aiUnit, 10, false);
        assertTrue("Pioneer should find a tile to improve",
                   loc instanceof Tile);
        PioneeringMission mission
            = new PioneeringMission(aiMain, aiUnit, loc);
        assertTrue("Mission should be valid",
                   mission.isValid());
        TileImprovementPlan tip = mission.getTileImprovementPlan();
        assertNotNull("Mission should have a plan",
                      tip);
        Tile target = tip.getTarget();
        assertNotNull("Plan should have a target",
                      target);
        assertEquals("Mission should stick", mission,
                     aiUnit.getMission());
    }
}
