/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;

public class ServerEuropeTest extends FreeColTestCase {

    public void testIncreaseRecruitmentDifficulty() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        Field field = Europe.class.getDeclaredField("baseRecruitPrice");
        field.setAccessible(true);
        int baseBefore = (int) field.get(se);

        se.increaseRecruitmentDifficulty();

        int incPrice = spec().getInteger(GameOptions.RECRUIT_PRICE_INCREASE);

        assertEquals("Recruit price should increase by the specified option value",
            baseBefore + incPrice, (int) field.get(se));
    }

    public void testReplaceRecruitsRemovesInvalidOnes() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        AbstractUnit invalid = new AbstractUnit(colonist.getId(), "model.role.default", 1);

        Method m = Europe.class.getDeclaredMethod("addRecruitable", AbstractUnit.class, boolean.class);
        m.setAccessible(true);
        m.invoke(se, invalid, true);

        boolean replaced = se.replaceRecruits(new Random(1));
        assertTrue("At least one recruit should have been replaced", replaced);

        List<AbstractUnit> recruits = se.getExpandedRecruitables(true);
        for (AbstractUnit au : recruits) {
            assertTrue("All recruits must be valid recruitable types",
                au.getType(spec()).isRecruitable());
        }
    }

    public void testExtractRecruitableRemovesCorrectSlot() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        se.initializeMigration(new Random(1));
        List<AbstractUnit> before = se.getExpandedRecruitables(true);

        AbstractUnit extracted = se.extractRecruitable(0, new Random(1));
        List<AbstractUnit> after = se.getExpandedRecruitables(true);

        assertNotNull("Extracted unit should not be null", extracted);
        assertEquals("List size should remain the same due to fillRecruitables", 
            before.size(), after.size());
        assertFalse("The extracted unit should no longer be in the recruitables list", 
            after.contains(extracted));
    }

    public void testGenerateFountainRecruits() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        int count = 3;
        List<Unit> recruits = se.generateFountainRecruits(count, new Random(1));

        assertEquals("Should generate exactly 3 units", count, recruits.size());
        for (Unit u : recruits) {
            assertEquals("Unit owner should be the Dutch player", dutch, u.getOwner());
            assertTrue("Generated unit type must be recruitable", u.getType().isRecruitable());
        }
    }

    public void testCsNewTurnRepairsDamagedNavalUnits() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        UnitType caravel = spec().getUnitType("model.unit.caravel");
        ServerUnit ship = new ServerUnit(game, se, dutch, caravel);

        ship.setLocation(se);

        assertTrue("Ship must be present in the Europe unit list",
            se.getUnits().anyMatch(u -> u == ship));

        int max = ship.getType().getHitPoints();
        assertTrue("Caravel unit type must have hit points defined", max > 0);

        ship.setHitPoints(max - 1);
        assertTrue("Ship should report as damaged before turn processing", ship.isDamaged());

        ChangeSet cs = new ChangeSet();
        LogBuilder lb = new LogBuilder(64);

        se.csNewTurn(new Random(1), lb, cs);

        assertFalse("Ship should be repaired after csNewTurn", ship.isDamaged());
        assertEquals("Ship should be restored to maximum HP", max, ship.getHitPoints());
    }

    public void testEquipForRoleFailsIfRoleUnavailable() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerEurope se = (ServerEurope) dutch.getEurope();

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        Unit unit = new ServerUnit(game, se, dutch, colonist);

        Role dragoon = spec().getRole("model.role.dragoon");

        boolean ok = se.equipForRole(unit, dragoon, 1);

        assertFalse("Equipping should fail when resources/requirements are not met", ok);
    }
}
